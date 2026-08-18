package com.example.onboarding.mcp.repository;

import com.example.onboarding.mcp.domain.Blocker;
import com.example.onboarding.mcp.domain.OnboardingStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockerRepository extends JpaRepository<Blocker, Long> {

    List<Blocker> findByStep(OnboardingStep step);

    List<Blocker> findByStepAndResolved(OnboardingStep step, boolean resolved);
}
