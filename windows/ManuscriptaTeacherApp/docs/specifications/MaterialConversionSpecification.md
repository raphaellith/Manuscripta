# Material Conversion Specification (Windows)

## Explanatory Note

This document defines the requirements for converting `MaterialEntity` objects into PDF documents within the Windows application backend. The conversion process shall render material content, embedded attachments, and interactive question elements into a printable format.

## Section 1 — General Principles

(1) This document may be cited as "Material Conversion Specification".

(2) The PDF conversion service shall be implemented in the `Main` component of the Windows application, as a backend service.

(3) The conversion process shall accept a `MaterialEntity` object, as defined in Validation Rules §2A and AdditionalValidationRules §2D, and produce a PDF document.

(4) The PDF output shall faithfully represent the material content as encoded per Material Encoding Specification, subject to the rendering rules defined in this document.

(5) In this document —

    (a) "Material" refers to a `MaterialEntity` object.
    (b) "Content" refers to the markdown-encoded `Content` field of a material.
    (c) "Attachment" refers to an `AttachmentEntity` object associated with the material.
    (d) "Question" refers to a `QuestionEntity` object associated with the material.

## Section 1A — Technical Stack

(1) The service shall use QuestPDF for PDF document generation. The `QuestPDF.Markdown` extension package shall be used to parse and render markdown content.

    [Explanatory Note: QuestPDF is a .NET library providing a fluent API for programmatic PDF creation. The `QuestPDF.Markdown` extension uses Markdig as its underlying parser.]

(2) Custom extensions shall be implemented for admonition-style markers (`!!!`) not natively supported by `QuestPDF.Markdown`. The service shall pre-process the content to extract custom markers before passing to the markdown renderer.

(3) Mathematical notation shall be rendered using a server-side LaTeX rendering approach.

    (a) The service may integrate with an external LaTeX rendering engine to convert LaTeX expressions to SVG images.

    (b) Rendered LaTeX shall be embedded as images within the PDF document.

    [Explanatory Note: Suitable LaTeX rendering engines include MathJax-node, KaTeX, or CSharpMath for SkiaSharp-based rendering.]

(4) For embedded PDF attachments (§3C), the service shall use a PDF manipulation library capable of merging pages.

    [Explanatory Note: PdfSharpCore or iText7 may be used for PDF page extraction and insertion.]


## Section 2 — Page Layout

(1) **Page Size**. The PDF shall use A4 page size (210mm × 297mm).

(2) **Margins**. The document shall have the following margins:

    (a) Top margin: 25mm (to accommodate header).
    (b) Bottom margin: 20mm (to accommodate footer).
    (c) Left margin: 20mm.
    (d) Right margin: 20mm.

(3) **Header**. Each page shall display a header containing:

    (a) The Quill Logo, sourced from `UI/src/resources/Quill Logo.png`, aligned to the left.
    (b) The logo shall be scaled proportionally to a maximum height of 15mm.

(4) **Footer**. Each page shall display a footer containing:

    (a) The page number, formatted as "Page X of Y", centred horizontally.
    (b) The font size for the footer shall be 10pt.

(5) **Title Block**. The first page shall begin with a title block containing:

    (a) The material `Title`, rendered as a level 1 heading.
    (b) A horizontal rule separating the title from the content.

## Section 3 — Content Rendering

(1) The service shall parse the `Content` field according to Material Encoding Specification and render each element as specified below.

### Section 3A — Standard Markdown Elements

(1) **Headers**. Markdown headers (§2(1) of Material Encoding Specification) shall be rendered with the following font sizes:

    (a) Level 1 (`#`): 24pt, bold.
    (b) Level 2 (`##`): 20pt, bold.
    (c) Level 3 (`###`): 16pt, bold.
    (d) Levels 4 and beyond: 14pt, bold.

(2) **Body Text**. Standard paragraph text shall be rendered at 12pt with 1.5 line spacing.

(3) **Bold and Italic**. Text formatting (§2(2)–(3) of Material Encoding Specification) shall be preserved in the PDF output.

(4) **Lists**. Ordered and unordered lists (§2(4)–(5) of Material Encoding Specification) shall be rendered with appropriate indentation and bullet/number markers.

(5) **Tables**. Tables (§2(6) of Material Encoding Specification) shall be rendered with visible borders. Column alignment shall be respected.

(6) **LaTeX**. Mathematical notation (§2(7) of Material Encoding Specification) shall be rendered as follows:

    (a) Inline LaTeX (`$...$`) shall be rendered inline with surrounding text, using the QuestPDF `Text` descriptor's `Element()` method to embed rendered images within the text flow.

    Paragraphs containing inline LaTeX shall be rendered as plain text with embedded LaTeX images. Standard markdown formatting (bold, italic) within such paragraphs shall not be preserved.

    [Explanatory Note: QuestPDF.Markdown does not support inline image embedding. Paragraphs containing inline LaTeX therefore bypass the Markdown extension and use the QuestPDF Text API directly, which supports inline element embedding but not automatic markdown parsing.]

    (b) Block LaTeX (`$$...$$`) shall be rendered as a centred block element.
    (c) The service shall use a LaTeX rendering engine to convert notation to vector graphics or high-resolution images.

(7) **Code Blocks**. Code blocks (§2(8) of Material Encoding Specification) shall be rendered in a monospace font with a light grey background.

(8) **Blockquotes**. Blockquotes (§2(9) of Material Encoding Specification) shall be rendered with a left border and indentation.

(9) **Horizontal Rules**. Horizontal rules (§2(10) of Material Encoding Specification) shall be rendered as a thin horizontal line spanning the content width.

### Section 3B — Attachment References

(1) **Images**. Image attachments (§3(1) of Material Encoding Specification) shall be embedded in the PDF at their referenced position.

    (a) Images shall be scaled to fit within the page margins whilst maintaining aspect ratio.
    (b) Large images may be scaled down but shall not exceed the content width.
    (c) The service shall resolve attachment references by reading from the `%AppData%\ManuscriptaTeacherApp\Attachments` directory.

(2) **Invalid References**. If an attachment cannot be resolved, the attachment shall not be rendered.

### Section 3C — PDF Embedding

(1) Embedded PDF markers (`!!! pdf id="..."`, §4(2) of Material Encoding Specification) shall be handled by merging the referenced PDF's pages into the output document.

(2) **Page Insertion**. The pages of the embedded PDF shall be inserted at the position of the marker in the content flow. Pages shall be broken before and after the inserted pages.

(3) **Invalid References**. If the referenced PDF attachment cannot be resolved, it shall not be rendered.

### Section 3D — Custom Markers

(1) **Centred Text**. The `center` marker (§4(3) of Material Encoding Specification) shall render its content horizontally centred.

(2) **Question References**. The `question` marker (§4(4) of Material Encoding Specification) shall be rendered according to Section 4.

## Section 4 — Question Rendering

(1) Questions referenced in the material content shall be rendered as interactive-style elements suitable for print.

(2) **Question Block**. Each question shall be rendered as a distinct block containing:

    (a) A question number, auto-incremented based on order of appearance (e.g., "Question 1", "Question 2").
    (b) The question text, rendered in bold.
    (c) If `MaxScore` is defined, the text "[X]" shall appear right-aligned after the question text, where X is the number of marks the question carries.

(3) **Multiple Choice Questions**. For questions where `QuestionType` is `MULTIPLE_CHOICE`:

    (a) Each option shall be rendered on a separate line, prefixed with an option letter (A, B, C, etc.).
    (b) The options shall be indented relative to the question text.
    (c) The `CorrectAnswer` shall not be indicated in the PDF output.

(4) **Written Answer Questions**. For questions where `QuestionType` is `WRITTEN_ANSWER`:

    (a) A set of blank lines shall be rendered below the question text to provide space for handwritten answers.
    (b) The number of blank lines shall be determined as follows:
        (i) If `MaxScore` is 1 or 2: 3 blank lines.
        (ii) If `MaxScore` is 3 to 5: 5 blank lines.
        (iii) If `MaxScore` is greater than 5: two times the number of blank lines as the maximum score.
    (c) The `CorrectAnswer` and `MarkScheme` shall not be rendered in the PDF output.

(5) **Invalid References**. If the referenced `QuestionEntity` cannot be resolved or is not associated with the material, that question shall not be rendered.

## Section 5 — Service Interface

(1) The PDF conversion functionality shall be exposed through a service class named `MaterialPdfService`.

(2) The service shall provide the following method:

    (a) `GeneratePdfAsync(Guid materialId)`: Accepts a material ID and returns a byte array containing the PDF document, or throws an exception if the material cannot be found.

(3) The generated PDF shall be returned as a byte array and shall not be persisted by this service.

## Section 6 — Error Handling

(1) If the specified material does not exist, the service shall throw a `KeyNotFoundException`.

(2) If the material content is empty or null, the service shall generate a PDF containing only the title block.

(3) Malformed markdown shall be rendered on a best-effort basis, consistent with Material Encoding Specification §1(3).

(4) LaTeX rendering failures shall result in the raw LaTeX source being displayed as plain text.
