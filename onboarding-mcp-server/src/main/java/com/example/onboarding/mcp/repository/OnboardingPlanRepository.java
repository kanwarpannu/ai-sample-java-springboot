package com.example.onboarding.mcp.repository;

import com.example.onboarding.mcp.domain.OnboardingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OnboardingPlanRepository extends JpaRepository<OnboardingPlan, UUID> {
}
