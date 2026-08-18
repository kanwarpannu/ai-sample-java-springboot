package com.example.onboarding.mcp.repository;

import com.example.onboarding.mcp.domain.StepTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StepTemplateRepository extends JpaRepository<StepTemplate, Long> {

    List<StepTemplate> findByRoleOrderByStepNumber(String role);
}
