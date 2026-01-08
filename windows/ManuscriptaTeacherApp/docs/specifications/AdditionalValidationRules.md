# Additional Validation Rules (Windows)

## Explanatory Note

This document defines the hierarchical system for grouping and organising Material items in the model layer. It also defines additional applicability of the validation rules on the Windows client.


## Section 1 - General Principles

(1) The material hierarchy consists of four distinct levels, as listed below in order from top to bottom.

    (a) Unit collections
    (b) Units
    (c) Lessons
    (d) Materials

(2) Each level must correspond to a distinct entity class defined in the Model layer.

(3) Excluding the top level, any entity belonging to a given level must contain the UUID of an entity belonging to the level immediately above. It must not contain UUIDs of entities belonging to any other level.

(4) This document shall be treated as providing additional rules for the validation rules defined in `Validation Rules.md`, per Section 1(7) thereof.

(5) The applicability of this document, as well as Sections 2A to 2C (`MaterialEntity`, `QuestionEntity`, `ResponseEntity`) of `Validation Rules.md`, shall extend to the corresponding Data Entities, enforced implicitly by the polymorphic domain entity classes, and explicitly by the appropriate services.

## Section 2 - Entity classes for Each Hierarchical Level

### Section 2A - Unit Collection

(1) A unit collection is represented by a `UnitCollectionEntity` class. This class must contain the following attributes.

    (a) `Title` (string): The length of this string is limited to a maximum of 500 characters.


### Section 2B - Unit

(1) A unit is represented by a `UnitEntity` class. This class must contain the following attributes.

    (a) `UnitCollectionId` (UUID): References the unit collection to which this unit belongs.

    (b) `Title` (string): The length of this string is limited to a maximum of 500 characters.

    (c) [Deleted.]


### Section 2C - Lesson

(1) A lesson is represented by a `LessonEntity` class. This class must contain the following attributes.

    (a) `UnitId` (UUID): References the unit to which this lesson belongs.

    (b) `Title` (string): The length of this string is limited to a maximum of 500 characters.

    (c) `Description` (string).


### Section 2D - Material

(1) A material is represented by a `MaterialEntity` class. In addition to those specified by Section 2A of `Validation Rules.md`, this class must also contain the following fields.

    (a) `LessonId` (UUID): References the lesson to which this material belongs.
    

(2) In addition to the mandatory fields in (1), a `MaterialEntity` object may have the following fields:

    (a) `ReadingAge` (int).
    (b) `ActualAge` (int).

(3) Additional fields defined in this Section do not apply to the Data Transfer Objects (DTOs) used for communication with the Android client, specified in the API Contract.



## Section 3 - Entity classes Not Belonging to the Material Hierarchy

## Section 3A - Source Document

(1) A source document is represented by a `SourceDocumentEntity` class. This class must contain the following attributes.

    (a) `UnitCollectionId` (UUID): References the unit collection to which this source document is imported.

    (b) `Transcript` (string): A textual transcript of the source document contents.

(2) Data fields defined in this Section must also conform to all the following constraints for the object to be valid:

    (a) The `UnitCollectionId` specified in (1)(a) must associate with a valid `UnitCollectionEntity`.


## Section 3B - Attachment

(1) An attachment is represented by an `AttachmentEntity` class. This class must contain the following attributes.

    (a) `MaterialId` (UUID): References the material to which this attachment belongs.

    (b) `FileName` (string): The name and extension of the attachment file.


(2) Data fields defined in this Section must also conform to all the following constraints for the object to be valid:

    (a) The `MaterialId` specified in (1)(a) must associate with a valid `MaterialEntity`.

    (b) The `FileName` specified in (1)(b) must refer to an existing file (F) in the directory `windows/ManuscriptaTeacherApp/Attachments`. The `FileName` must contain the file's base name, a full stop character `.` and the file's extension in that order.

    (c) The file F must have one of the following extensions.

        (i) `png`.

        (ii) `jpeg`.

        (iii) `pdf`.