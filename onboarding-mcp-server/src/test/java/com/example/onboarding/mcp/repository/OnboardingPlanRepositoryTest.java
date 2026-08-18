package com.example.onboarding.mcp.repository;

import com.example.onboarding.mcp.domain.OnboardingPlan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OnboardingPlanRepositoryTest {

    @Autowired
    private OnboardingPlanRepository planRepository;

    @Test
    void save_persistsPlanAndGeneratesUuid() {
        OnboardingPlan plan = OnboardingPlan.builder()
                .developerName("Alice Smith")
                .role("BACKEND_ENGINEER")
                .steps(new ArrayList<>())
                .build();

        OnboardingPlan saved = planRepository.save(plan);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDeveloperName()).isEqualTo("Alice Smith");
        assertThat(saved.getRole()).isEqualTo("BACKEND_ENGINEER");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_returnsSavedPlan() {
        OnboardingPlan plan = planRepository.save(OnboardingPlan.builder()
                .developerName("Bob Jones")
                .role("FRONTEND_ENGINEER")
                .steps(new ArrayList<>())
                .build());

        assertThat(planRepository.findById(plan.getId())).isPresent()
                .get()
                .extracting(OnboardingPlan::getDeveloperName)
                .isEqualTo("Bob Jones");
    }

    @Test
    void findById_returnsEmpty_whenPlanDoesNotExist() {
        assertThat(planRepository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findAll_returnsAllSavedPlans() {
        planRepository.save(OnboardingPlan.builder().developerName("Alice").role("BACKEND_ENGINEER").steps(new ArrayList<>()).build());
        planRepository.save(OnboardingPlan.builder().developerName("Bob").role("FRONTEND_ENGINEER").steps(new ArrayList<>()).build());
        planRepository.save(OnboardingPlan.builder().developerName("Carol").role("PRODUCT_MANAGER").steps(new ArrayList<>()).build());

        List<OnboardingPlan> all = planRepository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    void deleteById_removesFromDatabase() {
        OnboardingPlan plan = planRepository.save(OnboardingPlan.builder()
                .developerName("Dave")
                .role("BACKEND_ENGINEER")
                .steps(new ArrayList<>())
                .build());

        planRepository.deleteById(plan.getId());

        assertThat(planRepository.findById(plan.getId())).isEmpty();
    }
}
