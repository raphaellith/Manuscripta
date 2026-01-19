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

(6) For the purpose of this document, "contain" in relation to a data field means "contain a non-null value for".

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

(3) Additional fields defined in this Section shall not appear in the Data Transfer Objects (DTOs) used for communication with the Android client, specified in the API Contract.


### Section 2E - Question

(1) A question is represented by a `QuestionEntity` class. In addition to those specified by Section 2B of `Validation Rules.md`, this class may contain the following optional fields:

    (a) `MarkScheme` (string). The mark scheme for the question, for the purpose of AI-marking.

(2) Data fields defined in this Section must also conform to all the following constraints for the object to be valid:

    (a) A `QuestionEntity` object of type `MULTIPLE_CHOICE` must not have a `MarkScheme` defined in (1)(a).

    (b) A `QuestionEntity` object may not simultaneously contain `MarkScheme` and `CorrectAnswer` fields.

(3) Additional fields defined in this Section shall not appear in the Data Transfer Objects (DTOs) used for communication with the Android client, specified in the API Contract.


## Section 3 - Entity classes Not Belonging to the Material Hierarchy

## Section 3A - Source Document

(1) A source document is represented by a `SourceDocumentEntity` class. This class must contain the following attributes.

    (a) `UnitCollectionId` (UUID): References the unit collection to which this source document is imported.

    (b) `Transcript` (string): A textual transcript of the source document contents.

    (c) `EmbeddingStatus` (enum EmbeddingStatus, optional): Tracks the indexing state of the document for semantic retrieval. If not set, the document has not been submitted for indexing. Possible values are:

        (i) `PENDING` — The document is queued for indexing or indexing is in progress.

        (ii) `INDEXED` — The document has been successfully indexed and is available for semantic retrieval.

        (iii) `FAILED` — The indexing process failed. The document is not available for semantic retrieval.

(2) Data fields defined in this Section must also conform to all the following constraints for the object to be valid:

    (a) The `UnitCollectionId` specified in (1)(a) must associate with a valid `UnitCollectionEntity`.

(3) A source document with unset `EmbeddingStatus` shall be treated as not available for semantic retrieval. The frontend may offer an option to initiate indexing.


### Section 3AB - Generation Request

(1) A generation request is represented by a `GenerationRequest` class. This class must contain the following attributes:

    (a) `Description` (string) — The teacher's description of desired content.

    (b) `ReadingAge` (int) — Target reading age.

    (c) `ActualAge` (int) — Actual age of the audience.

    (d) `DurationInMinutes` (int) — Approximate completion time.

    (e) `UnitCollectionId` (Guid) — The unit collection containing source documents.

(2) In addition to the mandatory fields in (1), a `GenerationRequest` object may have the following optional fields:

    (a) `SourceDocumentIds` (List<Guid>) — If provided, limits semantic retrieval to the specified source documents. If null or empty, all indexed documents in the unit collection are searched.


### Section 3AC - Generation Result

(1) A generation result is represented by a `GenerationResult` class. This class must contain the following attributes:

    (a) `Content` (string) — The generated or modified content.

(2) In addition to the mandatory fields in (1), a `GenerationResult` object may have the following optional fields:

    (a) `Warnings` (List<ValidationWarning>) — A list of validation issues that could not be automatically resolved. See §3AD.


### Section 3AD - Validation Warning

(1) A validation warning is represented by a `ValidationWarning` class. This class must contain the following attributes:

    (a) `ErrorType` (string) — A code identifying the error type (e.g., `MALFORMED_MARKER`, `UNCLOSED_BLOCK`, `INVALID_REFERENCE`).

    (b) `Description` (string) — A human-readable description of the issue.

(2) In addition to the mandatory fields in (1), a `ValidationWarning` object may have the following optional fields:

    (a) `LineNumber` (int) — The line number where the issue occurs.


### Section 3AE — Feedback

(1) A feedback is represented by a `FeedbackEntity` class. In addition to those specified by Section 2F of `Validation Rules.md`, this class shall contain the following field —

    (a) `Status` (enum FeedbackStatus). Possible values are:

        (i) `PROVISIONAL` — Feedback exists but has not been approved by the teacher. Feedback in this status shall not be dispatched.

        (ii) `READY` — Feedback has been approved and is awaiting dispatch or acknowledgement.

        (iii) `DELIVERED` — Feedback has been dispatched and acknowledged by the student device.

    The default value is `PROVISIONAL`.

(2) Additional fields defined in this Section shall not appear in the Data Transfer Objects (DTOs) used for communication with the Android client, specified in the API Contract.


## Section 3B - Attachment

(1) An attachment is represented by an `AttachmentEntity` class. This class must contain the following attributes.

    (a) `MaterialId` (UUID): References the material to which this attachment belongs.

    (b) `FileBaseName` (string): The base name of the attachment file, as inputted by the user.

    (c) `FileExtension` (string): The extension of the attachment file.


(2) Data fields defined in this Section must also conform to all the following constraints for the object to be valid:

    (a) The `MaterialId` specified in (1)(a) must associate with a valid `MaterialEntity`.

    (b) The `FileExtension` specified in 1(c) must be one of the following.

        (i) `png`.

        (ii) `jpeg`.

        (iii) `pdf`.

    (c) There must exist a file in the directory `%AppData%\ManuscriptaTeacherApp\Attachments` whose file base name matches the UUID of the `AttachmentEntity` and whose file extension matches the `FileExtension` specified in 1(c).
