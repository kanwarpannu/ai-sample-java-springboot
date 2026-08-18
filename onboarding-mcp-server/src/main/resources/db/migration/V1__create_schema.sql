CREATE TABLE step_templates (
    id          BIGSERIAL PRIMARY KEY,
    role        VARCHAR(100) NOT NULL,
    step_number INT NOT NULL,
    title       VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT uq_step_templates_role_step UNIQUE (role, step_number)
);

CREATE TABLE onboarding_plans (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    developer_name VARCHAR(500) NOT NULL,
    role           VARCHAR(100) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE onboarding_steps (
    id           BIGSERIAL PRIMARY KEY,
    plan_id      UUID NOT NULL REFERENCES onboarding_plans(id) ON DELETE CASCADE,
    step_number  INT NOT NULL,
    title        VARCHAR(500) NOT NULL,
    description  TEXT NOT NULL,
    completed    BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    CONSTRAINT uq_onboarding_steps_plan_step UNIQUE (plan_id, step_number)
);

CREATE TABLE blockers (
    id          BIGSERIAL PRIMARY KEY,
    step_id     BIGINT NOT NULL REFERENCES onboarding_steps(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    resolved    BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
