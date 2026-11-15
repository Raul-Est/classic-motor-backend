package com.classicmotor.classic_motor_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
/*
 * Modelo Vehículo
 * Representa un automóvil en el rally
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Vehicle {
    private Integer id;       // PK en BD
    private String slug;              // Slug del vehículo (ej: "jaguar-e-type")
    private String marca;            // Marca del vehículo (ej: "Jaguar")   
    private String modelo;            // Nombre modelo (ej: "Jaguar E-Type")
    private String version;           // Versión del vehículo (ej: "Series 1")
    private Integer anio;             // Año fabricación

    // Relacion con la tabla de categorías FK
    private Integer categoriaId;      // ID de la categoría del 
    // Caracteristicas técnicas del vehículo
    private Integer potenciaCv;         // CV del motor
    private Integer cilindradaCc;       // Cilindrada del motor en cc
    private String combustible;        // Tipo de combustible (ej: "Gasolina")
    private String transmision;       // Tipo de transmisión (ej: "Manual")
    private String traccion;          // Tipo de tracción (ej: "Trasera")
    // Información histórica del vehículo
    private String historial;       // Historia y trayectoria del vehículo
    private String descripcion;     // Descripción general del vehículo
    // Visibilidad del vehículo (ej: "Público", "Privado")
    private Boolean destacado;        // Indica si el vehículo está destacado (true/false)
    private Boolean publicado;        // Indica si el vehículo está publicado (true/false)
    // Fechas de registro
    private LocalDateTime creadoEn;   // Fecha de creación del registro
    private LocalDateTime actualizadoEn; // Fecha de última actualización del registro
}
