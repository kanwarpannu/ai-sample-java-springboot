package com.example.onboarding.mcp.tool;

import com.example.onboarding.mcp.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingMcpToolsTest {

    @Mock
    private OnboardingService onboardingService;

    @InjectMocks
    private OnboardingMcpTools onboardingMcpTools;

    @Test
    void createOnboardingPlan_delegatesToServiceAndReturnsResult() {
        when(onboardingService.createOnboardingPlan("Alice", "BACKEND_ENGINEER"))
                .thenReturn("Plan created. ID: abc-123. 10 steps assigned.");

        String result = onboardingMcpTools.createOnboardingPlan("Alice", "BACKEND_ENGINEER");

        assertThat(result).isEqualTo("Plan created. ID: abc-123. 10 steps assigned.");
        verify(onboardingService).createOnboardingPlan("Alice", "BACKEND_ENGINEER");
    }

    @Test
    void getOnboardingProgress_delegatesToServiceAndReturnsResult() {
        String planId = "550e8400-e29b-41d4-a716-446655440000";
        when(onboardingService.getOnboardingProgress(planId))
                .thenReturn("Progress for Alice (BACKEND_ENGINEER): 5/10 steps completed.");

        String result = onboardingMcpTools.getOnboardingProgress(planId);

        assertThat(result).contains("5/10 steps completed");
        verify(onboardingService).getOnboardingProgress(planId);
    }

    @Test
    void updateOnboardingStep_delegatesToServiceAndReturnsResult() {
        String planId = "550e8400-e29b-41d4-a716-446655440000";
        when(onboardingService.updateOnboardingStep(planId, 3, true))
                .thenReturn("Step 3 'First PR' marked as COMPLETED for Alice.");

        String result = onboardingMcpTools.updateOnboardingStep(planId, 3, true);

        assertThat(result).contains("COMPLETED").contains("First PR");
        verify(onboardingService).updateOnboardingStep(planId, 3, true);
    }

    @Test
    void reportBlocker_delegatesToServiceAndReturnsResult() {
        String planId = "550e8400-e29b-41d4-a716-446655440000";
        when(onboardingService.reportBlocker(planId, 2, "No VPN access"))
                .thenReturn("Blocker reported for step 2. Blocker ID: 42.");

        String result = onboardingMcpTools.reportBlocker(planId, 2, "No VPN access");

        assertThat(result).contains("42");
        verify(onboardingService).reportBlocker(planId, 2, "No VPN access");
    }

    @Test
    void resolveBlocker_delegatesToServiceAndReturnsResult() {
        when(onboardingService.resolveBlocker(42L))
                .thenReturn("Blocker 42 resolved: 'No VPN access'");

        String result = onboardingMcpTools.resolveBlocker(42L);

        assertThat(result).contains("42").contains("resolved");
        verify(onboardingService).resolveBlocker(42L);
    }
}
