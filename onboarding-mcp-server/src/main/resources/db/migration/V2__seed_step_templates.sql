-- Backend Engineer onboarding steps (10 steps)
INSERT INTO step_templates (role, step_number, title, description) VALUES
('BACKEND_ENGINEER', 1,  'Development Environment Setup',    'Install JDK 26, Maven 3.9+, IntelliJ IDEA, Docker Desktop, and Git. Verify each tool is on PATH and accessible from the terminal.'),
('BACKEND_ENGINEER', 2,  'Repository Access & Clone',        'Request access to the GitHub/GitLab organisation. Clone the core repositories and verify you can build each one locally with ./mvnw package.'),
('BACKEND_ENGINEER', 3,  'Local Stack Startup',              'Start the full local development stack using Docker Compose (PostgreSQL, Redis, message broker). Verify all services are healthy and reachable.'),
('BACKEND_ENGINEER', 4,  'Architecture Deep Dive',           'Read the system architecture document, ADRs, and service boundary diagrams. Schedule a 1-hour walkthrough with a senior engineer on the team.'),
('BACKEND_ENGINEER', 5,  'First Code Review Shadow',         'Attend a live code review session with a senior developer. Leave at least one meaningful comment or question to demonstrate engagement.'),
('BACKEND_ENGINEER', 6,  'Write Your First Unit Test',       'Pick an existing service or utility class that has low test coverage and add at least three meaningful unit tests following the project conventions.'),
('BACKEND_ENGINEER', 7,  'Database Schema & Data Model',     'Review all PostgreSQL schemas, understand entity relationships, and trace how a key domain object flows from API request to database row.'),
('BACKEND_ENGINEER', 8,  'CI/CD Pipeline Walkthrough',       'Walk through the Jenkins/GitHub Actions pipeline configuration. Trigger a build manually and trace the steps from code push to deployment.'),
('BACKEND_ENGINEER', 9,  'First Feature Pull Request',       'Pick a small ticket (bug fix or minor feature), implement it following team conventions, open a PR, address review feedback, and get it merged.'),
('BACKEND_ENGINEER', 10, 'Security & Compliance Training',   'Complete all mandatory security awareness modules, review OWASP Top 10 as it applies to backend services, and sign the team''s security checklist.');

-- Frontend Engineer onboarding steps (10 steps)
INSERT INTO step_templates (role, step_number, title, description) VALUES
('FRONTEND_ENGINEER', 1,  'Development Environment Setup',    'Install Node.js 20 LTS, npm/pnpm, VS Code with recommended extensions, and Chrome with DevTools. Verify the local dev server starts without errors.'),
('FRONTEND_ENGINEER', 2,  'Repository Access & Local Run',    'Request access to the frontend repositories. Clone the repo, run npm install, start the dev server, and confirm the application loads in the browser.'),
('FRONTEND_ENGINEER', 3,  'Design System Orientation',        'Review the Figma design files, explore the component library (Storybook or equivalent), and understand the design tokens and colour palette in use.'),
('FRONTEND_ENGINEER', 4,  'UI Framework Deep Dive',           'Study the chosen framework''s architecture decisions (React/Vue/Angular) and the team''s specific conventions around folder structure, state management, and routing.'),
('FRONTEND_ENGINEER', 5,  'Build Your First Component',       'Build a small, self-contained UI component (e.g. a status badge or info card) following existing patterns, with unit tests and a Storybook story.'),
('FRONTEND_ENGINEER', 6,  'CSS & Styling Standards',          'Review the project''s CSS methodology (BEM, CSS Modules, Tailwind, etc.), understand responsive breakpoints, and fix a minor styling inconsistency.'),
('FRONTEND_ENGINEER', 7,  'API Integration Review',           'Trace how frontend components fetch data from backend APIs: review the API client setup, error handling strategy, and loading/error state patterns.'),
('FRONTEND_ENGINEER', 8,  'Cross-Browser & Accessibility',    'Run the application in Chrome, Firefox, and Safari. Use Lighthouse and axe-core to identify and document at least one accessibility or performance issue.'),
('FRONTEND_ENGINEER', 9,  'First Feature Pull Request',       'Pick a small UI ticket, implement it following team conventions, open a PR with screenshots, address review feedback, and get it merged.'),
('FRONTEND_ENGINEER', 10, 'Performance & Security Baseline',  'Complete the frontend security checklist (XSS, CSRF, CSP headers) and ensure the main route scores above 90 on Lighthouse Performance.');

-- Product Manager onboarding steps (10 steps)
INSERT INTO step_templates (role, step_number, title, description) VALUES
('PRODUCT_MANAGER', 1,  'Team & Stakeholder Introductions',  'Schedule 30-minute coffee chats with engineering leads, design leads, marketing, and key business stakeholders. Build your internal network map.'),
('PRODUCT_MANAGER', 2,  'Product Vision & Strategy Review',  'Review the product roadmap, company OKRs, and the strategy documents for your area. Schedule a session with the CPO or product director to align on priorities.'),
('PRODUCT_MANAGER', 3,  'Customer Research Deep Dive',       'Read existing user research reports, NPS data, and support ticket trends. Identify the top three unresolved pain points for your product area.'),
('PRODUCT_MANAGER', 4,  'Competitor & Market Analysis',      'Complete a competitive landscape analysis for your product area: identify three direct competitors, document their strengths and gaps versus your product.'),
('PRODUCT_MANAGER', 5,  'Backlog Review & Prioritisation',   'Review the existing product backlog. Understand the prioritisation framework in use (RICE, MoSCoW, ICE), and ensure the top 20 items are clearly scored.'),
('PRODUCT_MANAGER', 6,  'Agile Process Immersion',           'Attend a full sprint cycle: sprint planning, daily standups, sprint review, and retrospective. Observe and then facilitate at least one meeting yourself.'),
('PRODUCT_MANAGER', 7,  'Metrics & Analytics Setup',         'Gain access to product analytics (Amplitude, Mixpanel, GA4, etc.). Understand how the top three KPIs are tracked and set a personal dashboard.'),
('PRODUCT_MANAGER', 8,  'Write Your First User Story',       'Draft a complete user story with acceptance criteria for a medium-complexity feature. Get it reviewed by a senior engineer and a designer, then refine it.'),
('PRODUCT_MANAGER', 9,  'Design Collaboration Session',      'Participate in a full design critique: review wireframes or hi-fi designs, provide structured feedback aligned to user needs and business goals.'),
('PRODUCT_MANAGER', 10, 'First Stakeholder Update',          'Draft and share a written product update covering progress, risks, and next steps. Present it to at least one senior stakeholder and collect feedback.');
