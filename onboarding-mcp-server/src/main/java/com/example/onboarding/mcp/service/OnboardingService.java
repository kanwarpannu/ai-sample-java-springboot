package com.example.onboarding.mcp.service;

import com.example.onboarding.mcp.domain.Blocker;
import com.example.onboarding.mcp.domain.OnboardingPlan;
import com.example.onboarding.mcp.domain.OnboardingStep;
import com.example.onboarding.mcp.domain.StepTemplate;
import com.example.onboarding.mcp.repository.BlockerRepository;
import com.example.onboarding.mcp.repository.OnboardingPlanRepository;
import com.example.onboarding.mcp.repository.OnboardingStepRepository;
import com.example.onboarding.mcp.repository.StepTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingPlanRepository planRepository;
    private final OnboardingStepRepository stepRepository;
    private final BlockerRepository blockerRepository;
    private final StepTemplateRepository stepTemplateRepository;

    @Transactional
    public String createOnboardingPlan(String developerName, String role) {
        String normalizedRole = role.toUpperCase().replace(" ", "_");

        List<StepTemplate> templates = stepTemplateRepository.findByRoleOrderByStepNumber(normalizedRole);
        if (templates.isEmpty()) {
            return "Error: No step templates found for role '" + role
                    + "'. Supported roles: BACKEND_ENGINEER, FRONTEND_ENGINEER, PRODUCT_MANAGER";
        }

        OnboardingPlan plan = OnboardingPlan.builder()
                .developerName(developerName)
                .role(normalizedRole)
                .steps(new ArrayList<>())
                .build();
        plan = planRepository.save(plan);

        for (StepTemplate template : templates) {
            OnboardingStep step = OnboardingStep.builder()
                    .plan(plan)
                    .stepNumber(template.getStepNumber())
                    .title(template.getTitle())
                    .description(template.getDescription())
                    .completed(false)
                    .blockers(new ArrayList<>())
                    .build();
            stepRepository.save(step);
        }

        return String.format(
                "Onboarding plan created for %s (%s). Plan ID: %s. %d steps assigned. " +
                "Use this Plan ID to track progress, update steps, and report blockers.",
                developerName, normalizedRole, plan.getId(), templates.size());
    }

    @Transactional(readOnly = true)
    public String getOnboardingProgress(String planId) {
        UUID uuid = parseUuid(planId);
        if (uuid == null) {
            return "Error: Invalid plan ID format. Expected a UUID.";
        }

        OnboardingPlan plan = planRepository.findById(uuid).orElse(null);
        if (plan == null) {
            return "Error: No onboarding plan found with ID " + planId;
        }

        List<OnboardingStep> steps = stepRepository.findByPlanOrderByStepNumber(plan);
        long completed = steps.stream().filter(OnboardingStep::isCompleted).count();
        long total = steps.size();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Progress for %s (%s): %d/%d steps completed.\n",
                plan.getDeveloperName(), plan.getRole(), completed, total));

        List<OnboardingStep> incompleteSteps = steps.stream().filter(s -> !s.isCompleted()).toList();
        if (!incompleteSteps.isEmpty()) {
            sb.append("Remaining steps:\n");
            incompleteSteps.forEach(s -> sb.append(String.format("  Step %d: %s\n", s.getStepNumber(), s.getTitle())));
        }

        List<Blocker> openBlockers = new ArrayList<>();
        for (OnboardingStep step : steps) {
            openBlockers.addAll(blockerRepository.findByStepAndResolved(step, false));
        }

        if (!openBlockers.isEmpty()) {
            sb.append("Open blockers:\n");
            openBlockers.forEach(b -> sb.append(
                    String.format("  [Blocker ID: %d] Step %d — %s\n",
                            b.getId(), b.getStep().getStepNumber(), b.getDescription())));
        }

        if (completed == total) {
            sb.append("All steps complete! Onboarding finished.");
        }

        return sb.toString().trim();
    }

    @Transactional
    public String updateOnboardingStep(String planId, int stepNumber, boolean completed) {
        UUID uuid = parseUuid(planId);
        if (uuid == null) {
            return "Error: Invalid plan ID format. Expected a UUID.";
        }

        OnboardingPlan plan = planRepository.findById(uuid).orElse(null);
        if (plan == null) {
            return "Error: No onboarding plan found with ID " + planId;
        }

        OnboardingStep step = stepRepository.findByPlanAndStepNumber(plan, stepNumber).orElse(null);
        if (step == null) {
            return "Error: Step " + stepNumber + " not found in plan " + planId;
        }

        step.setCompleted(completed);
        step.setCompletedAt(completed ? LocalDateTime.now() : null);
        stepRepository.save(step);

        String status = completed ? "COMPLETED" : "REOPENED";
        return String.format("Step %d '%s' marked as %s for %s.",
                stepNumber, step.getTitle(), status, plan.getDeveloperName());
    }

    @Transactional
    public String reportBlocker(String planId, int stepNumber, String description) {
        UUID uuid = parseUuid(planId);
        if (uuid == null) {
            return "Error: Invalid plan ID format. Expected a UUID.";
        }

        OnboardingPlan plan = planRepository.findById(uuid).orElse(null);
        if (plan == null) {
            return "Error: No onboarding plan found with ID " + planId;
        }

        OnboardingStep step = stepRepository.findByPlanAndStepNumber(plan, stepNumber).orElse(null);
        if (step == null) {
            return "Error: Step " + stepNumber + " not found in plan " + planId;
        }

        Blocker blocker = Blocker.builder()
                .step(step)
                .description(description)
                .resolved(false)
                .build();
        blocker = blockerRepository.save(blocker);

        return String.format(
                "Blocker reported for step %d '%s' (plan: %s). Blocker ID: %d. Description: '%s'. " +
                "Use this Blocker ID to resolve it later.",
                stepNumber, step.getTitle(), plan.getDeveloperName(), blocker.getId(), description);
    }

    @Transactional
    public String resolveBlocker(long blockerId) {
        Blocker blocker = blockerRepository.findById(blockerId).orElse(null);
        if (blocker == null) {
            return "Error: No blocker found with ID " + blockerId;
        }

        blocker.setResolved(true);
        blocker.setResolvedAt(LocalDateTime.now());
        blockerRepository.save(blocker);

        return String.format("Blocker %d resolved: '%s'", blockerId, blocker.getDescription());
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
