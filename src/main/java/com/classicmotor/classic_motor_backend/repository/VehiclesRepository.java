package com.classicmotor.classic_motor_backend.repository;

import com.classicmotor.classic_motor_backend.model.Vehicle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VehiclesRepository {

    private final JdbcTemplate jdbc;

    public VehiclesRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Obtener todos los vehículos de MySQL
    public List<Vehicle> findAll() {
        String sql = "SELECT * FROM vehiculos ORDER BY id";
        return jdbc.query(sql, (rs, rowNum) -> {

            Vehicle vehiculo = new Vehicle();
            
            // Identificar el vehículo en la base de datos por su ID y slug
            vehiculo.setId(rs.getInt("id"));
            vehiculo.setSlug(rs.getString("slug"));
            // Información básica del vehículo
            vehiculo.setMarca(rs.getString("marca"));
            vehiculo.setModelo(rs.getString("modelo"));
            vehiculo.setVersion(rs.getString("version"));
            vehiculo.setAnio(rs.getInt("anio"));
            vehiculo.setCategoriaId(rs.getInt("categoria_id"));
            // Características técnicas del vehículo
            vehiculo.setPotenciaCv(rs.getObject("potencia_cv", Integer.class));
            vehiculo.setCilindradaCc(rs.getObject("cilindrada_cc", Integer.class));
            vehiculo.setCombustible(rs.getString("combustible"));
            vehiculo.setTransmision(rs.getString("transmision"));
            vehiculo.setTraccion(rs.getString("traccion"));
            
            // Información histórica
            vehiculo.setHistorial(rs.getString("historial"));
            vehiculo.setDescripcion(rs.getString("descripcion"));
            // Visibilidad del vehículo (destacado y publicado) 
            vehiculo.setDestacado(rs.getBoolean("destacado"));
            vehiculo.setPublicado(rs.getBoolean("publicado"));
            vehiculo.setCreadoEn(rs.getTimestamp("creado_en").toLocalDateTime());
            vehiculo.setActualizadoEn(rs.getTimestamp("actualizado_en").toLocalDateTime());
            return vehiculo;
        });
    }

}
