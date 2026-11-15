package com.classicmotor.classic_motor_backend.controller;

import com.classicmotor.classic_motor_backend.model.Vehicles;
import com.classicmotor.classic_motor_backend.repository.VehiclesRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehiclesController {

    private final VehiclesRepository repository;

    // Constructor: conexión con el repositorio
    public VehiclesController(VehiclesRepository repository) {
        this.repository = repository;
    }

    // GET /api/v1/vehicles
    // Devuelve los vehículos paginados
    @GetMapping
    public ResponseEntity<PagedModel<Vehicles>> listVehicles(
            @PageableDefault(size = 20) Pageable pageable) {

        // Validar los parámetros de paginación
        if (pageable.getPageSize() > 100) {
            return ResponseEntity.badRequest().build();
        }

        // Obtener los vehículos desde MySQL
        List<Vehicles> vehicles = repository.findAll();

        // Calcular los índices de la página solicitada
        int total = vehicles.size();
        long offset = pageable.getOffset();

        int inicio = (int) Math.min(offset, total);
        int fin = (int) Math.min(
                offset + pageable.getPageSize(), total
        );

        // Construir la página de resultados
        Page<Vehicles> page = new PageImpl<>(
                vehicles.subList(inicio, fin),
                pageable,
                total
        );

        // Devolver HTTP 200 con el JSON paginado
        return ResponseEntity.ok(new PagedModel<>(page));
    }
}
