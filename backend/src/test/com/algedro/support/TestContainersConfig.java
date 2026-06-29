package com.algedro.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@SpringBootTest
public abstract class TestContainersConfig {

    private static final String DATABASE_NAME = "algedro_test";
    private static final String USERNAME = "algedro_test";
    private static final String PASSWORD = "algedro_test";
    private static final String CONTAINER_NAME = "algedro-test-postgres-" + UUID.randomUUID();
    private static final int HOST_PORT = findFreePort();

    private static volatile boolean started;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        if (ensurePostgresStarted()) {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:" + HOST_PORT + "/" + DATABASE_NAME);
            registry.add("spring.datasource.username", () -> USERNAME);
            registry.add("spring.datasource.password", () -> PASSWORD);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
            return;
        }

        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:algedro_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    private static synchronized boolean ensurePostgresStarted() {
        if (started) {
            return true;
        }

        try {
            runCommand(List.of(
                    "docker", "run", "--rm", "-d",
                    "--name", CONTAINER_NAME,
                    "-e", "POSTGRES_DB=" + DATABASE_NAME,
                    "-e", "POSTGRES_USER=" + USERNAME,
                    "-e", "POSTGRES_PASSWORD=" + PASSWORD,
                    "-p", HOST_PORT + ":5432",
                    "postgres:16"
            ));

            waitForDatabase();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopContainerQuietly(CONTAINER_NAME)));
            started = true;
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static void waitForDatabase() {
        String jdbcUrl = "jdbc:postgresql://localhost:" + HOST_PORT + "/" + DATABASE_NAME;
        Instant deadline = Instant.now().plus(Duration.ofSeconds(45));

        while (Instant.now().isBefore(deadline)) {
            try (Connection ignored = DriverManager.getConnection(jdbcUrl, USERNAME, PASSWORD)) {
                return;
            } catch (SQLException exception) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrumpido esperando PostgreSQL de integración", interruptedException);
                }
            }
        }

        throw new IllegalStateException("PostgreSQL de integración no estuvo listo a tiempo");
    }

    private static void stopContainerQuietly(String containerName) {
        try {
            runCommand(List.of("docker", "stop", containerName));
        } catch (RuntimeException ignored) {
            // Best-effort cleanup for the transient integration container.
        }
    }

    private static void runCommand(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new IllegalStateException("Fallo ejecutando comando Docker: " + output);
            }
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("No se pudo preparar PostgreSQL de integración", exception);
        }
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo reservar un puerto libre para PostgreSQL de integración", exception);
        }
    }
}
