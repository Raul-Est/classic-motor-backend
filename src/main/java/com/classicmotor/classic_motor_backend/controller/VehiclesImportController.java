package com.classicmotor.classic_motor_backend.controller;

import com.classicmotor.classic_motor_backend.service.WikimediaImportService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/imports/vehicles")
public class VehiclesImportController {

    private final WikimediaImportService service;

    public VehiclesImportController(WikimediaImportService service) {
        this.service = service;
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<?> importImages(
      @PathVariable Integer id) {

        int procesadas = service.importImages(
            id,
            "Jaguar E-Type Series 1 1967"
        );

        return ResponseEntity.ok(
            Map.of(
                "vehicleId", id,
                "processed", procesadas
            )
        );
    }

}
