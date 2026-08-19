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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingPlanRepository planRepository;
    private final OnboardingStepRepository stepRepository;
    private final BlockerRepository blockerRepository;
    private final StepTemplateRepository stepTemplateRepository;

    @Transactional
    public String createOnboardingPlan(String developerName, String role) {
        log.info("[SERVICE] createOnboardingPlan | developerName='{}', role='{}'", developerName, role);
        String normalizedRole = role.toUpperCase().replace(" ", "_");

        List<StepTemplate> templates = stepTemplateRepository.findByRoleOrderByStepNumber(normalizedRole);
        log.info("[REPOSITORY] stepTemplateRepository.findByRoleOrderByStepNumber | role='{}' → {} template(s)", normalizedRole, templates.size());
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
        log.info("[REPOSITORY] planRepository.save | plan created, id='{}'", plan.getId());

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
        log.info("[REPOSITORY] stepRepository.save | {} step(s) saved for planId='{}'", templates.size(), plan.getId());
        log.info("[SERVICE] createOnboardingPlan | completed for '{}', planId='{}'", developerName, plan.getId());

        return String.format(
                "Onboarding plan created for %s (%s). Plan ID: %s. %d steps assigned. " +
                "Use this Plan ID to track progress, update steps, and report blockers.",
                developerName, normalizedRole, plan.getId(), templates.size());
    }

    @Transactional(readOnly = true)
    public String getOnboardingProgress(String planId) {
        log.info("[SERVICE] getOnboardingProgress | planId='{}'", planId);
        UUID uuid = parseUuid(planId);
        if (uuid == null) {
            return "Error: Invalid plan ID format. Expected a UUID.";
        }

        OnboardingPlan plan = planRepository.findById(uuid).orElse(null);
        log.info("[REPOSITORY] planRepository.findById | planId='{}' → found={}", planId, plan != null);
        if (plan == null) {
            return "Error: No onboarding plan found with ID " + planId;
        }

        List<OnboardingStep> steps = stepRepository.findByPlanOrderByStepNumber(plan);
        log.info("[REPOSITORY] stepRepository.findByPlanOrderByStepNumber | {} step(s) found for planId='{}'", steps.size(), planId);
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

        log.info("[SERVICE] getOnboardingProgress | completed={}/{} for planId='{}'", completed, total, planId);
        return sb.toString().trim();
    }

    @Transactional
    public String updateOnboardingStep(String planId, int stepNumber, boolean completed) {
        log.info("[SERVICE] updateOnboardingStep | planId='{}', stepNumber={}, completed={}", planId, stepNumber, completed);
        UUID uuid = parseUuid(planId);
        if (uuid == null) {
            return "Error: Invalid plan ID format. Expected a UUID.";
        }

        OnboardingPlan plan = planRepository.findById(uuid).orElse(null);
        log.info("[REPOSITORY] planRepository.findById | planId='{}' → found={}", planId, plan != null);
        if (plan == null) {
            return "Error: No onboarding plan found with ID " + planId;
        }

        OnboardingStep step = stepRepository.findByPlanAndStepNumber(plan, stepNumber).orElse(null);
        log.info("[REPOSITORY] stepRepository.findByPlanAndStepNumber | stepNumber={} → found={}", stepNumber, step != null);
        if (step == null) {
            return "Error: Step " + stepNumber + " not found in plan " + planId;
        }

        step.setCompleted(completed);
        step.setCompletedAt(completed ? LocalDateTime.now() : null);
        stepRepository.save(step);

        String status = completed ? "COMPLETED" : "REOPENED";
        log.info("[REPOSITORY] stepRepository.save | step {} marked {}", stepNumber, status);
        log.info("[SERVICE] updateOnboardingStep | step {} → {} for planId='{}'", stepNumber, status, planId);
        return String.format("Step %d '%s' marked as %s for %s.",
                stepNumber, step.getTitle(), status, plan.getDeveloperName());
    }

    @Transactional
    public String reportBlocker(String planId, int stepNumber, String description) {
        log.info("[SERVICE] reportBlocker | planId='{}', stepNumber={}", planId, stepNumber);
        UUID uuid = parseUuid(planId);
        if (uuid == null) {
            return "Error: Invalid plan ID format. Expected a UUID.";
        }

        OnboardingPlan plan = planRepository.findById(uuid).orElse(null);
        log.info("[REPOSITORY] planRepository.findById | planId='{}' → found={}", planId, plan != null);
        if (plan == null) {
            return "Error: No onboarding plan found with ID " + planId;
        }

        OnboardingStep step = stepRepository.findByPlanAndStepNumber(plan, stepNumber).orElse(null);
        log.info("[REPOSITORY] stepRepository.findByPlanAndStepNumber | stepNumber={} → found={}", stepNumber, step != null);
        if (step == null) {
            return "Error: Step " + stepNumber + " not found in plan " + planId;
        }

        Blocker blocker = Blocker.builder()
                .step(step)
                .description(description)
                .resolved(false)
                .build();
        blocker = blockerRepository.save(blocker);
        log.info("[REPOSITORY] blockerRepository.save | blockerId={} created for stepNumber={}", blocker.getId(), stepNumber);
        log.info("[SERVICE] reportBlocker | blockerId={} created, stepNumber={}, planId='{}'", blocker.getId(), stepNumber, planId);

        return String.format(
                "Blocker reported for step %d '%s' (plan: %s). Blocker ID: %d. Description: '%s'. " +
                "Use this Blocker ID to resolve it later.",
                stepNumber, step.getTitle(), plan.getDeveloperName(), blocker.getId(), description);
    }

    @Transactional
    public String resolveBlocker(long blockerId) {
        log.info("[SERVICE] resolveBlocker | blockerId={}", blockerId);
        Blocker blocker = blockerRepository.findById(blockerId).orElse(null);
        log.info("[REPOSITORY] blockerRepository.findById | blockerId={} → found={}", blockerId, blocker != null);
        if (blocker == null) {
            return "Error: No blocker found with ID " + blockerId;
        }

        blocker.setResolved(true);
        blocker.setResolvedAt(LocalDateTime.now());
        blockerRepository.save(blocker);
        log.info("[REPOSITORY] blockerRepository.save | blockerId={} resolved", blockerId);
        log.info("[SERVICE] resolveBlocker | blockerId={} resolved", blockerId);

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
