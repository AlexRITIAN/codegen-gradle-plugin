package io.github.alexritian.codegen;

import nu.studer.gradle.jooq.JooqEdition;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * @author Too_young
 */
public class CodegenExtension {

    private static final String DEFAULT_VERSION = "3.19.1";
    private static final JooqEdition DEFAULT_EDITION = JooqEdition.OSS;

    private final Property<Boolean> useContainer;
    private final Property<String> version;
    private final Property<JooqEdition> edition;
    private final NamedDomainObjectContainer<CodegenConfig> configurations;

    @Inject
    public CodegenExtension(ObjectFactory objects, Project project) {
        this.useContainer = objects.property(Boolean.class).convention(true);
        this.version = objects.property(String.class).convention(DEFAULT_VERSION);
        this.edition = objects.property(JooqEdition.class).convention(DEFAULT_EDITION);
        this.configurations = objects.domainObjectContainer(CodegenConfig.class, name -> objects.newInstance(CodegenConfig.class, name));

        version.finalizeValueOnRead();
        edition.finalizeValueOnRead();
    }

    @SuppressWarnings("unused")
    public Property<Boolean> getUseContainer() {
        return useContainer;
    }

    @SuppressWarnings("unused")
    public Property<String> getVersion() {
        return version;
    }

    @SuppressWarnings("unused")
    public Property<JooqEdition> getEdition() {
        return edition;
    }

    @SuppressWarnings("unused")
    public NamedDomainObjectContainer<CodegenConfig> getConfigurations() {
        return configurations;
    }

}
