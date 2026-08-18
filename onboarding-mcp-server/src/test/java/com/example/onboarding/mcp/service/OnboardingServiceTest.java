package com.example.onboarding.mcp.service;

import com.example.onboarding.mcp.domain.Blocker;
import com.example.onboarding.mcp.domain.OnboardingPlan;
import com.example.onboarding.mcp.domain.OnboardingStep;
import com.example.onboarding.mcp.domain.StepTemplate;
import com.example.onboarding.mcp.repository.BlockerRepository;
import com.example.onboarding.mcp.repository.OnboardingPlanRepository;
import com.example.onboarding.mcp.repository.OnboardingStepRepository;
import com.example.onboarding.mcp.repository.StepTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock private OnboardingPlanRepository planRepository;
    @Mock private OnboardingStepRepository stepRepository;
    @Mock private BlockerRepository blockerRepository;
    @Mock private StepTemplateRepository stepTemplateRepository;

    @InjectMocks
    private OnboardingService onboardingService;

    // ── createOnboardingPlan ──────────────────────────────────────────────────

    @Test
    void createOnboardingPlan_withValidRole_returnsPlanSummaryWithPlanId() {
        List<StepTemplate> templates = List.of(
                template("BACKEND_ENGINEER", 1, "Setup"),
                template("BACKEND_ENGINEER", 2, "Access")
        );
        OnboardingPlan savedPlan = plan("Alice", "BACKEND_ENGINEER");

        when(stepTemplateRepository.findByRoleOrderByStepNumber("BACKEND_ENGINEER")).thenReturn(templates);
        when(planRepository.save(any())).thenReturn(savedPlan);
        when(stepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = onboardingService.createOnboardingPlan("Alice", "BACKEND_ENGINEER");

        assertThat(result)
                .contains("Alice")
                .contains("BACKEND_ENGINEER")
                .contains("Plan ID:")
                .contains("2 steps");
        verify(stepRepository, times(2)).save(any());
    }

    @Test
    void createOnboardingPlan_normalizesRoleToUpperSnakeCase() {
        when(stepTemplateRepository.findByRoleOrderByStepNumber("BACKEND_ENGINEER")).thenReturn(List.of(
                template("BACKEND_ENGINEER", 1, "Setup")
        ));
        when(planRepository.save(any())).thenReturn(plan("Alice", "BACKEND_ENGINEER"));
        when(stepRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = onboardingService.createOnboardingPlan("Alice", "backend engineer");

        assertThat(result).contains("BACKEND_ENGINEER");
        verify(stepTemplateRepository).findByRoleOrderByStepNumber("BACKEND_ENGINEER");
    }

    @Test
    void createOnboardingPlan_withUnknownRole_returnsError() {
        when(stepTemplateRepository.findByRoleOrderByStepNumber("UNKNOWN")).thenReturn(List.of());

        String result = onboardingService.createOnboardingPlan("Alice", "unknown");

        assertThat(result).startsWith("Error:");
        verify(planRepository, never()).save(any());
    }

    // ── getOnboardingProgress ─────────────────────────────────────────────────

    @Test
    void getOnboardingProgress_withValidPlanId_returnsFormattedProgress() {
        UUID planId = UUID.randomUUID();
        OnboardingPlan mockPlan = plan("Bob", "FRONTEND_ENGINEER");
        List<OnboardingStep> steps = List.of(
                stepWithCompletion(mockPlan, 1, "Step 1", true),
                stepWithCompletion(mockPlan, 2, "Step 2", false)
        );

        when(planRepository.findById(planId)).thenReturn(Optional.of(mockPlan));
        when(stepRepository.findByPlanOrderByStepNumber(mockPlan)).thenReturn(steps);
        when(blockerRepository.findByStepAndResolved(any(), eq(false))).thenReturn(List.of());

        String result = onboardingService.getOnboardingProgress(planId.toString());

        assertThat(result)
                .contains("Bob")
                .contains("FRONTEND_ENGINEER")
                .contains("1/2 steps completed")
                .contains("Step 2");
    }

    @Test
    void getOnboardingProgress_withAllStepsComplete_indicatesFinished() {
        UUID planId = UUID.randomUUID();
        OnboardingPlan mockPlan = plan("Carol", "PRODUCT_MANAGER");
        List<OnboardingStep> steps = List.of(
                stepWithCompletion(mockPlan, 1, "Intro", true)
        );

        when(planRepository.findById(planId)).thenReturn(Optional.of(mockPlan));
        when(stepRepository.findByPlanOrderByStepNumber(mockPlan)).thenReturn(steps);
        when(blockerRepository.findByStepAndResolved(any(), eq(false))).thenReturn(List.of());

        String result = onboardingService.getOnboardingProgress(planId.toString());

        assertThat(result).contains("All steps complete");
    }

    @Test
    void getOnboardingProgress_withInvalidUuid_returnsError() {
        String result = onboardingService.getOnboardingProgress("not-a-uuid");
        assertThat(result).startsWith("Error:").contains("UUID");
    }

    @Test
    void getOnboardingProgress_withUnknownPlanId_returnsError() {
        UUID unknownId = UUID.randomUUID();
        when(planRepository.findById(unknownId)).thenReturn(Optional.empty());

        String result = onboardingService.getOnboardingProgress(unknownId.toString());

        assertThat(result).startsWith("Error:").contains(unknownId.toString());
    }

    // ── updateOnboardingStep ──────────────────────────────────────────────────

    @Test
    void updateOnboardingStep_marksStepCompleted() {
        UUID planId = UUID.randomUUID();
        OnboardingPlan mockPlan = plan("Dave", "BACKEND_ENGINEER");
        OnboardingStep mockStep = stepWithCompletion(mockPlan, 3, "First PR", false);

        when(planRepository.findById(planId)).thenReturn(Optional.of(mockPlan));
        when(stepRepository.findByPlanAndStepNumber(mockPlan, 3)).thenReturn(Optional.of(mockStep));
        when(stepRepository.save(any())).thenReturn(mockStep);

        String result = onboardingService.updateOnboardingStep(planId.toString(), 3, true);

        assertThat(result).contains("COMPLETED").contains("First PR").contains("Dave");
    }

    @Test
    void updateOnboardingStep_reopensStep() {
        UUID planId = UUID.randomUUID();
        OnboardingPlan mockPlan = plan("Dave", "BACKEND_ENGINEER");
        OnboardingStep mockStep = stepWithCompletion(mockPlan, 3, "First PR", true);

        when(planRepository.findById(planId)).thenReturn(Optional.of(mockPlan));
        when(stepRepository.findByPlanAndStepNumber(mockPlan, 3)).thenReturn(Optional.of(mockStep));
        when(stepRepository.save(any())).thenReturn(mockStep);

        String result = onboardingService.updateOnboardingStep(planId.toString(), 3, false);

        assertThat(result).contains("REOPENED");
    }

    @Test
    void updateOnboardingStep_withUnknownPlan_returnsError() {
        UUID planId = UUID.randomUUID();
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        String result = onboardingService.updateOnboardingStep(planId.toString(), 1, true);

        assertThat(result).startsWith("Error:").contains(planId.toString());
    }

    // ── reportBlocker ─────────────────────────────────────────────────────────

    @Test
    void reportBlocker_savesBlockerAndReturnsBlockerId() {
        UUID planId = UUID.randomUUID();
        OnboardingPlan mockPlan = plan("Eve", "FRONTEND_ENGINEER");
        OnboardingStep mockStep = stepWithCompletion(mockPlan, 2, "Clone Repo", false);
        Blocker savedBlocker = Blocker.builder().id(42L).step(mockStep).description("No VPN access").resolved(false).build();

        when(planRepository.findById(planId)).thenReturn(Optional.of(mockPlan));
        when(stepRepository.findByPlanAndStepNumber(mockPlan, 2)).thenReturn(Optional.of(mockStep));
        when(blockerRepository.save(any())).thenReturn(savedBlocker);

        String result = onboardingService.reportBlocker(planId.toString(), 2, "No VPN access");

        assertThat(result).contains("42").contains("Clone Repo").contains("No VPN access");
    }

    // ── resolveBlocker ────────────────────────────────────────────────────────

    @Test
    void resolveBlocker_marksBlockerResolved() {
        Blocker blocker = Blocker.builder().id(5L).description("Network issue").resolved(false).build();
        when(blockerRepository.findById(5L)).thenReturn(Optional.of(blocker));
        when(blockerRepository.save(any())).thenReturn(blocker);

        String result = onboardingService.resolveBlocker(5L);

        assertThat(result).contains("5").contains("Network issue");
        verify(blockerRepository).save(argThat(b -> b.isResolved() && b.getResolvedAt() != null));
    }

    @Test
    void resolveBlocker_withUnknownId_returnsError() {
        when(blockerRepository.findById(99L)).thenReturn(Optional.empty());

        String result = onboardingService.resolveBlocker(99L);

        assertThat(result).startsWith("Error:").contains("99");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private StepTemplate template(String role, int number, String title) {
        return StepTemplate.builder().id((long) number).role(role).stepNumber(number).title(title).description("Desc").build();
    }

    private OnboardingPlan plan(String name, String role) {
        return OnboardingPlan.builder().id(UUID.randomUUID()).developerName(name).role(role).steps(new ArrayList<>()).build();
    }

    private OnboardingStep stepWithCompletion(OnboardingPlan plan, int number, String title, boolean completed) {
        return OnboardingStep.builder()
                .id((long) number)
                .plan(plan)
                .stepNumber(number)
                .title(title)
                .description("Desc")
                .completed(completed)
                .blockers(new ArrayList<>())
                .build();
    }
}
