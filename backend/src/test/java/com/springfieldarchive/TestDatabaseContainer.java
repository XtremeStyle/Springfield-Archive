package com.springfieldarchive;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Contenedor PostgreSQL 18 compartido por todas las pruebas Spring (Testcontainers).
 * Evita que las pruebas dependan de una base de datos local preexistente.
 * Docker es un requisito de las pruebas de este proyecto.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestDatabaseContainer {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18");
    }
}
