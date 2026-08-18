package com.example.onboarding.mcp.repository;

import com.example.onboarding.mcp.domain.Blocker;
import com.example.onboarding.mcp.domain.OnboardingPlan;
import com.example.onboarding.mcp.domain.OnboardingStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BlockerRepositoryTest {

    @Autowired
    private OnboardingPlanRepository planRepository;

    @Autowired
    private OnboardingStepRepository stepRepository;

    @Autowired
    private BlockerRepository blockerRepository;

    private OnboardingStep savedStep;

    @BeforeEach
    void setUp() {
        OnboardingPlan plan = planRepository.save(OnboardingPlan.builder()
                .developerName("Frank Green")
                .role("FRONTEND_ENGINEER")
                .steps(new ArrayList<>())
                .build());

        savedStep = stepRepository.save(OnboardingStep.builder()
                .plan(plan)
                .stepNumber(1)
                .title("Environment Setup")
                .description("Install tools")
                .completed(false)
                .blockers(new ArrayList<>())
                .build());
    }

    @Test
    void save_persistsBlockerWithGeneratedIdAndCreatedAt() {
        Blocker blocker = Blocker.builder()
                .step(savedStep)
                .description("No access to npm registry")
                .resolved(false)
                .build();

        Blocker saved = blockerRepository.save(blocker);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDescription()).isEqualTo("No access to npm registry");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.isResolved()).isFalse();
    }

    @Test
    void findByStep_returnsAllBlockersForStep() {
        blockerRepository.save(blocker(savedStep, "Blocker One"));
        blockerRepository.save(blocker(savedStep, "Blocker Two"));

        List<Blocker> found = blockerRepository.findByStep(savedStep);

        assertThat(found).hasSize(2)
                .extracting(Blocker::getDescription)
                .containsExactlyInAnyOrder("Blocker One", "Blocker Two");
    }

    @Test
    void findByStepAndResolved_filtersCorrectly() {
        blockerRepository.save(blocker(savedStep, "Open blocker"));
        Blocker resolved = Blocker.builder()
                .step(savedStep)
                .description("Resolved blocker")
                .resolved(true)
                .resolvedAt(LocalDateTime.now())
                .build();
        blockerRepository.save(resolved);

        List<Blocker> openBlockers = blockerRepository.findByStepAndResolved(savedStep, false);
        List<Blocker> resolvedBlockers = blockerRepository.findByStepAndResolved(savedStep, true);

        assertThat(openBlockers).hasSize(1).extracting(Blocker::getDescription).containsExactly("Open blocker");
        assertThat(resolvedBlockers).hasSize(1).extracting(Blocker::getDescription).containsExactly("Resolved blocker");
    }

    @Test
    void resolve_updatesResolvedFields() {
        Blocker blocker = blockerRepository.save(blocker(savedStep, "Waiting for license"));

        blocker.setResolved(true);
        blocker.setResolvedAt(LocalDateTime.now());
        blockerRepository.save(blocker);
        blockerRepository.flush();

        Blocker reloaded = blockerRepository.findById(blocker.getId()).orElseThrow();
        assertThat(reloaded.isResolved()).isTrue();
        assertThat(reloaded.getResolvedAt()).isNotNull();
    }

    private Blocker blocker(OnboardingStep step, String description) {
        return Blocker.builder()
                .step(step)
                .description(description)
                .resolved(false)
                .build();
    }
}
