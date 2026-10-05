package com.tdcostmanager.backend.application.controller;

import com.tdcostmanager.backend.application.dto.QuoteCreateRequest;
import com.tdcostmanager.backend.application.dto.QuoteResponse;
import com.tdcostmanager.backend.application.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse create(@PathVariable Long projectId, @Valid @RequestBody QuoteCreateRequest request) {
        return quoteService.generateQuote(projectId, request);
    }

    @GetMapping
    public List<QuoteResponse> getByProject(@PathVariable Long projectId) {
        return quoteService.findByProjectId(projectId);
    }

    @GetMapping("/{quoteId}")
    public QuoteResponse getById(@PathVariable Long quoteId) {
        return quoteService.findById(quoteId);
    }
}
