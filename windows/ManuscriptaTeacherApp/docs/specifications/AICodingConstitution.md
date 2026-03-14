# AI Coding Constitution (Windows)

## Explanatory Note

This document defines the framework under which AI coding agents shall work in.

AI agents should be directed to this document when working on tasks. It should provide sufficient information for it to look up the correst specifications.

This document is effective for Windows App development only.

## Section 1: General Principles

(1) This document may be cited as "AI Coding Constitution (Windows)".

(2) In all specifications, unless otherwise specified, modal verbs "should", "shall" and "must" shall take their ordinary meanings from RFC2119.

(3) AI agents should be used —

    (a) When implementing a specification that has been merged into the branches specified in Appendix 1.
    (b) When filling in implementation details regarding a specification.
    (c) When reviewing the reasonableness of a specification.
    (d) When verifying the compliance of an implementation against specification(s).

(4) AI agents should not be prompted to conduct the following —

    (a) Implementing a functionality without an approved specification.
    (b) Writing a specification without an explicit design discussion between developers.

(5) In this document, a "developer" means a human working on the project.

(6) This document shall govern all usage of AI development within the Windows application.

## Section 2: Type of Tasks

(1) When delegating a task to an AI agent, a developer may use one or more of the following terminologies, or their corresponding section numbers to indicate the nature of the task:

    (a) Review document. An AI agent shall judge the reasonableness of a document, and make modification suggestions orally. However, it may not modify the document without a developer's consent.
    (b) Edit a document. A developer shall first explicitly describe clause(s) they wish to be edited or extended by the AI agent, and the AI agent shall construct those provisions with implementation details accordingly.
    (c) Implement a specification. A developer shall specify the specification, and the corresponding clauses, that the AI agent is expected to implement. The AI agent shall implement those provisions accordingly. The Windows application uses test-driven development, and the AI agent must also produce corresponding tests, as appropriate, for their implementation.
    (d) Verify compliance. A developer shall specify a specification they wish to check compliance for, and the AI agent shall produce a report of compliance of the current implementation against that specification.
    (e) Code review. A developer shall request a review from Copilot in a pull request.

(2) Nothing in this specification shall mandate a developer to use a category defined in (1).

## Section 3: Requirement to Follow Specification For Implementation

(1) In this project, specifications shall be treated as authoritative against any implementation presented. The implementation of a corresponding application (such as the Android app) shall never be treated as a specification and influence the design of the Windows app accordingly. They shall not be written as, or be treated as, descriptive documentation.

(2) An AI agent shall not —
    (a) Implement a functionality, however sound it may deem it to be, without specification. 
    (b) Amend a specification without human consent.
    (c) Suggest the amendment of a specification to match implementation, unless any of subsections (3)(4) or (5) below apply.

(3) If an AI agent deems that implementing a specification is impractical as it is incomplete, or that it contains technical errors, it shall report this view to a human and suggest modification.

(4) If an AI agent deems that a specification lacks a feature that is likely to be helpful, and it is insufficient to deem the specification as incomplete under subsection (3), it shall implement the specification as how it is written, and report its suggestion.

(5) If an AI agent deems two pieces of specifications contradictory, it shall first seek for resolution clauses in either specification. If none of such clauses exist, it shall seek clarification on hierarchy. It shall then propose an amendment accordingly.

(6) In this section —

    (a) "Incomplete specification" means a specification lacking clauses or configuration data that would make implementation impractical without making substantial assumptions. This shall include, but is not limited to, the omission of ports, opcodes, encoding mechanisms, data formats, or protocol sequences.
    (b) "Technical error" means an error which would result in any implementation following the specification predictably failing to work as intended.

## Section 3A: Responsibility of the Review Agent

(1) In this Section, a review agent refers to any agent which is tasked with reviewing an implementation.

(2) **Recommendations to Developers**

    A developer seeking a review should include —

    (a) the respective changes of specifications in the pull request; or
    (b) in case of a specification being approved before implementation, a reference to those specifications in the form of the pull request during which the specifications are merged, or the corresponding section(s).
    
(3) An AI agent shall conduct a review of the submitted implementation including, but not limited to, the following:

    (a) Unless otherwise excluded, all cited specification sections have been implemented.
    (b) No unreasonable non-specified features are present.
    (c) In the case of a data entity, all validation rules defined in Validation Rules and Additional Validation Rules are enforced.
    (d) Files are organised in the manner specified in Windows App Structure Specification.
    (e) The implementation follows best practices, including:
        (i) Thread safety.
        (ii) Avoidance of repeated iterations or N+1 database queries.
        (iii) Appropriate test coverage.
        (iv) Code Formatting and Documentation.
        (v) Appropriate cross-references to the specification sections.
    (f) The implementation does not introduce security regressions, including:
        (i) No hard-coded secrets or credentials.
        (ii) Where required, appropriate authentication and authorisation checks.
        (iii) Avoidance of unsafe deserialization patterns.
        (iv) Avoidance of sensitive data leakage in logs.
    (g) Configuration changes are documented and wired through appropriate configuration sources, with safe defaults.
    (h) Error handling and HTTP responses (where applicable) are consistent with specifications and provide user-safe failures.
    (i) Backward compatibility concerns are addressed, including data contract changes and migrations where required.
    (j) Performance regressions are avoided, including UI thread blocking and unbounded resource use.
    (k) New dependencies are justified and do not introduce incompatible licensing or version conflicts.

(4) Pursuant to Section 3(2)(c), the review agent shall request a change in specifications only when this is the only sensible option.

(5) If the developers do not provide specification sections, the review agent shall attempt deduce those from the code comments, and if those are unavailable, conduct a general-purpose code review and report this fact.

(6) If a pull request contains specifications only, the review agent shall judge whether it falls under Sections 3(3)(4) or (5), and report any potential improvements.

## Section 4: Requirement to Read Mandatory Documents

(1) When conducting any task, an AI agent shall read the following documents, or a role-specific edition of them, as defined in Appendix 1:

    (a) This document
    (b) Windows App Structure Specification

(2) In addition to these documents, an AI agent shall comply with any requests to read a document from a developer.

(3) Developers should update Appendix 1 of this document to reflect the location of the documents, and the authoritative branch they are maintained on. 

(4) The AI agent shall always retrieve the documents in the specified branch in Appendix 1.

## Appendix 1: Registry of Documents

Project-wide specifications, located in `main` branch, `/docs`:

- `Project Specification.md`, provides a high level overview of the project.
- `GitHub Conventions.md`, defines branching conventions.
- `API Contract.md`, defines communication between devices. Subject to `Validation Rules.md` per §1(5) thereof.
- `Pairing Process.md`, defines the pairing process between devices.
- `Validation Rules.md`, defines the validation rules of data entities. Takes precedence over `API Contract.md` per §1(5).
- `Session Interaction.md`, defines session lifecycle, heartbeat mechanism, material distribution, and session state transitions.
- `Material Encoding.md`, defines the encoding of materials.

Local specifications, located in `windows/ManuscriptaTeachingApp/docs/specifications`, `windows-app` branch:

- `AICodingConstitution.md`, this document.
- `WindowsAppStructure.md`, defines the directory structure and hierarchy of layers for the Windows app.
- `PersistenceAndCascadingRules.md`, the rules for persisting models and cascading deletes.
- `AdditionalValidationRules.md`, defines the material hierarchy and additional Windows-specific validation rules per `Validation Rules.md` §1(7).
- `NetworkingAPISpec.md`, the networking specification that describes the bidirectional communication methods between its Electron front end and its ASP.NET Core back end.
- `FrontendWorkflowSpecifications.md`, the specification that describes the frontend workflow, including how server methods and client handlers are expected to interact.
- `GenAiSpec.md`, the specifications for classes enabling the `Main` component to access generative AI (GenAI) functionalities provided by IBM Granite and Ollama.
- `BuildAndDeploymentSpec.md`, defines the build and deployment of the application.
