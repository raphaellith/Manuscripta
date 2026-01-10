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

(2) Nothing in this specification shall mandate a developer to use a category defined in (1).

## Section 3: Requirement to Follow Specification For Implementation

(1) In this project, specifications shall be treated as authoritative against any implementation presented. The implementation of a corresponding application (such as the Android app) shall never be treated as a specification and influence the design of the Windows app accordingly.

(2) An AI agent shall not —
    (a) Implement a functionality, however sound it may deem it to be, without specification. 
    (b) Amend a specification without human consent.

(3) If an AI agent deems that implementing a specification is impractical as it is incomplete, or that it contains technical errors, it shall report this view to a human and suggest modification.

(4) If an AI agent deems that a specification lacks a feature that is likely to be helpful, and it is insufficient to deem the specification as incomplete under (3), it shall implement the specification as how it is written, and report its suggestion.

(5) If an AI agent deems two pieces of specifications contradictory, it shall first seek for resolution clauses in either specification. If none of such clauses exist, it shall seek clarification on hierarchy. It shall then propose an amendment accordingly.

### Interpretation

(6) In this section —

    (a) "Incomplete specification" means a specification lacking clauses or configuration data that would make implementation impractical without making substantial assumptions. This shall include, but is not limited to, the omission of ports, opcodes, encoding mechanisms, data formats, or protocol sequences.
    (b) "Technical error" means an error which would result in any implementation following the specification predictably failing to work as intended.

## Section 4: Requirement to Read Mandatory Documents

(1) When conducting any task, an AI agent shall read the following documents, as defined in Appendix 1:

    (a) This document
    (b) Project Specification

(2) In addition to these documents, an AI agent shall comply with any requests to read a document from a developer.

(3) Developers shall update Appendix 1 of this document to reflect the location of the documents, and the authoritative branch they are maintained on. 

(4) The AI agent shall always retrieve the documents in the specified branch in Appendix 1.

## Appendix 1: Registry of Documents

Project-wide specifications, located in `main` branch, `/docs`:

- `Project Specification.md`, provides a high level overview of the project.
- `GitHub Conventions.md`, defines branching conventions.
- `API Contract.md`, defines communication between devices. Subject to `Validation Rules.md` per §1(5) thereof.
- `Pairing Process.md`, defines the pairing process between devices.
- `Validation Rules.md`, defines the validation rules of data entities. Takes precedence over `API Contract.md` per §1(5).
- `Session Interaction.md`, defines session lifecycle, heartbeat mechanism, material distribution, and session state transitions.

Local specifications, located in `windows/ManuscriptaTeachingApp/docs/specifications`, `windows-app` branch:

- `AICodingConstitution.md`, this document.
- `WindowsAppStructure.md`, defines the directory structure and hierarchy of layers for the Windows app.
- `PersistenceAndCascadingRules.md`, the rules for persisting models and cascading deletes.
- `AdditionalValidationRules.md`, defines the material hierarchy and additional Windows-specific validation rules per `Validation Rules.md` §1(7).
- `NetworkingAPISpec.md`, the networking specification that describes the bidirectional communication methods between its Electron front end and its ASP.NET Core back end.
- `FrontendWorkflowSpecifications.md`, the specification that describes the frontend workflow, including how server methods and client handlers are expected to interact.
- `GenAiSpec.md`, the specifications for classes enabling the `Main` component to access generative AI (GenAI) functionalities provided by IBM Granite and Ollama.
