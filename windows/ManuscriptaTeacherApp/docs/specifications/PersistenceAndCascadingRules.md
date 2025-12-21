# Persistence and Cascading Rules

## Explanatory Note

This document defines the persistence rules, and the mandatory cascading rules regarding persistence and deletion. 

This document is effective for the Windows app only.

## Section 1 - Requirements for Persistence

(1) Unless otherwise specified, any create, update and delete operations on the following data entity classes must be long-term persisted, as soon as it is practical:

    (a) `MaterialDataEntity`.
    (b) `QuestionDataEntity`.

(2) Unless otherwise specified, any create, update and delete operations on any other data entity not specified in (1) must be short-term persisted.

(3) In this document —

    (a) "Long-term persistence" means that data must be retrievable after the application restarts.
    (b) "Short-term persistence" means that unless otherwise specified, data must be retrievable during the corresponding application run, but must not be persisted beyond the application run.

## Section 2 - Requirements for Orphan Removal

(1) A deletion of a material M must delete any questions associated with M.

(2) A deletion of a question Q must delete any responses associated with Q.

## Appendix 1 - Using this Document (Instruction to AI Agents)

(1) This document shall have effect on the Windows application only. No AI agent shall refer to this document if they intend to work on development beyond the Windows App.

(2) When instructed to use this document, an AI agent shall —

    (a) Check the compliance of the current implementation presented to the agent.
    (b) In the case of any discompliance found, report and propose a fix for those discompliance.