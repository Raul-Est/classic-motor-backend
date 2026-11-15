package com.classicmotor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Modelo Vehículo
 * Representa un automóvil en el rally
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Vehiculo {
    private Integer idVehiculo;       // PK en BD
    private String matricula;         // Matrícula del vehículo (ej: "ABC-1234")
    private String modelo;            // Nombre modelo (ej: "Jaguar E-Type")
    private Integer anno;             // Año fabricación
    private Integer potencia;         // CV del motor
    private Integer fkIdCategoria;    // FK a categorias
    private Integer fkIdEvento;       // FK a eventos

    // Campo opcional (no en BD, rellenado en JOIN)
    private String categoriaNombre;   // Nombre categoría (para mostrar en frontend)
}
