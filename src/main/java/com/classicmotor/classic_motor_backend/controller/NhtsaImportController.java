
package com.classicmotor.classic_motor_backend.controller;

import com.classicmotor.classic_motor_backend.service.NhtsaImportService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/imports/nhtsa")
public class NhtsaImportController {

    private final NhtsaImportService service;

    public NhtsaImportController(NhtsaImportService service) {
        this.service = service;
    }

    // Importar modelos de Jaguar o Porsche
    @PostMapping("/{make}")
    public ResponseEntity<?> importMake(
            @PathVariable String make) {

        // Limitar las pruebas a dos marcas
        if (!make.equalsIgnoreCase("Jaguar") &&
            !make.equalsIgnoreCase("Porsche")) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Marca no permitida")
            );
        }

        int inserted = service.importMake(make);

        return ResponseEntity.ok(Map.of(
            "make", make,
            "inserted", inserted
        ));
    }
}
