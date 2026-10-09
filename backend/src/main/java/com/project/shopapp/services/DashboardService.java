package com.project.shopapp.services;

import com.project.shopapp.responses.DashboardResponse;
import java.time.LocalDate;

public interface DashboardService {
    DashboardResponse report(LocalDate from, LocalDate to);
}
