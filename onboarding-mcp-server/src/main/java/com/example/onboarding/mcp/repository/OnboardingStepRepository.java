package com.example.onboarding.mcp.repository;

import com.example.onboarding.mcp.domain.OnboardingPlan;
import com.example.onboarding.mcp.domain.OnboardingStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingStepRepository extends JpaRepository<OnboardingStep, Long> {

    List<OnboardingStep> findByPlanOrderByStepNumber(OnboardingPlan plan);

    Optional<OnboardingStep> findByPlanAndStepNumber(OnboardingPlan plan, int stepNumber);
}
