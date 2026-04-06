# Sprint P6: Deployment Plan

## 1. Frontend Deployment
### Objective
Deploy the P3 frontend to AWS Amplify with environment-aware configuration, automated build and deploy through GitHub Actions, and end-to-end connectivity to the deployed API Gateway backend.

### Key Tasks
- Create an AWS Amplify app connected to the GitHub repository and configure branch-based environments for dev, staging, and main.
- Configure Amplify build settings in amplify.yml for install, test, and production build steps.
- Add environment variables in Amplify for API base URL, auth configuration, and runtime flags per environment.
- Implement custom domain mapping and HTTPS verification for staging and production.
- Add a GitHub Actions workflow to run frontend lint and unit tests on pull requests and trigger Amplify deployment only after successful checks.
- Define branch protection rules with required checks, pull request reviews, and blocked direct pushes to main.
- Execute integration smoke tests from GitHub Actions after deployment by validating key frontend user flows against live backend endpoints.

### Deliverables
- Amplify app configured with at least staging and production branches.
- Successful GitHub Actions pipeline for frontend quality gates and deployment trigger.
- Frontend URLs documented with environment mapping and API Gateway endpoint configuration.
- Integration smoke test report attached to CI run artifacts.

### Risks & Mitigations
- Risk: Environment variable mismatch causes frontend-backend connection failures. Mitigation: Use a per-branch environment variable mapping table and validate required variables in CI before deployment.
- Risk: Broken deployment due to dependency or build drift. Mitigation: Pin Node version and lockfile; enforce build-step parity between local and Amplify.
- Risk: Unreviewed changes reach production. Mitigation: Enforce branch protection with required checks and at least one reviewer approval.

## 2. Backend Deployment
### Objective
Deploy the P4 backend as AWS Lambda functions exposed through API Gateway REST API, with automated packaging and deployment via GitHub Actions and post-deploy integration verification.

### Key Tasks
- Package backend components for AWS Lambda runtime compatibility, including handler entrypoint, dependencies, and configuration.
- Provision API Gateway REST API routes mapped to Lambda handlers, including CORS, request and response mapping, and stage configuration for dev and prod.
- Manage infrastructure as code using CloudFormation, SAM, or Terraform for Lambda, API Gateway, IAM roles, and logging.
- Configure CloudWatch logs and metrics plus baseline alarms for 5xx error rate and latency.
- Add a GitHub Actions backend workflow for build, unit test, artifact packaging, and deployment to Lambda and API Gateway using AWS credentials from GitHub Secrets or OIDC.
- Add deployment gates so only merges to the protected branch trigger production deployment, while pull requests deploy to non-production.
- Implement integration testing strategy with API-level tests after deployment for health check, auth flow, critical CRUD path, and error path; fail pipeline on regressions.

### Deliverables
- Deployed Lambda-backed API Gateway REST API with documented endpoints.
- Infrastructure as code definitions committed and reproducible.
- GitHub Actions backend CI/CD workflow with staged deployment behavior.
- Integration test suite integrated into CI with pass and fail reporting.

### Risks & Mitigations
- Risk: IAM misconfiguration blocks API Gateway to Lambda invocation. Mitigation: Use least-privilege templates reviewed in pull requests and run automated post-deploy permission checks.
- Risk: Cold starts or timeout issues degrade user experience. Mitigation: Tune memory and timeout, optimize dependency size, and monitor p95 latency in CloudWatch.
- Risk: Deployment secrets exposure. Mitigation: Use GitHub OIDC or encrypted secrets, rotate credentials, and restrict environment-level access.

## 3. LLM Integration and Deployment
### Objective
Use LLM-assisted workflows to accelerate deployment automation and integration testing while enforcing strict verification to avoid hallucinated changes, ensuring reliable prompt-driven operations for a student team.

### Key Tasks
- Use an LLM to generate first-draft deployment scripts and workflows for AWS Lambda, API Gateway, and Amplify, including GitHub Actions YAML, infrastructure templates, and runbooks.
- Use an LLM to draft integration tests covering frontend and backend paths, including login and session, board and task CRUD, failure cases, and API contract checks.
- Apply a multi-agent verification approach where Agent A generates scripts and tests, Agent B reviews service names, IAM scopes, API routes, and workflow triggers against AWS and GitHub documentation, and Agent C executes CI checks and integration tests to reject outputs that fail execution.
- Enforce evidence-based validation for every LLM output through syntax linting, dry-run or plan output, test execution logs, and pull request checklist sign-off.
- Enforce team policy: no direct code edits — only prompt-based modifications, with all generated outputs committed through reviewed pull requests.
- Add sprint governance in GitHub using a weekly planning issue, a deployment checklist template, and required pull request template fields for prompt used, verification evidence, and rollback notes.

### Deliverables
- Prompt library for deployment scripts, CI workflows, and integration test generation.
- Verified GitHub Actions workflows and integration tests with execution evidence.
- LLM verification checklist and multi-agent review protocol documented for repeat use.
- Sprint timeline (4 weeks): Week 1 covers Amplify setup, Lambda packaging baseline, and branch protection with workflow scaffolding; Week 2 covers API Gateway REST integration, staged deployments, and initial integration tests; Week 3 covers full CI/CD automation, LLM-generated script and test refinement, and verification hardening; Week 4 covers end-to-end validation, rollback drill, documentation freeze, and sprint demo.

### Risks & Mitigations
- Risk: LLM hallucinations produce invalid AWS commands or insecure configuration. Mitigation: Require multi-agent review plus executable validation before merge and reject unverified output.
- Risk: Over-reliance on prompts without system understanding. Mitigation: Assign a student owner review for each generated artifact and require runbook explanation.
- Risk: Prompt drift causes inconsistent outputs. Mitigation: Version prompts in the repository and require prompt ID and version in each pull request.