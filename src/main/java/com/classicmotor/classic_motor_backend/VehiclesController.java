package com.classicmotor.classic_motor_backend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehiclesController {

    @GetMapping
    public PagedModel<?> listVehicles(
            @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(Page.empty(pageable));
    }
}