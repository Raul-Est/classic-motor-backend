package com.classicmotor.classic_motor_backend.service;

import com.classicmotor.classic_motor_backend.model.VehicleImage;
import com.classicmotor.classic_motor_backend.repository.VehicleImagesRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class WikimediaImportService {

    private final VehicleImagesRepository repository;
    private final RestClient client;

    // Inyeccion del repositorio y configuracion del cliente HTTP
    public WikimediaImportService(VehicleImagesRepository repository) {
        this.repository = repository;
        this.client = RestClient.builder()
        .baseUrl("https://commons.wikimedia.org")
        .defaultHeader("User-Agent", "ClassicMotorBackend/1.0 Project/Joyfe DAW")
        .build();
    }
    // buscar fotografias y guardarlas pendientes de revision
    public int importImages(Integer vehicleId, String search) {

        if (vehicleId == null || vehicleId <= 0 ||
            search == null || search.isBlank()) {
            throw new IllegalArgumentException(
                "Busqueda no valida"
            );
        }
        Integer id = vehicleId; // Asignar vehicleId a id para mantener consistencia con el resto del código
        // GET a wikipedia commons
        Map<?,?> response = client.get()
            .uri(builder -> builder
                .path("/w/api.php")
                .queryParam("action", "query")
                .queryParam("generator", "search")
                .queryParam("gsrsearch", search)
                .queryParam("gsrnamespace", "6") // Limitar la búsqueda a archivos de Wikimedia Commons
                .queryParam("gsrlimit", "10")
                .queryParam("prop", "imageinfo")
                .queryParam("iiprop", "url|extmetadata|mime")
                .queryParam("format", "json")
                .build()
            )
            .retrieve()
            .body(Map.class);
        if(response == null) {
            return 0;
        }
        Map<?, ?> query = asMap(response.get("query"));
        Map<?, ?> pages = asMap(query.get("pages"));

        int procesadas = 0;

        for (Object pageObject : pages.values()) {

            Map<?, ?> page = asMap(pageObject);
            Object infosObject = page.get("imageinfo");

            if (!(infosObject instanceof List<?> infos)
                    || infos.isEmpty()) {
                continue;
            }

            Map<?, ?> info = asMap(infos.get(0));

            String url = string(info.get("url"));
            String mime = string(info.get("mime"));

            // Aceptar únicamente fotografías JPEG, PNG y WebP
            if (url == null || url.length() > 500 ||
                    !List.of("image/jpeg", "image/png", "image/webp")
                            .contains(mime)) {
                continue;
            }

            Map<?, ?> metadata = asMap(info.get("extmetadata"));

            String author = metadataValue(metadata, "Artist");
            String license = metadataValue(metadata, "LicenseShortName");
            String licenseUrl = metadataValue(metadata, "LicenseUrl");

            // Crear el modelo con los datos obtenidos
            VehicleImage image = new VehicleImage();

            image.setVehicleId(id);
            image.setUrl(url);
            image.setTextoAlternativo(
                limit(string(page.get("title")), 255)
            );

            image.setUrlFuente(
                limit(string(info.get("descriptionurl")), 500)
            );

            image.setAutor(limit(author, 180));
            image.setLicencia(limit(license, 120));
            image.setUrlLicencia(limit(licenseUrl, 500));
            image.setAtribucion(author);

            // Las imágenes no se publican automáticamente
            image.setLicenciaVerificada(false);
            image.setPublicada(false);
            image.setOrden(0);

            repository.save(image);
            procesadas++;
        }

        return procesadas;
    }

    // Convertir un objeto JSON en un mapa
    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    // Convertir un valor JSON en texto
    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    // Obtener un valor de los metadatos de Wikimedia
    private String metadataValue(Map<?, ?> metadata, String key) {
        Map<?, ?> item = asMap(metadata.get(key));
        return string(item.get("value"));
    }

    // Limitar longitud para respetar las columnas MySQL
    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }

        return value.length() <= max
                ? value
                : value.substring(0, max);

    }
}