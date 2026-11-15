package com.classicmotor.classic_motor_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//  Modelo de imagen de vehículo.
//  Representa un registro de la tabla imagenes_vehiculo.
//  Almacena referencias a imágenes y sus licencias.

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleImage {
    //identificador único de la imagen de vehículo.
    private Integer id; // Pk
    private Integer vehicleId; //fk a vehiculo
    // Información de la imagen
    private String url;          // URL de la imagen
    private String textoAlternativo; // Descripción accesible
    private String urlFuente;     // URL de origen
    private String autor;       // Autor de la fotografía

    // Derechos y licencias
    private String licencia;  // Tipo de licencia
    private String urlLicencia;  // Enlace a la licencia
    private String atribucion;     // Texto de atribución
    private Boolean licenciaVerificada; // Licencia revisada

    // Publicación y orden
    private Boolean publicada;     // Visible en la web
    private Integer orden;      // Orden de visualización
}


