# Persistence and Cascading Rules

## Explanatory Note

This document defines the persistence rules, and the mandatory cascading rules regarding persistence and deletion. 

This document is effective for the Windows app only.

## Section 1 - Requirements for Persistence

(1) Unless otherwise specified, any create, update and delete operations on the following data entity classes must be long-term persisted, as soon as it is practical:

    (a) `MaterialDataEntity`.
    (b) `QuestionDataEntity`.
    (c) `UnitCollectionDataEntity`.
    (d) `UnitDataEntity`.
    (e) `LessonDataEntity`.

(2) Unless otherwise specified, any create, update and delete operations on any other data entity not specified in (1) must be short-term persisted.

(3) In this document —

    (a) "Long-term persistence" means that data must be retrievable after the application restarts.
    (b) "Short-term persistence" means that unless otherwise specified, data must be retrievable during the corresponding application run, but must not be persisted beyond the application run.

## Section 2 - Requirements for Orphan Removal

(1) The deletion of a material M must delete any questions associated with M.

(2) The deletion of a question Q must delete any responses associated with Q.

(3) The deletion of a unit collection C must delete any units associated with C.

(4) The deletion of a unit U must delete any lessons associated with U.

(5) The deletion of a lesson L must delete any materials associated with L.


## Appendix 1 - Using this Document (Instruction to AI Agents)

(1) This document shall have effect on the Windows application only. No AI agent shall refer to this document if they intend to work on development beyond the Windows App.

(2) When instructed to use this document, an AI agent shall —

    (a) Check the compliance of the current implementation presented to the agent.
    (b) In the case of any discompliance found, report and propose a fix for those discompliance.