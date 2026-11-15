package com.classicmotor.classic_motor_backend.repository;
import com.classicmotor.classic_motor_backend.model.VehicleImage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VehicleImagesRepository {

  private final JdbcTemplate jdbc;

  // Inyeccion de dependencias del JdbcTemplate
  public VehicleImagesRepository(JdbcTemplate jdbc) {
      this.jdbc = jdbc;
  }
  // Obtener la imagen del vehículo por su ID
  public List<VehicleImage> findByVehicleId(Integer vehicleId) {
      String sql = "SELECT * FROM imagenes_vehiculo WHERE vehiculo_id = ? ORDER BY orden, id";
        return jdbc.query(sql, (rs, rowNum) -> {

        VehicleImage image = new VehicleImage();

        // Identificación
        image.setId(rs.getInt("id"));
        image.setVehicleId(rs.getInt("vehiculo_id"));

        // Información de la imagen
        image.setUrl(rs.getString("url"));
        image.setTextoAlternativo(
            rs.getString("texto_alternativo")
        );
        image.setUrlFuente(rs.getString("url_fuente"));
        image.setAutor(rs.getString("autor"));

        // Derechos y licencias
        image.setLicencia(rs.getString("licencia"));
        image.setUrlLicencia(
            rs.getString("url_licencia")
        );
        image.setAtribucion(
            rs.getString("atribucion")
        );
        image.setLicenciaVerificada(
            rs.getBoolean("licencia_verificada")
        );

        // Publicación y orden
        image.setPublicada(rs.getBoolean("publicada"));
        image.setOrden(rs.getInt("orden"));

        return image;
        // vehicleId se pasa como parámetro al query
      }, vehicleId);

    }

  // Guardar una imagen pendiente de revisión
  public void save(VehicleImage image) {

        String sql = """
            INSERT INTO imagenes_vehiculo (
                vehiculo_id,
                url,
                texto_alternativo,
                url_fuente,
                autor,
                licencia,
                url_licencia,
                atribucion,
                licencia_verificada,
                publicada,
                orden
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, FALSE, ?)
            ON DUPLICATE KEY UPDATE id = id
            """;

        jdbc.update(
            sql,
            image.getVehicleId(),
            image.getUrl(),
            image.getTextoAlternativo(),
            image.getUrlFuente(),
            image.getAutor(),
            image.getLicencia(),
            image.getUrlLicencia(),
            image.getAtribucion(),
            image.getOrden() == null ? 0 : image.getOrden()
        );
    
    }
}
