# Material Encoding Specification

This document defines the markdown-based encoding for the `Content` field of `MaterialEntity` objects, as specified in Validation Rules §2A(1)(c).

## Section 1: General Principles

(1) In this document —

    (a) "Markdown" means the subset of CommonMark and GitHub Flavored Markdown syntax specified in Section 2.
    (b) "Custom marker" means a non-standard syntax element defined in Section 4.
    (c) "Attachment reference" means a reference to a file stored on the Windows server, accessible via the `/attachments/{id}` endpoint.

(2) This document specifies the encoding for the `Content` field of `MaterialEntity` objects.

(3) Applications shall render malformed markdown on a best-effort basis. Invalid syntax elements shall be rendered as plain text where possible.

(4) External URLs shall not be permitted within marker content. All resource references shall use attachment references, as specified in Section 3.

(5) This document may be cited as "Material Encoding Specification".

## Section 2: Standard Markdown Elements

(1) **Headers**. The following header syntax shall be supported:

    (a) `# Header` — Level 1 header.
    (b) `## Header` — Level 2 header.
    (c) `### Header` — Level 3 header.
    (d) Headers of levels 4 and beyond (`####`, etc.) are supported but should be used sparingly.

(2) **Bold**. Text shall be rendered in bold using `**text**` or `__text__` syntax.

(3) **Italic**. Text shall be rendered in italic using `*text*` or `_text_` syntax.

(4) **Unordered Lists**. Unordered lists shall be specified using:

    (a) `- item` — Hyphen prefix.
    (b) `* item` — Asterisk prefix.

(5) **Ordered Lists**. Ordered lists shall be specified using `1. item` syntax. Sequential numbering (`1.`, `2.`, `3.`) or repeated `1.` may be used.

(6) **Tables**. Tables use GitHub Flavored Markdown syntax:

    (a) Headers and data rows shall be separated by pipes (`|`).
    (b) A separator row of dashes (`---`) shall follow the header row.
    (c) Column alignment shall be specified using colons in the separator row:
        (i) `:---` — Left-aligned.
        (ii) `:---:` — Centre-aligned.
        (iii) `---:` — Right-aligned.

(7) **LaTeX**. Mathematical notation shall be supported using LaTeX syntax:

    (a) Inline LaTeX shall be delimited by single dollar signs: `$...$`.
    (b) Block LaTeX shall be delimited by double dollar signs: `$$...$$`.

## Section 3: Attachment References

(1) **Image Syntax**. Images shall be embedded using standard markdown image syntax with attachment paths:

```
![alt text](/attachments/{id})
```

Where `{id}` shall be the UUID of the attachment.

(2) **Attachment Resolution**. Applications shall resolve attachment references by calling the `GET /attachments/{id}` endpoint as specified in API Contract §2.1.3.

(3) **Invalid References**. If an attachment reference cannot be resolved, applications should display a placeholder indicating the missing resource.

## Section 4: Custom Markers

(1) **General Syntax**. Custom markers shall use the Admonition syntax as supported by the flexmark-java library:

```
!!! marker-name attribute="value"
    content
```

Where `marker-name` shall be the marker type and attributes shall be specified as space-separated key-value pairs. Content within the marker shall be indented by four spaces.

(2) **PDF Embed**. PDF documents shall be embedded using the `pdf` marker:

```
!!! pdf id="attachment-uuid"
```

    (a) The `id` attribute shall be mandatory and shall reference a valid attachment.
    (b) Applications shall render an embedded PDF viewer or a download link.

(3) **Centred Text**. Text shall be centred using the `center` marker:

```
!!! center
    This text will be centred.
```

    (a) Content within the marker may include other markdown elements, each line indented by four spaces.
    (b) Nested custom markers shall not be supported.

(4) **Question Reference**. A reference to a `QuestionEntity` shall be embedded using the `question` marker:

```
:::question{id="question-uuid"}
:::
```

    (a) The `id` attribute shall be mandatory and shall reference a valid `QuestionEntity` associated with the material, as defined in Validation Rules §2B.
    (b) Applications shall render the referenced question inline, including the question text and any options or input fields as appropriate to the `QuestionType`.
    (c) The referenced `QuestionEntity` shall have a `MaterialId` matching the containing material's ID. If the referenced `QuestionEntity` does not exist or is not associated with the material, applications shall display a placeholder indicating the missing resource.

## Appendix 1: Syntax Summary

| Element | Syntax | Example |
|---------|--------|---------|
| Header 1 | `# text` | `# Introduction` |
| Header 2 | `## text` | `## Chapter 1` |
| Header 3 | `### text` | `### Section 1.1` |
| Bold | `**text**` | `**important**` |
| Italic | `*text*` | `*emphasis*` |
| Unordered list | `- item` | `- First point` |
| Ordered list | `1. item` | `1. Step one` |
| Table | `\| col \| col \|` | See §2(6) |
| Inline LaTeX | `$...$` | `$x^2 + y^2 = z^2$` |
| Block LaTeX | `$$...$$` | `$$\int_0^1 f(x) dx$$` |
| Image | `![alt](/attachments/id)` | `![diagram](/attachments/abc-123)` |
| PDF embed | `!!! pdf id="..."` | `!!! pdf id="abc-123"` |
| Centred text | `!!! center` | See §4(3) |
| Question ref | `!!! question id="..."` | See §4(4) |
