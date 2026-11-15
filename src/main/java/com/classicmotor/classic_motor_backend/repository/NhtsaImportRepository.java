
package com.classicmotor.classic_motor_backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.util.UUID;

/**
 * Repositorio de importaciones desde NHTSA.
 *
 * Gestiona las tablas:
 * - fuentes_datos
 * - importaciones_api
 * - vehiculos_importados
 *
 * Utiliza Spring JDBC y MySQL.
 */
@Repository
public class NhtsaImportRepository {

    private final JdbcTemplate jdbc;

    // Inyección de dependencias
    public NhtsaImportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Obtener el identificador de la fuente NHTSA.
     */
    public Integer getSourceId() {

        String sql = """
            SELECT id
            FROM fuentes_datos
            WHERE codigo = ?
            """;

        return jdbc.queryForObject(
            sql,
            Integer.class,
            "nhtsa-vpic"
        );
    }

    /**
     * Comprobar si la fuente está habilitada.
     */
    public boolean isEnabled() {

        String sql = """
            SELECT habilitada
            FROM fuentes_datos
            WHERE codigo = ?
            """;

        Boolean enabled = jdbc.queryForObject(
            sql,
            Boolean.class,
            "nhtsa-vpic"
        );

        return Boolean.TRUE.equals(enabled);
    }

    /**
     * Registrar el inicio de una importación.
     *
     * Devuelve el ID generado por MySQL.
     */
    public Integer startImport(String resource) {

        if (!isEnabled()) {
            throw new IllegalStateException(
                "La fuente NHTSA no está habilitada"
            );
        }

        String sql = """
            INSERT INTO importaciones_api (
                fuente_id,
                clave_idempotencia,
                recurso_solicitado
            )
            VALUES (?, ?, ?)
            """;

        Integer sourceId = getSourceId();

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {

            var ps = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            );

            ps.setInt(1, sourceId);
            ps.setString(2, UUID.randomUUID().toString());
            ps.setString(3, resource);

            return ps;

        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException(
                "No se pudo obtener el ID de importación"
            );
        }

        return key.intValue();
    }

    /**
     * Guardar un modelo obtenido de NHTSA.
     *
     * No modifica los vehículos aprobados.
     * Los registros nuevos quedan pendientes de revisión.
     *
     * Devuelve:
     * 1 = insertado
     * 0 = ya existía
     */
    // public int saveModel(
    //         Integer importId,
    //         String externalId,
    //         String make,
    //         String model,
    //         String hash) {

    //     String sql = """
    //         INSERT INTO vehiculos_importados (
    //             fuente_id,
    //             importacion_id,
    //             identificador_externo,
    //             marca,
    //             modelo,
    //             hash_datos,
    //             estado_revision
    //         )
    //         VALUES (?, ?, ?, ?, ?, ?, 'pendiente')
    //         ON DUPLICATE KEY UPDATE id = id
    //         """;

    //     return jdbc.update(
    //         sql,
    //         getSourceId(),
    //         importId,
    //         externalId,
    //         make,
    //         model,
    //         hash
    //     );
    // }

    
    // Guarda un modelo de NHTSA sin duplicarlo.
    //   1 = registro insertado
    //   0 = registro existente
     
    public int saveModel(
            Integer importId,
            String externalId,
            String make,
            String model,
            String hash) {

        String sql = """
            INSERT INTO vehiculos_importados (
                fuente_id,
                importacion_id,
                identificador_externo,
                marca,
                modelo,
                hash_datos,
                estado_revision
            )
            VALUES (?, ?, ?, ?, ?, ?, 'pendiente')
            """;

        try {
            return jdbc.update(
                sql,
                getSourceId(),
                importId,
                externalId,
                make,
                model,
                hash
            );

        } catch (org.springframework.dao.DuplicateKeyException e) {
            // El modelo ya existe: no insertar nuevamente
            return 0;
        }
    }



    /**
     * Finalizar una importación correctamente.
     */
    public void finishImport(
            Integer importId,
            int read,
            int inserted) {

        String sql = """
            UPDATE importaciones_api
            SET estado = 'completada',
                registros_leidos = ?,
                registros_insertados = ?,
                finalizada_en = CURRENT_TIMESTAMP
            WHERE id = ?
              AND fuente_id = ?
              AND estado = 'iniciada'
            """;

        jdbc.update(
            sql,
            read,
            inserted,
            importId,
            getSourceId()
        );
    }

    /**
     * Registrar una importación fallida.
     */
    public void failImport(
            Integer importId,
            String errorCode) {

        String sql = """
            UPDATE importaciones_api
            SET estado = 'fallida',
                codigo_error = ?,
                finalizada_en = CURRENT_TIMESTAMP
            WHERE id = ?
              AND fuente_id = ?
              AND estado = 'iniciada'
            """;

        jdbc.update(
            sql,
            errorCode,
            importId,
            getSourceId()
        );
    }
}
