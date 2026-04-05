package com.finance.controller;

import com.finance.dto.response.ApiResponse;
import com.finance.dto.response.DashboardResponse;
import com.finance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated financial summaries — all authenticated roles")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get full dashboard summary: totals, category breakdown, trends")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved",
                dashboardService.getSummary()));
    }
}
