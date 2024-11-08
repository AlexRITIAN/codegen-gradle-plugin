package io.github.alexritian.codegen;

import nu.studer.gradle.jooq.JooqConfig;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.ForcedType;
import org.jooq.meta.jaxb.Strategy;

import javax.inject.Inject;
import java.time.Instant;

/**
 * @author Too_young
 */
public class CodegenConfig extends JooqConfig {

    final String name;

    private final Database database;
    private final Output output;
    private final Configuration configuration;
    private final ForcedTypeContainer forcedTypes;

    @Inject
    public CodegenConfig(String name, Project project, ObjectFactory objects, ProviderFactory providers, ProjectLayout layout) {
        super(name, objects, providers, layout);
        this.name = name;
        this.database = new Database();
        this.output = new Output();
        this.configuration = super.getJooqConfiguration();
        this.forcedTypes = new ForcedTypeContainer(project);
    }

    @Override
    public Configuration getJooqConfiguration() {
        return this.configuration;
    }

    public ForcedTypeContainer getForcedTypes() {
        return this.forcedTypes;
    }

    public void database(Action<? super Database> action) {
        action.execute(database);
        var strategy = new Strategy();
        strategy.setName("io.github.alexritian.codegen.NameGeneratorStrategy");
        configuration.withJdbc(configuration.getJdbc()
                        .withDriver(database.getDriver())
                        .withUrl(database.getUrl())
                        .withUser(database.getUser())
                        .withPassword(database.getPassword()))
                .withGenerator(configuration.getGenerator()
                        .withGenerate(configuration.getGenerator()
                                .withStrategy(strategy).getGenerate()
                                .withPojosEqualsAndHashCode(true)
                                .withRelations(true)
                                .withDeprecated(false)
                                .withRecords(true)
                                .withImmutablePojos(false)
                                .withFluentSetters(true)
                                .withSpringAnnotations(true)
                                .withPojos(true))
                        .withDatabase(configuration.getGenerator().getDatabase()
                                .withInputSchema(database.getSchema())
                                .withIncludes(database.getIncludes())
                                .withExcludes(database.getExcludes())));
    }

    public void output(Action<? super Output> action) {
        action.execute(output);
        configuration.getGenerator().getTarget()
                .withPackageName(output.getPackageName())
                .withDirectory(output.getDirectory());
    }

    public void forcedTypes(Action<? super ForcedTypeContainer> action) {
        action.execute(forcedTypes);
        configuration.getGenerator().getDatabase().getForcedTypes().addAll(forcedTypes.forcedTypes);
    }

    public static class Database {
        private String driver;
        private String url;
        private String user;
        private String password;
        private String schema;
        private String includes;
        private String excludes;

        public String getDriver() {
            return driver;
        }

        public String getUrl() {
            return url;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public String getSchema() {
            return schema;
        }

        public String getIncludes() {
            return includes;
        }

        public String getExcludes() {
            return excludes;
        }
    }

    public static class Output {
        private String packageName;
        private String directory = "build/generated/source/jooq";

        public String getPackageName() {
            return packageName;
        }

        public String getDirectory() {
            return directory;
        }
    }

   public static class ForcedTypeContainer {
        private final static String TIMESTAMP_INSTANT_CONVERTER = "io.github.alexritian.codegen.converter.TimestampInstantConverter";
        private final static String TIMESTAMP_TYPE = "(?i)TIMESTAMP(_WITH(_TIME)?_ZONE|_TZ|TZ)?(\\(.*\\))?";
        private final NamedDomainObjectContainer<ForcedType> forcedTypes;

        public ForcedTypeContainer(Project project) {
            this.forcedTypes = project.container(ForcedType.class);
        }

        public NamedDomainObjectContainer<ForcedType> getForcedTypes() {
            return this.forcedTypes;
        }

        public void timestampToInstant() {
            var forcedType = new ForcedType();
            forcedType.setConverter(TIMESTAMP_INSTANT_CONVERTER);
            forcedType.setIncludeExpression(".*\\.*");
            forcedType.setUserType(Instant.class.getName());
            forcedType.setIncludeTypes(TIMESTAMP_TYPE);
            generateName(forcedType);
            forcedTypes.add(forcedType);
        }

        private String generateName(String converter, String expr, String userType, String includeTypes) {
            return converter + expr + userType + includeTypes;
        }

        private void generateName(ForcedType forcedType) {
            forcedType.setName(forcedType.getConverter() + forcedType.getIncludeExpression() + forcedType.getUserType() + forcedType.getIncludeTypes());
        }
   }

}
