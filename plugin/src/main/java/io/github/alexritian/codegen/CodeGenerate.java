package io.github.alexritian.codegen;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import nu.studer.gradle.jooq.JooqConfig;
import nu.studer.gradle.jooq.util.Gradles;
import nu.studer.gradle.jooq.util.Objects;
import org.flywaydb.core.Flyway;
import org.gradle.api.*;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;
import org.jooq.tools.StringUtils;
import io.github.alexritian.codegen.containers.PostgresServer;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import static nu.studer.gradle.jooq.util.Objects.cloneObject;

/**
 * @author Too_young
 */
@CacheableTask
public abstract class CodeGenerate extends DefaultTask {
    private final Boolean useContainer;
    private final Configuration jooqConfiguration;
    private final Provider<String> normalizedJooqConfigurationHash;
    private final FileCollection runtimeClasspath;
    private final Provider<Directory> outputDir;
    private final Property<Boolean> allInputsDeclared;

    private Action<? super Configuration> generationToolNormalization;
    private Action<? super JavaExecSpec> javaExecSpec;
    private Action<? super ExecResult> execResultHandler;

    private final ProjectLayout projectLayout;
    private final ExecOperations execOperations;
    private final FileSystemOperations fileSystemOperations;

    private static final Action<Configuration> OUTPUT_DIRECTORY_NORMALIZATION = c -> c.getGenerator().getTarget().setDirectory(null);

    @Inject
    public CodeGenerate(Boolean useContainer, JooqConfig config, FileCollection runtimeClasspath, ExtensionContainer extensions, ObjectFactory objects, ProviderFactory providers,
                        ProjectLayout projectLayout, ExecOperations execOperations, FileSystemOperations fileSystemOperations) {
        this.useContainer = useContainer;
        this.jooqConfiguration = config.getJooqConfiguration();
        this.normalizedJooqConfigurationHash = normalizedJooqConfigurationHash(objects, providers);
        this.runtimeClasspath = objects.fileCollection().from(runtimeClasspath);
        this.outputDir = objects.directoryProperty().value(config.getOutputDir());
        this.allInputsDeclared = objects.property(Boolean.class).convention(Boolean.FALSE);

        this.projectLayout = projectLayout;
        this.execOperations = execOperations;
        this.fileSystemOperations = fileSystemOperations;

        // do not use lambda due to a bug in Gradle 6.5
        getOutputs().upToDateWhen(new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                return allInputsDeclared.get();
            }
        });
    }

    private Provider<String> normalizedJooqConfigurationHash(ObjectFactory objects, ProviderFactory providers) {
        Property<String> normalizedConfigurationHash = objects.property(String.class);
        normalizedConfigurationHash.set(providers.provider(() -> {
            Configuration clonedConfiguration = cloneObject(jooqConfiguration);
            OUTPUT_DIRECTORY_NORMALIZATION.execute(clonedConfiguration);
            if (generationToolNormalization != null) {
                generationToolNormalization.execute(clonedConfiguration);
            }
            return Objects.deepHash(clonedConfiguration);
        }));
        normalizedConfigurationHash.finalizeValueOnRead();
        return normalizedConfigurationHash;
    }

    @Input
    public Provider<String> getNormalizedJooqConfigurationHash() {
        return normalizedJooqConfigurationHash;
    }

    @Classpath
    public FileCollection getRuntimeClasspath() {
        return runtimeClasspath;
    }

    @OutputDirectory
    public Provider<Directory> getOutputDir() {
        return outputDir;
    }

    @Internal
    public Property<Boolean> getAllInputsDeclared() {
        return allInputsDeclared;
    }

    @Internal
    public Action<? super JavaExecSpec> getJavaExecSpec() {
        return javaExecSpec;
    }

    @SuppressWarnings("unused")
    public void setJavaExecSpec(Action<? super JavaExecSpec> javaExecSpec) {
        this.javaExecSpec = javaExecSpec;
    }

    @Internal
    public Action<? super ExecResult> getExecResultHandler() {
        return execResultHandler;
    }

    @SuppressWarnings("unused")
    public void setExecResultHandler(Action<? super ExecResult> execResultHandler) {
        this.execResultHandler = execResultHandler;
    }

    @Internal
    public Action<? super Configuration> getGenerationToolNormalization() {
        return generationToolNormalization;
    }

    @SuppressWarnings("unused")
    public void setGenerationToolNormalization(Action<? super Configuration> generationToolNormalization) {
        this.generationToolNormalization = generationToolNormalization;
    }

    @Internal
    abstract Property<PostgresServer> getServer();

    @TaskAction
    public void generate() {
        // only configure the container for jooq configuration domain where the jdbc url is blank
        if (useContainer && StringUtils.isBlank(jooqConfiguration.getJdbc().getUrl())) {
            // start database container
            startContainer();
            // migrate data to database
            migrateDatabase();
            // exclude flyway tables
            jooqConfiguration.getGenerator().getDatabase().setExcludes("flyway_.* | " +
                    jooqConfiguration.getGenerator().getDatabase().getExcludes());
        }

        // abort if cleaning of output directory is disabled
        ensureTargetIsCleaned(jooqConfiguration);

        // avoid excessive and/or schema-violating XML being created due to the serialization of default values
        trimConfiguration(jooqConfiguration);

        // set target directory to the defined default value if no explicit value has been configured
        jooqConfiguration.getGenerator().getTarget().setDirectory(outputDir.get().getAsFile().getAbsolutePath());

        // clean target directory to ensure no stale files are still around
        fileSystemOperations.delete(spec -> spec.delete(outputDir.get()));

        // define a config file to which the jOOQ code generation configuration is written to
        File configFile = new File(getTemporaryDir(), "config.xml");

        // write jOOQ code generation configuration to config file
        writeConfiguration(jooqConfiguration, configFile);

        // generate the jOOQ Java sources files using the written config file
        ExecResult execResult = executeJooq(configFile);

        // invoke custom result handler
        if (execResultHandler != null) {
            execResultHandler.execute(execResult);
        }
    }

    private void ensureTargetIsCleaned(Configuration configuration) {
        Generator generator = configuration.getGenerator();
        if (generator != null) {
            Target target = generator.getTarget();
            if (target != null) {
                if (!target.isClean()) {
                    throw new GradleException(
                            "generator.target.clean must not be set to false. " +
                                    "Disabling the cleaning of the output directory can lead to unexpected behavior in a Gradle build.");
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void trimConfiguration(Configuration configuration) {
        // avoid default value (name) being written even when matchers are configured
        Generator generator = configuration.getGenerator();
        if (generator != null) {
            Strategy strategy = generator.getStrategy();
            if (strategy != null && strategy.getMatchers() != null) {
                strategy.setName(null);
            }
        }

        // avoid JDBC element being written when it has an empty configuration
        Jdbc jdbc = configuration.getJdbc();
        if (jdbc != null) {
            if (jdbc.getDriver() == null
                    && jdbc.getUrl() == null
                    && jdbc.getSchema() == null
                    && jdbc.getUser() == null
                    && jdbc.getUsername() == null
                    && jdbc.getPassword() == null
                    && jdbc.isAutoCommit() == null
                    && jdbc.getProperties().isEmpty()
            ) {
                configuration.setJdbc(null);
            }
        }
    }

    private void writeConfiguration(Configuration config, File file) {
        try (OutputStream fs = new FileOutputStream(file)) {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String resourceFileName = xsdResourcePath();
            URL schemaResourceURL = GenerationTool.class.getResource(resourceFileName);
            if (schemaResourceURL == null) {
                throw new GradleException("Failed to locate jOOQ codegen schema: " + resourceFileName);
            }

            Schema schema = sf.newSchema(schemaResourceURL);
            JAXBContext ctx = JAXBContext.newInstance(Configuration.class);
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setSchema(schema);

            marshaller.marshal(config, fs);
        } catch (IOException | JAXBException | SAXException e) {
            throw new TaskExecutionException(CodeGenerate.this, e);
        }
    }

    private String xsdResourcePath() {
        // use reflection to avoid inlining of the String constant org.jooq.Constants.XSD_CODEGEN
        try {
            Class<?> jooqConstants = Class.forName("org.jooq.Constants");
            return (String) jooqConstants.getDeclaredField("CP_CODEGEN").get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new TaskExecutionException(CodeGenerate.this, e);
        }
    }

    private ExecResult executeJooq(final File configFile) {
        return execOperations.javaexec(spec -> {
            setMainClass("org.jooq.codegen.GenerationTool", spec);
            spec.setClasspath(runtimeClasspath);
            spec.setWorkingDir(projectLayout.getProjectDirectory());
            spec.args(configFile);
            if (javaExecSpec != null) {
                javaExecSpec.execute(spec);
            }
        });
    }

    private void setMainClass(String mainClass, JavaExecSpec spec) {
        if (Gradles.isAtLeastGradleVersion("6.4")) {
            spec.getMainClass().set(mainClass);
        } else {
            setMainClassDeprecated(mainClass, spec);
        }
    }

    @SuppressWarnings("deprecation")
    private void setMainClassDeprecated(String mainClass, JavaExecSpec spec) {
        spec.setMain(mainClass);
    }

    private void startContainer() {
        var container = getServer().get().getContainer();
        jooqConfiguration.getJdbc()
                .withDriver(container.getDriverClassName())
                .withUrl(container.getJdbcUrl())
                .withUser(container.getUsername())
                .withPassword(container.getPassword());
    }

    private void migrateDatabase() {
        String migrationScriptsLocation = new File(projectLayout.getProjectDirectory().getAsFile(), "src/main/resources/db/migration").getAbsolutePath();
        var result = Flyway.configure()
                .dataSource(jooqConfiguration.getJdbc().getUrl(), jooqConfiguration.getJdbc().getUser(), jooqConfiguration.getJdbc().getPassword())
                .schemas(jooqConfiguration.getGenerator().getDatabase().getInputSchema())
                .locations("filesystem:" + migrationScriptsLocation)
                .load()
                .migrate();
        result.migrations.forEach(migration -> getLogger().info("Executed migration: {}", migration.filepath));
    }

}
