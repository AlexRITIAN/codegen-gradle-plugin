package io.github.alexritian.codegen.containers;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author Too_young
 */
public abstract class PostgresServer implements BuildService<BuildServiceParameters.None>, AutoCloseable {
    private final PostgreSQLContainer<?> container;

    public PostgresServer() {
        container = new PostgreSQLContainer<>("postgres");
        container.start();
    }

    @Override
    public void close() throws Exception {
        container.stop();
    }

    public PostgreSQLContainer<?> getContainer() {
        return container;
    }

    public static Provider<PostgresServer> getProvider(Project project) {
        return project.getGradle().getSharedServices().registerIfAbsent("postgres", PostgresServer.class);
    }

}
