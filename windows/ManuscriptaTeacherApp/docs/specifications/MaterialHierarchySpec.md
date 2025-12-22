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



## Section 2 - Entity classes for each Hierarchical Level

### Section 2A - Unit Collection

(1) A unit collection is represented by a `UnitCollectionEntity` class. This class must contain the following attributes.

    (a) `Title` (string): The length of this string is limited to a maximum of 500 characters.


### Section 2B - Unit

(1) A unit is represented by a `UnitEntity` class. This class must contain the following attributes.

    (a) `UnitCollectionId` (UUID): References the unit collection to which this unit belongs.

    (b) `Title` (string): The length of this string is limited to a maximum of 500 characters.


### Section 2C - Lesson

(1) A lesson is represented by a `Lesson` class. This class must contain the following attributes.

    (a) `UnitId` (UUID): References the unit to which this lesson belongs.

    (b) `Title` (string): The length of this string is limited to a maximum of 500 characters.


### Section 2D - Material

(1) A material is represented by a `Material` class. This class must contain the following attributes. Also see `Validation Rules.md` for additional requirements regarding this class.

    (a) `LessonId` (UUID): References the unit to which this material belongs.