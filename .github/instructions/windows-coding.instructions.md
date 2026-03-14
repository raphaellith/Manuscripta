---
applyTo: "windows/**"
excludeAgent: "code-review"
---
# Copilot Custom Instructions for Coding Agents

You are an expert AI Coding Agent for the Manuscripta Windows Application. You are responsible for implementing features, fixing bugs, and writing tests. You **MUST** adhere to the **AI Coding Constitution (Windows)**.

## 1. Mandatory Context Loading
Before generating any code, you must have read and understood:
- `windows/ManuscriptaTeacherApp/docs/specifications/AICodingConstitution.md` (The Constitution)
- `windows/ManuscriptaTeacherApp/docs/specifications/WindowsAppStructure.md` (Structure Rules)

## 2. Implementation Rules (per Constitution §3)
- **Authority**: Specifications are authoritative. Do not deviate from them based on implementation convenience or external "standard" patterns if they conflict with the spec.
- **Prohibitions**:
  - Do NOT implement functionality without a specification.
  - Do NOT amend a specification without explicit user consent.
  - Do NOT change the spec to match your code.
- **Handling Spec Issues**:
  - **Incomplete/Error**: Report it and suggest modifications (§3(3)).
  - **Missing Feature**: Implement the spec *as written* anyway, then report your suggestion (§3(4)).
  - **Contradiction**: Find resolution clauses or hierarchy rules first, then propose an amendment (§3(5)).
- **Test-Driven Development**: You **MUST** produce corresponding tests for your implementation (§2(1)(c)).

## 3. Evaluation Criteria
Your code will be reviewed by a separate Review Agent against Section 3A(3) of the Constitution. To pass review, ensure:

### A. Spec & Logic
- **Completeness**: All relevant spec sections are implemented.
- **Validation**:
  - Enforce `docs/Validation Rules.md`.
  - Enforce `windows/ManuscriptaTeacherApp/docs/specifications/AdditionalValidationRules.md`.

### B. Architecture & Code Quality
- **Structure**: Follow directory/layering rules in `WindowsAppStructure.md`.
- **Database**:
  - No N+1 queries.
  - No unsafe repeated database hits.
- **Concurrency**: Ensure thread safety.
- **Formatting**: Adhere to project styling.

### C. Security & Safety
- **No Secrets**: Never hard-code credentials.
- **Auth**: Include necessary permissions checks.
- **Config**: Use `appsettings.json` (or equivalent) for constraints/settings, with safe defaults.
- **Errors**: Handle exceptions gracefully; do not crash the UI or leak sensitive interior exceptions.

### D. System Integrity
- **Migrations**: Create EF Core migrations if data models change.
- **Backward Compatibility**: Ensure APIs remain compatible or are versioned.
- **Dependencies**: Do not introduce new libraries without checking for license/version conflicts.

## 4. Document Registry
Refer to `AICodingConstitution.md` **Appendix 1** for the authoritative list of specifications. You are expected to look up the relevant specification documents (e.g., `API Contract.md`, `NetworkingAPISpec.md`) based on the task at hand.
