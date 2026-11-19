package com.springfieldarchive;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración contra PostgreSQL 18 REAL (Testcontainers).
 * Base descartable y vacía; Flyway migra desde cero; sin H2 ni datos externos.
 */
@SpringBootTest(properties = "spring.profiles.active=test")
@Import(TestDatabaseContainer.class)
class CatalogMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void containerIsPostgres18() {
        String version = jdbc.queryForObject("SHOW server_version", String.class);
        assertThat(version).startsWith("18");
    }

    @Test
    void tablesExistWithExpectedStructure() {
        Integer tables = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables " +
                        "WHERE table_schema='public' AND table_name IN ('characters','locations')",
                Integer.class);
        assertThat(tables).isEqualTo(2);

        // Columnas obligatorias
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns " +
                        "WHERE table_name='characters'", Integer.class)).isEqualTo(15);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns " +
                        "WHERE table_name='locations'", Integer.class)).isEqualTo(12);

        // Tipo de identidad y timestamptz
        assertThat(jdbc.queryForObject(
                "SELECT data_type FROM information_schema.columns " +
                        "WHERE table_name='characters' AND column_name='id'", String.class)).isEqualTo("bigint");
        assertThat(jdbc.queryForObject(
                "SELECT data_type FROM information_schema.columns " +
                        "WHERE table_name='characters' AND column_name='created_at'", String.class))
                .isEqualTo("timestamp with time zone");
    }

    @Test
    void flywayAppliedMigrationsExactlyOnceAndValidates() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
                .isEqualTo(8);
        // validate() comprueba que el esquema real coincide con el historial
        assertThatCode(() -> flyway.validate()).doesNotThrowAnyException();
        // Una nueva ejecución de migrate() no aplica nada (idempotente), sin duplicar datos
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void slugConstraintsAreEnforced() {
        // Inválido: mayúsculas y espacios
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,status,description) VALUES('Homer Simpson','Homer Simpson','Alive','x')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO locations(slug,name,type,description) VALUES('Moe', 'Moe','landmark','x')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // Válidos
        jdbc.update("INSERT INTO characters(slug,name,status,description) VALUES('homer-simpson','Homer Simpson','Alive','Jefe de seguridad en la Planta Nuclear.')");
        jdbc.update("INSERT INTO locations(slug,name,type,description) VALUES('moe-s-tavern','Taberna de Moe','business','Lugar emblemático de reunión.')");
        // Duplicado (unicidad)
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,status,description) VALUES('homer-simpson','Otro','Alive','y')"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void blankNameAndDescriptionAreRejected() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,status,description) VALUES('a-name','','Alive','x')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,status,description) VALUES('a-name','   ','Alive','x')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,status,description) VALUES('a-name','X','Alive','   ')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO locations(slug,name,type,description) VALUES('a-loc','','park','x')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO locations(slug,name,type,description) VALUES('a-loc','X','park','')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void statusTypeAndOccupationConstraintsAreEnforced() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,status,description) VALUES('a-status','X','Dead-','x')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,status,description) VALUES('a-status2','X','alive','x')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO locations(slug,name,type,description) VALUES('a-type','X','restaurant','x')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,occupation,status,description) VALUES('a-occ','X','   ','Alive','x')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // Valores válidos: occupation NULL y con texto real
        jdbc.update("INSERT INTO characters(slug,name,occupation,status,description) VALUES('valid-a','Valida',NULL,'Alive','Desc válida')");
        jdbc.update("INSERT INTO characters(slug,name,occupation,status,description) VALUES('valid-b','Valida','Inspector de seguridad','Unknown','Desc válida')");
    }

    @Test
    void whitespaceOnlyValuesAreRejected() {
        // Nombre compuesto exclusivamente por tabulaciones
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,status,description) VALUES('ws-tabs',E'\\t\\t','Alive','Desc válida')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // Descripción compuesta exclusivamente por saltos de línea
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,status,description) VALUES('ws-newline','Nombre válido','Alive',E'\\n\\n')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // Location también rechaza nombre ni descripción de puras tabulaciones/saltos de línea
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO locations(slug,name,type,description) VALUES('ws-loc',E'\\t','park','Desc válida')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO locations(slug,name,type,description) VALUES('ws-loc2','Nombre válido','park',E'\\n')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // Occupation compuesta exclusivamente por espacios y tabulaciones (si no es NULL)
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO characters(slug,name,occupation,status,description) VALUES('ws-occ','Nombre válido',E' \\t ','Alive','Desc válida')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        // Valores válidos siguen siendo aceptados
        jdbc.update("INSERT INTO characters(slug,name,occupation,status,description) VALUES('ws-ok-a','Nombre válido',NULL,'Alive','Desc válida')");
        jdbc.update("INSERT INTO characters(slug,name,occupation,status,description) VALUES('ws-ok-b','Nombre válido',E'\\t\\tEscapado','Alive','Desc válida con tabs alrededor')");
    }

    @BeforeEach
    void cleanRows() {
        // Quitar filas de pruebas para que sean deterministas y orden-independientes
        jdbc.update("DELETE FROM characters WHERE slug LIKE 'h-integration-%'");
        jdbc.update("DELETE FROM characters WHERE slug LIKE 'h4-%'");
        jdbc.update("DELETE FROM characters WHERE slug IN ('homer-simpson','a-name','a-status','a-status2','a-occ','valid-a','valid-b','ws-tabs','ws-newline','ws-occ','ws-ok-a','ws-ok-b')");
        jdbc.update("DELETE FROM locations WHERE slug LIKE 'h-integration-%' OR slug LIKE 'h4-%' OR slug IN ('moe-s-tavern','a-loc','ws-loc','ws-loc2')");
    }

    @org.junit.jupiter.api.AfterEach
    void cleanRowsAfter() {
        // Dejar la base igual que en beforeEach: ninguna fila de prueba queda para otras clases
        cleanRows();
    }
}
