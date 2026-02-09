using System.Text.RegularExpressions;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Services.Repositories;
using PdfSharpCore.Pdf;
using PdfSharpCore.Pdf.IO;
using QuestPDF.Fluent;
using QuestPDF.Helpers;
using QuestPDF.Infrastructure;
using QuestPDF.Markdown;

namespace Main.Services;

/// <summary>
/// Service for generating PDF documents from materials.
/// Per MaterialConversionSpecification.
/// </summary>
public class MaterialPdfService : IMaterialPdfService
{
    private readonly IMaterialRepository _materialRepository;
    private readonly IQuestionRepository _questionRepository;
    private readonly IAttachmentRepository _attachmentRepository;
    private readonly IFileService _fileService;

    public MaterialPdfService(
        IMaterialRepository materialRepository,
        IQuestionRepository questionRepository,
        IAttachmentRepository attachmentRepository,
        IFileService fileService)
    {
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
        _attachmentRepository = attachmentRepository ?? throw new ArgumentNullException(nameof(attachmentRepository));
        _fileService = fileService ?? throw new ArgumentNullException(nameof(fileService));
    }

    /// <inheritdoc/>
    public async Task<byte[]> GeneratePdfAsync(Guid materialId)
    {
        // Retrieve material
        var material = await _materialRepository.GetByIdAsync(materialId)
            ?? throw new KeyNotFoundException($"Material with ID {materialId} not found.");

        // Retrieve associated questions and attachments
        var questions = (await _questionRepository.GetByMaterialIdAsync(materialId)).ToList();
        var attachments = (await _attachmentRepository.GetByMaterialIdAsync(materialId)).ToList();

        // Build question lookup by ID
        var questionLookup = questions.ToDictionary(q => q.Id.ToString(), StringComparer.OrdinalIgnoreCase);

        // Build attachment lookup by ID
        var attachmentLookup = attachments.ToDictionary(a => a.Id.ToString(), StringComparer.OrdinalIgnoreCase);

        // Configure QuestPDF license
        QuestPDF.Settings.License = LicenseType.Community;

        // Track PDF embeds for post-processing merge
        var pdfEmbeds = new List<PdfEmbedInfo>();

        // Generate document
        var document = Document.Create(container =>
        {
            container.Page(page =>
            {
                // Page settings per MaterialConversionSpecification §2
                page.Size(PageSizes.A4);
                page.MarginTop(25, Unit.Millimetre);
                page.MarginBottom(20, Unit.Millimetre);
                page.MarginLeft(20, Unit.Millimetre);
                page.MarginRight(20, Unit.Millimetre);

                // Header per §2(3)
                page.Header().Element(container => RenderHeader(container));

                // Footer per §2(4)
                page.Footer().Element(container => RenderFooter(container));

                // Content
                page.Content().Element(container => 
                    RenderContent(container, material, questionLookup, attachmentLookup, pdfEmbeds));
            });
        });

        var basePdfBytes = document.GeneratePdf();

        // If no PDF embeds, return the base PDF directly
        if (pdfEmbeds.Count == 0)
        {
            return basePdfBytes;
        }

        // Post-process: merge external PDFs at the placeholder positions
        return MergePdfEmbeds(basePdfBytes, pdfEmbeds);
    }

    /// <summary>
    /// Merges external PDF files into the base document at placeholder positions.
    /// Per MaterialConversionSpecification §3C(2).
    /// </summary>
    private static byte[] MergePdfEmbeds(byte[] basePdfBytes, List<PdfEmbedInfo> pdfEmbeds)
    {
        using var baseStream = new MemoryStream(basePdfBytes);
        using var outputDocument = PdfReader.Open(baseStream, PdfDocumentOpenMode.Import);
        using var resultDocument = new PdfDocument();

        // Sort embeds by placeholder page number (descending to maintain indices)
        var sortedEmbeds = pdfEmbeds.OrderBy(e => e.PlaceholderPageNumber).ToList();

        // Track which pages are placeholders
        var placeholderPages = new HashSet<int>(pdfEmbeds.Select(e => e.PlaceholderPageNumber));

        var currentResultPage = 0;
        for (var i = 0; i < outputDocument.PageCount; i++)
        {
            // Check if this is a placeholder page
            var embed = sortedEmbeds.FirstOrDefault(e => e.PlaceholderPageNumber == i);
            
            if (embed != null)
            {
                // Insert the external PDF pages instead of the placeholder
                try
                {
                    using var externalStream = new FileStream(embed.FilePath, FileMode.Open, FileAccess.Read);
                    using var externalPdf = PdfReader.Open(externalStream, PdfDocumentOpenMode.Import);
                    
                    foreach (var externalPage in externalPdf.Pages)
                    {
                        resultDocument.AddPage(externalPage);
                        currentResultPage++;
                    }
                }
                catch (Exception)
                {
                    // If we can't read the external PDF, keep the placeholder page
                    resultDocument.AddPage(outputDocument.Pages[i]);
                    currentResultPage++;
                }
            }
            else
            {
                // Regular page - copy it
                resultDocument.AddPage(outputDocument.Pages[i]);
                currentResultPage++;
            }
        }

        using var outputStream = new MemoryStream();
        resultDocument.Save(outputStream);
        return outputStream.ToArray();
    }

    /// <summary>
    /// Renders the page header with Quill Logo.
    /// Per MaterialConversionSpecification §2(3).
    /// </summary>
    private static void RenderHeader(IContainer container)
    {
        container.Row(row =>
        {
            row.RelativeItem().AlignLeft().Height(15, Unit.Millimetre).Element(logoContainer =>
            {
                // Load logo from embedded resources
                var assembly = typeof(MaterialPdfService).Assembly;
                using var logoStream = assembly.GetManifestResourceStream("QuillLogo.png");
                
                if (logoStream != null)
                {
                    // Read stream into byte array for QuestPDF
                    using var memoryStream = new MemoryStream();
                    logoStream.CopyTo(memoryStream);
                    logoContainer.Image(memoryStream.ToArray()).FitHeight();
                }
                else
                {
                    // Fallback if logo not found
                    logoContainer.Text("Manuscripta").Bold().FontSize(14);
                }
            });
        });
    }

    /// <summary>
    /// Renders the page footer with page numbers.
    /// Per MaterialConversionSpecification §2(4).
    /// </summary>
    private static void RenderFooter(IContainer container)
    {
        container.AlignCenter().Text(text =>
        {
            text.Span("Page ").FontSize(10);
            text.CurrentPageNumber().FontSize(10);
            text.Span(" of ").FontSize(10);
            text.TotalPages().FontSize(10);
        });
    }

    /// <summary>
    /// Renders the main content area.
    /// Per MaterialConversionSpecification §2(5) and §3.
    /// </summary>
    private void RenderContent(
        IContainer container,
        MaterialEntity material,
        Dictionary<string, QuestionEntity> questionLookup,
        Dictionary<string, AttachmentEntity> attachmentLookup,
        List<PdfEmbedInfo> pdfEmbeds)
    {
        container.Column(column =>
        {
            column.Spacing(10);

            // Title block per §2(5)
            column.Item().Text(material.Title).FontSize(24).Bold();
            column.Item().LineHorizontal(1).LineColor(Colors.Grey.Medium);
            column.Item().PaddingBottom(10);

            // Render content with custom marker support
            var content = material.Content ?? string.Empty;
            var questionNumber = 0;

            // Process content - split by custom markers and render segments
            RenderContentWithMarkers(column, content, questionLookup, attachmentLookup, ref questionNumber, pdfEmbeds);
        });
    }

    /// <summary>
    /// Renders content with custom marker support.
    /// Pre-processes custom markers per MaterialConversionSpecification §1A(2).
    /// </summary>
    private void RenderContentWithMarkers(
        ColumnDescriptor column,
        string content,
        Dictionary<string, QuestionEntity> questionLookup,
        Dictionary<string, AttachmentEntity> attachmentLookup,
        ref int questionNumber,
        List<PdfEmbedInfo> pdfEmbeds)
    {
        // Unified marker pattern to find all special markers in order
        // We'll process content linearly, handling each marker type as we encounter it
        var combinedPattern = new Regex(
            @"(!!! question id=""([^""]+)"")|" +  // Group 1,2: question marker
            @"(!!! pdf id=""([^""]+)"")|" +       // Group 3,4: pdf marker
            @"(!\[([^\]]*)\]\(/attachments/([^)]+)\))|" + // Group 5,6,7: image attachment
            @"(!!! center\s*\n((?:    .+\n?)+))", // Group 8,9: center marker
            RegexOptions.Multiline);

        var matches = combinedPattern.Matches(content);
        var lastIndex = 0;

        foreach (Match match in matches)
        {
            // Render markdown content before this marker
            if (match.Index > lastIndex)
            {
                var markdownSegment = content.Substring(lastIndex, match.Index - lastIndex);
                RenderPureMarkdown(column, markdownSegment);
            }

            // Determine which marker type matched
            if (match.Groups[1].Success)
            {
                // Question marker
                var questionId = match.Groups[2].Value;
                questionNumber++;
                RenderQuestion(column, questionId, questionNumber, questionLookup);
            }
            else if (match.Groups[3].Success)
            {
                // PDF embed marker - per §3C
                var pdfId = match.Groups[4].Value;
                RenderPdfEmbed(column, pdfId, attachmentLookup, pdfEmbeds);
            }
            else if (match.Groups[5].Success)
            {
                // Image attachment - per §3B
                var altText = match.Groups[6].Value;
                var attachmentId = match.Groups[7].Value;
                RenderImageAttachment(column, attachmentId, altText, attachmentLookup);
            }
            else if (match.Groups[8].Success)
            {
                // Center marker - per §3D(1)
                var centeredContent = match.Groups[9].Value;
                RenderCenteredContent(column, centeredContent);
            }

            lastIndex = match.Index + match.Length;
        }

        // Render remaining content after last marker
        if (lastIndex < content.Length)
        {
            var remainingContent = content.Substring(lastIndex);
            RenderPureMarkdown(column, remainingContent);
        }
    }

    /// <summary>
    /// Renders centered content.
    /// Per MaterialConversionSpecification §3D(1).
    /// </summary>
    private static void RenderCenteredContent(ColumnDescriptor column, string content)
    {
        // Remove 4-space indentation from each line per Material Encoding Spec §4(3)
        var unindentedContent = string.Join("\n", 
            content.Split('\n')
                .Select(line => line.StartsWith("    ") ? line.Substring(4) : line));
        
        // Render centered markdown content
        if (!string.IsNullOrWhiteSpace(unindentedContent))
        {
            column.Item().AlignCenter().Markdown(unindentedContent.Trim());
        }
    }

    /// <summary>
    /// Renders pure markdown without attachment references.
    /// Per MaterialConversionSpecification §3A.
    /// Note: Custom markers (center, question, pdf) are handled by RenderContentWithMarkers.
    /// </summary>
    private static void RenderPureMarkdown(ColumnDescriptor column, string markdown)
    {
        if (!string.IsNullOrWhiteSpace(markdown))
        {
            column.Item().Markdown(markdown);
        }
    }

    /// <summary>
    /// Renders an image attachment directly using QuestPDF.
    /// Per MaterialConversionSpecification §3B.
    /// </summary>
    private void RenderImageAttachment(
        ColumnDescriptor column,
        string attachmentId,
        string altText,
        Dictionary<string, AttachmentEntity> attachmentLookup)
    {
        if (attachmentLookup.TryGetValue(attachmentId, out var attachment))
        {
            // Only handle image attachments (png, jpeg, jpg)
            var ext = attachment.FileExtension.ToLowerInvariant();
            if (ext is "png" or "jpeg" or "jpg")
            {
                var filePath = _fileService.GetAttachmentFilePath(attachment.Id, attachment.FileExtension);
                if (_fileService.FileExists(filePath))
                {
                    try
                    {
                        // Render image scaled to fit within content width per §3B(1)(a)-(b)
                        column.Item().Element(container =>
                        {
                            container.Image(filePath).FitWidth();
                        });
                        return;
                    }
                    catch (Exception)
                    {
                        // Fall through to placeholder
                    }
                }
            }
        }

        // Invalid reference or unsupported format per §3B(2)
        column.Item().Background(Colors.Grey.Lighten3).Padding(10)
            .Text($"[Attachment not found: {altText}]").FontSize(10).Italic();
    }

    /// <summary>
    /// Renders an embedded PDF attachment.
    /// Per MaterialConversionSpecification §3C.
    /// Tracks the placeholder page for post-processing PDF merge.
    /// </summary>
    private void RenderPdfEmbed(
        ColumnDescriptor column,
        string pdfId,
        Dictionary<string, AttachmentEntity> attachmentLookup,
        List<PdfEmbedInfo> pdfEmbeds)
    {
        if (attachmentLookup.TryGetValue(pdfId, out var attachment))
        {
            var ext = attachment.FileExtension.ToLowerInvariant();
            if (ext == "pdf")
            {
                var filePath = _fileService.GetAttachmentFilePath(attachment.Id, attachment.FileExtension);
                if (_fileService.FileExists(filePath))
                {
                    // Per §3C(2): Insert a placeholder page that will be replaced
                    // Track this position for post-processing
                    column.Item().PageBreak();
                    
                    // Track this embed for post-processing
                    pdfEmbeds.Add(new PdfEmbedInfo
                    {
                        FilePath = filePath,
                        PlaceholderPageNumber = pdfEmbeds.Count > 0 
                            ? pdfEmbeds[^1].PlaceholderPageNumber + 1 
                            : 1, // Will be recalculated after generation
                        FileName = attachment.FileBaseName
                    });
                    
                    // Empty placeholder page - will be replaced by actual PDF pages
                    column.Item().MinHeight(1);
                    
                    column.Item().PageBreak();
                    return;
                }
            }
        }

        // Invalid reference per §3C(3)
        column.Item().Background(Colors.Grey.Lighten3).Padding(10)
            .Text($"[PDF attachment not found: {pdfId}]").FontSize(10).Italic();
    }

    /// <summary>
    /// Renders a question block.
    /// Per MaterialConversionSpecification §4.
    /// </summary>
    private static void RenderQuestion(
        ColumnDescriptor column,
        string questionId,
        int questionNumber,
        Dictionary<string, QuestionEntity> questionLookup)
    {
        if (!questionLookup.TryGetValue(questionId, out var question))
        {
            // Invalid reference per §4(5)
            return;
        }

        column.Item().PaddingVertical(10).Column(questionColumn =>
        {
            // Question header per §4(2)
            questionColumn.Item().Row(row =>
            {
                row.RelativeItem().Text(text =>
                {
                    text.Span($"Question {questionNumber}: ").Bold().FontSize(12);
                    text.Span(question.QuestionText).Bold().FontSize(12);
                });

                // Max score per §4(2)(c)
                if (question.MaxScore.HasValue)
                {
                    row.ConstantItem(80).AlignRight().Text($"[{question.MaxScore.Value}]").FontSize(10).Bold();
                }
            });

            // Render based on question type
            switch (question.QuestionType)
            {
                case QuestionType.MULTIPLE_CHOICE:
                    RenderMultipleChoiceQuestion(questionColumn, question as MultipleChoiceQuestionEntity);
                    break;
                case QuestionType.WRITTEN_ANSWER:
                    RenderWrittenAnswerQuestion(questionColumn, question as WrittenAnswerQuestionEntity);
                    break;
            }
        });
    }

    /// <summary>
    /// Renders a multiple choice question.
    /// Per MaterialConversionSpecification §4(3).
    /// </summary>
    private static void RenderMultipleChoiceQuestion(
        ColumnDescriptor questionColumn,
        MultipleChoiceQuestionEntity? question)
    {
        if (question == null) return;

        var optionLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int i = 0; i < question.Options.Count && i < optionLetters.Length; i++)
        {
            var optionLetter = optionLetters[i];
            var optionText = question.Options[i];

            // Checkbox and option per §4(3)(a)-(b)
            questionColumn.Item().PaddingLeft(20).Row(row =>
            {
                row.ConstantItem(30).Text($"{optionLetter} ").FontSize(11).Bold();
                row.RelativeItem().Text(optionText).FontSize(11);
            });
        }
    }

    /// <summary>
    /// Renders a written answer question.
    /// Per MaterialConversionSpecification §4(4).
    /// </summary>
    private static void RenderWrittenAnswerQuestion(
        ColumnDescriptor questionColumn,
        WrittenAnswerQuestionEntity? question)
    {
        if (question == null) return;

        // Calculate blank lines per §4(4)(b)
        int blankLines;
        if (question.MaxScore is null or >= 1 and <= 2)
        {
            blankLines = 3;
        }
        else if (question.MaxScore >= 3 && question.MaxScore <= 5)
        {
            blankLines = 5;
        }
        else if (question.MaxScore > 5)
        {
            // Per §4(4)(b)(iii): two times the max score
            blankLines = question.MaxScore.Value * 2;
        }
        else
        {
            blankLines = 5;
        }

        // Render blank lines for answers per §4(4)(a)
        for (int i = 0; i < blankLines; i++)
        {
            questionColumn.Item().PaddingTop(15).BorderBottom(1).BorderColor(Colors.Grey.Medium);
        }
    }
}

/// <summary>
/// Tracks information about PDF embeds for post-processing merge.
/// </summary>
public class PdfEmbedInfo
{
    /// <summary>
    /// Path to the external PDF file to embed.
    /// </summary>
    public required string FilePath { get; set; }
    
    /// <summary>
    /// The placeholder page number in the base PDF that will be replaced.
    /// </summary>
    public int PlaceholderPageNumber { get; set; }
    
    /// <summary>
    /// Original filename for reference.
    /// </summary>
    public string? FileName { get; set; }
}
