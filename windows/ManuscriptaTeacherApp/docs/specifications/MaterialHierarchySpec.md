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



## Section 2 - Entity classes for each Hierarchical Level

### Section 2A - Unit Collection

(1) A unit collection is represented by a `UnitCollectionEntity` class. This class must contain the following attributes.

    (a) `Title` (string): The length of this string is limited to a maximum of 500 characters.


### Section 2B - Unit

(1) A unit is represented by a `UnitEntity` class. This class must contain the following attributes.

    (a) `UnitCollectionId` (UUID): References the unit collection to which this unit belongs.

    (b) `Title` (string): The length of this string is limited to a maximum of 500 characters.

    (c) `SourceDocuments` (List<System.IO.Path>): A list of file paths pointing to source documents.


### Section 2C - Lesson

(1) A lesson is represented by a `LessonEntity` class. This class must contain the following attributes.

    (a) `UnitId` (UUID): References the unit to which this lesson belongs.

    (b) `Title` (string): The length of this string is limited to a maximum of 500 characters.

    (c) `Description` (string).


### Section 2D - Material

(1) A material is represented by a `MaterialEntity` class. In addition to those specified by Section 2A of `Validation Rules.md`, this class must also contain the following fields.

    (a) `LessonId` (UUID): References the unit to which this material belongs.