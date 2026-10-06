package com.tdcostmanager.backend.application.controller;

import com.tdcostmanager.backend.application.dto.ToolCreateRequest;
import com.tdcostmanager.backend.application.dto.ToolResponse;
import com.tdcostmanager.backend.application.dto.ToolUpdateRequest;
import com.tdcostmanager.backend.application.service.ToolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final ToolService service;

    public ToolController(ToolService service) {
        this.service = service;
    }

    @GetMapping
    public List<ToolResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ToolResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ToolResponse create(@Valid @RequestBody ToolCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ToolResponse update(@PathVariable Long id, @Valid @RequestBody ToolUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
