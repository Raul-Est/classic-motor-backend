
package com.classicmotor.classic_motor_backend.service;

import com.classicmotor.classic_motor_backend.repository.NhtsaImportRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class NhtsaImportService {

    private final RestClient client;
    private final NhtsaImportRepository repository;

    // Configuración e inyección de dependencias
    public NhtsaImportService(NhtsaImportRepository repository) {
        this.repository = repository;

        this.client = RestClient.builder()
            .baseUrl("https://vpic.nhtsa.dot.gov/api")
            .build();
    }

    // Obtener los modelos de una marca desde NHTSA
    public List<?> getModels(String make) {

        if (make == null || make.isBlank()) {
            throw new IllegalArgumentException(
                "La marca es obligatoria"
            );
        }

        Map<?, ?> response = client.get()
            .uri(builder -> builder
                .path("/vehicles/GetModelsForMake/{make}")
                .queryParam("format", "json")
                .build(make))
            .retrieve()
            .body(Map.class);

        if (response == null) {
            return List.of();
        }

        Object results = response.get("Results");

        if (results instanceof List<?> models) {
            return models;
        }

        return List.of();
    }

    // Importar los modelos de una marca a MySQL
    public int importMake(String make) {

        if (make == null || make.isBlank()) {
            throw new IllegalArgumentException(
                "La marca es obligatoria"
            );
        }

        if (!repository.isEnabled()) {
            throw new IllegalStateException(
                "La fuente NHTSA está deshabilitada"
            );
        }

        String resource = "/vehicles/GetModelsForMake/" + make;

        // Consultar API externa
        List<?> models = getModels(make);

        // Registrar inicio de importación
        Integer importId = repository.startImport(resource);

        int read = 0;
        int inserted = 0;

        try {

            // Recorrer los modelos recibidos
            for (Object item : models) {

                if (!(item instanceof Map<?, ?> model)) {
                    continue;
                }

                read++;

                Object modelId = model.get("Model_ID");
                Object makeId = model.get("Make_ID");

                String modelName = text(model.get("Model_Name"));
                String makeName = text(model.get("Make_Name"));

                // Comprobar los datos obligatorios
                if (modelId == null || makeId == null ||
                    modelName == null || makeName == null) {
                    continue;
                }

                // Evitar marcas con nombres parecidos
                if (!makeName.equalsIgnoreCase(make)) {
                    continue;
                }

                if (makeName.length() > 100 ||
                    modelName.length() > 120) {
                    continue;
                }

                // Identificador único de NHTSA
                String externalId = makeId + "-" + modelId;

                // Huella SHA-256 de los datos
                String hash = sha256(
                    externalId + "|" + makeName + "|" + modelName
                );

                // Guardar en MySQL sin duplicados
                int result = repository.saveModel(
                    importId,
                    externalId,
                    makeName,
                    modelName,
                    hash
                );

                if (result == 1) {
                    inserted++;
                }
            }

            // Registrar finalización
            repository.finishImport(importId, read, inserted);

            return inserted;

        } catch (RuntimeException e) {

            // Registrar fallo y conservar el error original
            try {
                repository.failImport(
                    importId,
                    "IMPORT_ERROR"
                );
            } catch (RuntimeException failure) {
                e.addSuppressed(failure);
            }

            throw e;
        }
    }

    // Convertir un valor recibido en texto válido
    private String text(Object value) {

        if (value == null) {
            return null;
        }

        String result = value.toString().trim();

        return result.isEmpty() ? null : result;
    }

    // Calcular SHA-256 sin bibliotecas externas
    private String sha256(String value) {

        try {
            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "No se pudo calcular el hash SHA-256", e
            );
        }
    }
}
