package com.example.onboarding.mcp.repository;

import com.example.onboarding.mcp.domain.OnboardingPlan;
import com.example.onboarding.mcp.domain.OnboardingStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OnboardingStepRepositoryTest {

    @Autowired
    private OnboardingPlanRepository planRepository;

    @Autowired
    private OnboardingStepRepository stepRepository;

    private OnboardingPlan savedPlan;

    @BeforeEach
    void setUp() {
        savedPlan = planRepository.save(OnboardingPlan.builder()
                .developerName("Eve Brown")
                .role("BACKEND_ENGINEER")
                .steps(new ArrayList<>())
                .build());
    }

    @Test
    void findByPlanOrderByStepNumber_returnsStepsInAscendingOrder() {
        stepRepository.save(step(savedPlan, 3, "Third Step"));
        stepRepository.save(step(savedPlan, 1, "First Step"));
        stepRepository.save(step(savedPlan, 2, "Second Step"));

        List<OnboardingStep> steps = stepRepository.findByPlanOrderByStepNumber(savedPlan);

        assertThat(steps).hasSize(3);
        assertThat(steps.get(0).getStepNumber()).isEqualTo(1);
        assertThat(steps.get(1).getStepNumber()).isEqualTo(2);
        assertThat(steps.get(2).getStepNumber()).isEqualTo(3);
    }

    @Test
    void findByPlanAndStepNumber_returnsCorrectStep() {
        stepRepository.save(step(savedPlan, 1, "Step One"));
        stepRepository.save(step(savedPlan, 2, "Step Two"));

        assertThat(stepRepository.findByPlanAndStepNumber(savedPlan, 1))
                .isPresent()
                .get()
                .extracting(OnboardingStep::getTitle)
                .isEqualTo("Step One");
    }

    @Test
    void findByPlanAndStepNumber_returnsEmpty_whenStepNotFound() {
        assertThat(stepRepository.findByPlanAndStepNumber(savedPlan, 99)).isEmpty();
    }

    @Test
    void updateCompleted_persistsChange() {
        OnboardingStep step = stepRepository.save(step(savedPlan, 1, "Setup"));

        step.setCompleted(true);
        stepRepository.save(step);
        stepRepository.flush();

        OnboardingStep reloaded = stepRepository.findById(step.getId()).orElseThrow();
        assertThat(reloaded.isCompleted()).isTrue();
    }

    private OnboardingStep step(OnboardingPlan plan, int number, String title) {
        return OnboardingStep.builder()
                .plan(plan)
                .stepNumber(number)
                .title(title)
                .description("Description for " + title)
                .completed(false)
                .blockers(new ArrayList<>())
                .build();
    }
}
