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

(3) Any entity belonging to a given level, except materials, must contain (i.e. have a list of references to) zero or more entities belonging to the level immediately below. It must not contain entities of any other level.

(4) Zero or more unit collections may exist during runtime.