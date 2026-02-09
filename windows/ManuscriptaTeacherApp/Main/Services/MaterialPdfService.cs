using System.Text.RegularExpressions;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Services.Repositories;
using PdfSharpCore.Drawing;
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

        // Split content at PDF embed markers
        var contentSegments = SplitContentAtPdfMarkers(
            material.Content ?? string.Empty, 
            attachmentLookup);

        // Generate PDFs for each segment and concatenate with external PDFs
        var finalPdf = GenerateSegmentedPdf(material, contentSegments, questionLookup, attachmentLookup);

        // Add page numbers at the end
        return AddPageNumbers(finalPdf);
    }

    /// <summary>
    /// Splits content at PDF embed markers, returning segments of content
    /// alternating with PDF file paths.
    /// </summary>
    private List<ContentSegment> SplitContentAtPdfMarkers(
        string content,
        Dictionary<string, AttachmentEntity> attachmentLookup)
    {
        var segments = new List<ContentSegment>();
        var pdfMarkerPattern = new Regex(@"^!!! pdf id=""([^""]+)""", RegexOptions.Multiline);
        var matches = pdfMarkerPattern.Matches(content);
        
        var lastIndex = 0;
        foreach (Match match in matches)
        {
            // Add content before this PDF marker
            if (match.Index > lastIndex)
            {
                var textContent = content.Substring(lastIndex, match.Index - lastIndex);
                segments.Add(new ContentSegment { Type = SegmentType.Content, Content = textContent });
            }

            // Add the PDF embed segment
            var pdfId = match.Groups[1].Value;
            if (attachmentLookup.TryGetValue(pdfId, out var attachment) && 
                attachment.FileExtension.ToLowerInvariant() == "pdf")
            {
                var filePath = _fileService.GetAttachmentFilePath(attachment.Id, attachment.FileExtension);
                if (_fileService.FileExists(filePath))
                {
                    segments.Add(new ContentSegment 
                    { 
                        Type = SegmentType.PdfEmbed, 
                        FilePath = filePath,
                        FileName = attachment.FileBaseName
                    });
                }
                else
                {
                    // PDF not found - add placeholder text
                    segments.Add(new ContentSegment 
                    { 
                        Type = SegmentType.Content, 
                        Content = $"\n\n[PDF attachment not found: {pdfId}]\n\n" 
                    });
                }
            }
            else
            {
                // Invalid reference - add placeholder text
                segments.Add(new ContentSegment 
                { 
                    Type = SegmentType.Content, 
                    Content = $"\n\n[PDF attachment not found: {pdfId}]\n\n" 
                });
            }

            lastIndex = match.Index + match.Length;
        }

        // Add remaining content after the last marker
        if (lastIndex < content.Length)
        {
            var textContent = content.Substring(lastIndex);
            segments.Add(new ContentSegment { Type = SegmentType.Content, Content = textContent });
        }

        // If no segments were created (no content), add an empty segment
        if (segments.Count == 0)
        {
            segments.Add(new ContentSegment { Type = SegmentType.Content, Content = string.Empty });
        }

        return segments;
    }

    /// <summary>
    /// Generates a single PDF by rendering segments separately and concatenating
    /// with external PDFs at the appropriate positions.
    /// </summary>
    private byte[] GenerateSegmentedPdf(
        MaterialEntity material,
        List<ContentSegment> segments,
        Dictionary<string, QuestionEntity> questionLookup,
        Dictionary<string, AttachmentEntity> attachmentLookup)
    {
        using var resultDocument = new PdfDocument();
        var isFirstSegment = true;
        var questionNumber = 0;

        foreach (var segment in segments)
        {
            if (segment.Type == SegmentType.PdfEmbed)
            {
                // Per §3C(2): Insert external PDF pages
                try
                {
                    using var externalStream = new FileStream(segment.FilePath!, FileMode.Open, FileAccess.Read);
                    using var externalPdf = PdfReader.Open(externalStream, PdfDocumentOpenMode.Import);
                    
                    foreach (var page in externalPdf.Pages)
                    {
                        resultDocument.AddPage(page);
                    }
                }
                catch
                {
                    // If we can't read the PDF, add an error page
                    var errorPage = resultDocument.AddPage();
                    using var gfx = XGraphics.FromPdfPage(errorPage);
                    gfx.DrawString($"[Failed to load PDF: {segment.FileName}]", 
                        new XFont("Arial", 12), XBrushes.Red, 
                        new XPoint(100, 100));
                }
            }
            else
            {
                // Render content segment using QuestPDF
                var segmentPdfBytes = RenderContentSegment(
                    material, 
                    segment.Content ?? string.Empty, 
                    isFirstSegment,
                    questionLookup, 
                    attachmentLookup,
                    ref questionNumber);

                // Add pages from this segment to the result
                using var segmentStream = new MemoryStream(segmentPdfBytes);
                using var segmentPdf = PdfReader.Open(segmentStream, PdfDocumentOpenMode.Import);
                
                foreach (var page in segmentPdf.Pages)
                {
                    resultDocument.AddPage(page);
                }

                isFirstSegment = false;
            }
        }

        using var outputStream = new MemoryStream();
        resultDocument.Save(outputStream);
        return outputStream.ToArray();
    }

    /// <summary>
    /// Renders a single content segment as a PDF using QuestPDF.
    /// </summary>
    private byte[] RenderContentSegment(
        MaterialEntity material,
        string content,
        bool includeTitle,
        Dictionary<string, QuestionEntity> questionLookup,
        Dictionary<string, AttachmentEntity> attachmentLookup,
        ref int questionNumber)
    {
        // Capture the current question number for use in the closure
        var startQuestionNumber = questionNumber;
        
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

                // Footer - space reserved, page numbers added in post-processing
                page.Footer().Element(container => RenderFooter(container));

                // Content
                page.Content().Element(contentContainer =>
                {
                    contentContainer.Column(column =>
                    {
                        column.Spacing(10);

                        // Title block per §2(5) - only for first segment
                        if (includeTitle)
                        {
                            column.Item().Text(material.Title).FontSize(24).Bold();
                            column.Item().LineHorizontal(1).LineColor(Colors.Grey.Medium);
                            column.Item().PaddingBottom(10);
                        }

                        // Render content (without PDF markers - they're already split out)
                        var localQuestionNumber = startQuestionNumber;
                        RenderContentWithMarkers(column, content, questionLookup, attachmentLookup, ref localQuestionNumber);
                        startQuestionNumber = localQuestionNumber;
                    });
                });
            });
        });

        // Update the caller's question number
        questionNumber = startQuestionNumber;

        return document.GeneratePdf();
    }

    /// <summary>
    /// Adds page numbers to all pages of a PDF document.
    /// Per MaterialConversionSpecification §2(4).
    /// This is done post-processing to ensure all pages are correctly counted.
    /// </summary>
    private static byte[] AddPageNumbers(byte[] pdfBytes)
    {
        using var inputStream = new MemoryStream(pdfBytes);
        using var document = PdfReader.Open(inputStream, PdfDocumentOpenMode.Modify);
        
        var totalPages = document.PageCount;
        var font = new XFont("Arial", 10, XFontStyle.Regular);
        
        for (var i = 0; i < document.PageCount; i++)
        {
            var page = document.Pages[i];
            using var gfx = XGraphics.FromPdfPage(page);
            
            var pageText = $"Page {i + 1} of {totalPages}";
            var textWidth = gfx.MeasureString(pageText, font).Width;
            
            // Position at bottom center of page (20mm from bottom, centered)
            var x = (page.Width.Point / 2) - (textWidth / 2);
            var y = page.Height.Point - 40; // ~14mm from bottom
            
            gfx.DrawString(pageText, font, XBrushes.Black, new XPoint(x, y));
        }
        
        using var outputStream = new MemoryStream();
        document.Save(outputStream);
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
    /// Renders the page footer.
    /// Per MaterialConversionSpecification §2(4).
    /// Note: Page numbers are added during post-processing to correctly count all pages.
    /// </summary>
    private static void RenderFooter(IContainer container)
    {
        // Footer space reserved - page numbers are added in post-processing
        container.Height(15);
    }

    /// <summary>
    /// Renders content with custom marker support (excluding PDF markers which are pre-processed).
    /// Pre-processes custom markers per MaterialConversionSpecification §1A(2).
    /// </summary>
    private void RenderContentWithMarkers(
        ColumnDescriptor column,
        string content,
        Dictionary<string, QuestionEntity> questionLookup,
        Dictionary<string, AttachmentEntity> attachmentLookup,
        ref int questionNumber)
    {
        // Pattern for markers (excluding PDF which is handled at higher level)
        var combinedPattern = new Regex(
            @"(!!! question id=""([^""]+)"")|" +  // Group 1,2: question marker
            @"(!\[([^\]]*)\]\(/attachments/([^)]+)\))|" + // Group 3,4,5: image attachment
            @"(!!! center\s*\n((?:    .+\n?)+))", // Group 6,7: center marker
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
                // Image attachment marker - per §3B
                var altText = match.Groups[4].Value;
                var attachmentId = match.Groups[5].Value;
                RenderImageAttachment(column, attachmentId, altText, attachmentLookup);
            }
            else if (match.Groups[6].Success)
            {
                // Center marker - per §3D(1)
                var centeredContent = match.Groups[7].Value;
                RenderCenteredContent(column, centeredContent);
            }

            lastIndex = match.Index + match.Length;
        }

        // Render remaining markdown after the last marker
        if (lastIndex < content.Length)
        {
            var markdownSegment = content.Substring(lastIndex);
            RenderPureMarkdown(column, markdownSegment);
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
    /// Note: Custom markers (center, question, pdf) are handled separately.
    /// </summary>
    private static void RenderPureMarkdown(ColumnDescriptor column, string markdown)
    {
        if (!string.IsNullOrWhiteSpace(markdown))
        {
            column.Item().Markdown(markdown);
        }
    }

    /// <summary>
    /// Renders an image attachment.
    /// Per MaterialConversionSpecification §3B.
    /// </summary>
    private void RenderImageAttachment(
        ColumnDescriptor column,
        string attachmentId,
        string altText,
        Dictionary<string, AttachmentEntity> attachmentLookup)
    {
        if (!attachmentLookup.TryGetValue(attachmentId, out var attachment))
        {
            // Invalid reference per §3B(2)
            column.Item().Background(Colors.Grey.Lighten3).Padding(10)
                .Text($"[Attachment not found: {altText}]").FontSize(10).Italic();
            return;
        }

        var filePath = _fileService.GetAttachmentFilePath(attachment.Id, attachment.FileExtension);
        if (!_fileService.FileExists(filePath))
        {
            // File not found per §3B(2)
            column.Item().Background(Colors.Grey.Lighten3).Padding(10)
                .Text($"[Attachment not found: {altText}]").FontSize(10).Italic();
            return;
        }

        var ext = attachment.FileExtension.ToLowerInvariant();
        if (ext is "png" or "jpg" or "jpeg" or "gif" or "bmp" or "webp")
        {
            // Per §3B(1)(a-b): Render image scaled to fit within page width
            try
            {
                column.Item().Image(filePath).FitWidth();
                return;
            }
            catch
            {
                // Fall through to error case
            }
        }

        // Unsupported format or error
        column.Item().Background(Colors.Grey.Lighten3).Padding(10)
            .Text($"[Attachment not found: {altText}]").FontSize(10).Italic();
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

        column.Item().PaddingTop(10).Column(questionColumn =>
        {
            // Question header with number per §4(2)(a-b)
            questionColumn.Item().Row(row =>
            {
                row.RelativeItem().Text(text =>
                {
                    text.Span($"Question {questionNumber}. ").Bold();
                    text.Span(question.QuestionText ?? string.Empty);
                });
                
                // Max score per §4(2)(c)
                if (question.MaxScore.HasValue)
                {
                    row.ConstantItem(80).AlignRight().Text($"[{question.MaxScore.Value}]").FontSize(10).Bold();
                }
            });

            // Render question type-specific content
            if (question.QuestionType == QuestionType.MULTIPLE_CHOICE)
            {
                RenderMultipleChoiceQuestion(questionColumn, question);
            }
            else
            {
                RenderWrittenAnswerQuestion(questionColumn, question);
            }
        });
    }

    /// <summary>
    /// Renders a multiple choice question with options.
    /// Per MaterialConversionSpecification §4(3).
    /// </summary>
    private static void RenderMultipleChoiceQuestion(ColumnDescriptor questionColumn, QuestionEntity question)
    {
        // Cast to MCQ entity to access Options
        if (question is not MultipleChoiceQuestionEntity mcq || mcq.Options == null)
            return;

        var optionLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < mcq.Options.Count && i < 26; i++)
        {
            var optionLetter = optionLetters[i];
            var optionText = mcq.Options[i];
            
            // Checkbox and option per §4(3)(a)-(b)
            questionColumn.Item().PaddingLeft(20).Row(row =>
            {
                row.ConstantItem(30).Text($"{optionLetter} ").FontSize(11).Bold();
                row.RelativeItem().Text(optionText).FontSize(11);
            });
        }
    }

    /// <summary>
    /// Renders a written answer question with blank lines.
    /// Per MaterialConversionSpecification §4(4).
    /// </summary>
    private static void RenderWrittenAnswerQuestion(ColumnDescriptor questionColumn, QuestionEntity question)
    {
        // Calculate blank lines per §4(4)(b)
        var blankLines = 3; // Default
        if (question.MaxScore.HasValue)
        {
            if (question.MaxScore.Value <= 2)
            {
                blankLines = question.MaxScore.Value;
            }
            else if (question.MaxScore.Value <= 5)
            {
                blankLines = question.MaxScore.Value;
            }
            else
            {
                blankLines = question.MaxScore.Value * 2;
            }
        }

        // Cap reasonable max
        if (blankLines > 20)
        {
            blankLines = 20;
        }

        // Render blank lines for answers per §4(4)(a)
        for (int i = 0; i < blankLines; i++)
        {
            questionColumn.Item().PaddingTop(30).BorderBottom(1).BorderColor(Colors.Grey.Medium);
        }
    }
}

/// <summary>
/// Represents a segment of content for PDF generation.
/// </summary>
public class ContentSegment
{
    public SegmentType Type { get; set; }
    public string? Content { get; set; }
    public string? FilePath { get; set; }
    public string? FileName { get; set; }
}

/// <summary>
/// Type of content segment.
/// </summary>
public enum SegmentType
{
    Content,
    PdfEmbed
}
