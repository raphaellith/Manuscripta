# Material Hierarchy (Windows)

## Explanatory Note

This document defines the hierarchical system for grouping and organising Material items in the model layer.


## Section 1 - General Principles

(1) The material hierarchy consists of four distinct levels, as listed below in order from top to bottom.

    (a) Unit collections
    (b) Units
    (c) Lessons
    (d) Materials

(2) Each level must correspond to a distinct entity class defined in the Model layer.

(3) Excluding the top level, any entity belonging to a given level must contain the UUID of an entity belonging to the level immediately above. It must not contain UUIDs of entities belonging to any other level.

(4) Zero or more unit collections may exist during runtime.