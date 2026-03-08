using System.Text.RegularExpressions;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Services.Latex;
using Main.Services.Repositories;
using PdfSharpCore.Drawing;
using PdfSharpCore.Pdf;
using PdfSharpCore.Pdf.IO;
using QuestPDF.Fluent;
using QuestPDF.Helpers;
using QuestPDF.Infrastructure;
using QuestPDF.Markdown;
using SkiaSharp;

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
    private readonly ILatexRenderer _latexRenderer;
    private readonly IPdfExportSettingsRepository _pdfExportSettingsRepository;
    private readonly IResponseRepository _responseRepository;
    private readonly IFeedbackRepository _feedbackRepository;
    private readonly IDeviceRegistryService _deviceRegistryService;

    public MaterialPdfService(
        IMaterialRepository materialRepository,
        IQuestionRepository questionRepository,
        IAttachmentRepository attachmentRepository,
        IFileService fileService,
        ILatexRenderer latexRenderer,
        IPdfExportSettingsRepository pdfExportSettingsRepository,
        IResponseRepository responseRepository,
        IFeedbackRepository feedbackRepository,
        IDeviceRegistryService deviceRegistryService)
    {
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
        _attachmentRepository = attachmentRepository ?? throw new ArgumentNullException(nameof(attachmentRepository));
        _fileService = fileService ?? throw new ArgumentNullException(nameof(fileService));
        _latexRenderer = latexRenderer ?? throw new ArgumentNullException(nameof(latexRenderer));
        _pdfExportSettingsRepository = pdfExportSettingsRepository ?? throw new ArgumentNullException(nameof(pdfExportSettingsRepository));
        _responseRepository = responseRepository ?? throw new ArgumentNullException(nameof(responseRepository));
        _feedbackRepository = feedbackRepository ?? throw new ArgumentNullException(nameof(feedbackRepository));
        _deviceRegistryService = deviceRegistryService ?? throw new ArgumentNullException(nameof(deviceRegistryService));
    }

    /// <inheritdoc/>
    public async Task<byte[]> GeneratePdfAsync(Guid materialId)
    {
        // Retrieve material
        var material = await _materialRepository.GetByIdAsync(materialId)
            ?? throw new KeyNotFoundException($"Material with ID {materialId} not found.");

        // Resolve effective PDF settings per MaterialConversionSpecification §1(5)(h)
        var globalDefaults = await _pdfExportSettingsRepository.GetAsync();
        var effectiveSettings = new EffectivePdfSettings(
            LinePatternType: material.LinePatternType ?? globalDefaults.LinePatternType,
            LineSpacingPreset: material.LineSpacingPreset ?? globalDefaults.LineSpacingPreset,
            FontSizePreset: material.FontSizePreset ?? globalDefaults.FontSizePreset);

        // Retrieve associated questions and attachments
        var questions = (await _questionRepository.GetByMaterialIdAsync(materialId)).ToList();
        var attachments = (await _attachmentRepository.GetByMaterialIdAsync(materialId)).ToList();

        // Build question lookup by ID
        var questionLookup = questions.ToDictionary(q => q.Id.ToString(), StringComparer.OrdinalIgnoreCase);

        // Build attachment lookup by ID
        var attachmentLookup = attachments.ToDictionary(a => a.Id.ToString(), StringComparer.OrdinalIgnoreCase);

        // Split content at PDF embed markers
        var contentSegments = SplitContentAtPdfMarkers(
            material.Content ?? string.Empty, 
            attachmentLookup);

        // Generate PDFs for each segment and concatenate with external PDFs
        var finalPdf = GenerateSegmentedPdf(material, contentSegments, questionLookup, attachmentLookup, effectiveSettings);

        // Add page numbers at the end
        return AddPageNumbers(finalPdf);
    }

    /// <inheritdoc/>
    public async Task<byte[]> GenerateResponsePdfAsync(Guid materialId, string deviceId, bool includeFeedback, bool includeMarkScheme)
    {
        // Retrieve material per §7(2)(a)
        var material = await _materialRepository.GetByIdAsync(materialId)
            ?? throw new KeyNotFoundException($"Material with ID {materialId} not found.");

        // Resolve effective PDF settings per §7(6) and §1(5)(h)
        var globalDefaults = await _pdfExportSettingsRepository.GetAsync();
        var effectiveSettings = new EffectivePdfSettings(
            LinePatternType: material.LinePatternType ?? globalDefaults.LinePatternType,
            LineSpacingPreset: material.LineSpacingPreset ?? globalDefaults.LineSpacingPreset,
            FontSizePreset: material.FontSizePreset ?? globalDefaults.FontSizePreset);

        // Parse device ID
        if (!Guid.TryParse(deviceId, out var deviceGuid))
            throw new ArgumentException($"Invalid device ID: {deviceId}");

        // Retrieve questions for this material
        var questions = (await _questionRepository.GetByMaterialIdAsync(materialId)).ToList();
        var questionLookup = questions.ToDictionary(q => q.Id, q => q);

        // Extract question IDs in content order (same order as material PDF rendering)
        var orderedQuestionIds = ExtractQuestionIdsFromContent(material.Content ?? string.Empty);

        // Collect responses for this device per §7(2)(b)
        var allResponses = new List<ResponseEntity>();
        foreach (var question in questions)
        {
            var questionResponses = await _responseRepository.GetByQuestionIdAsync(question.Id);
            allResponses.AddRange(questionResponses.Where(r => r.DeviceId == deviceGuid));
        }

        // §7(7): throw if no responses exist from this device
        if (allResponses.Count == 0)
            throw new InvalidOperationException($"No responses exist from device {deviceId} for material {materialId}.");

        var responseLookup = allResponses.ToDictionary(r => r.QuestionId, r => r);

        // Load feedback per response if needed per §7(5)(d)
        var feedbackLookup = new Dictionary<Guid, FeedbackEntity>();
        if (includeFeedback)
        {
            foreach (var response in allResponses)
            {
                var feedback = await _feedbackRepository.GetByResponseIdAsync(response.Id);
                if (feedback != null)
                    feedbackLookup[response.Id] = feedback;
            }
        }

        // Resolve device display name per §7(4)(b)
        var allDevices = await _deviceRegistryService.GetAllAsync();
        var device = allDevices.FirstOrDefault(d => d.DeviceId == deviceGuid);
        var deviceDisplayName = device?.Name ?? deviceId;

        var bodyFontSize = effectiveSettings.BodyFontSizePt;

        // Build QuestPDF document per §7(3)-(5)
        var document = Document.Create(container =>
        {
            container.Page(page =>
            {
                // Page layout per §7(3) (same as §2)
                page.Size(PageSizes.A4);
                page.MarginTop(25, Unit.Millimetre);
                page.MarginBottom(20, Unit.Millimetre);
                page.MarginLeft(20, Unit.Millimetre);
                page.MarginRight(20, Unit.Millimetre);
                page.DefaultTextStyle(x => x.FontSize(bodyFontSize));

                page.Header().Element(c => RenderHeader(c));
                page.Footer().Element(c => RenderFooter(c));

                page.Content().Element(contentContainer =>
                {
                    contentContainer.Column(column =>
                    {
                        column.Spacing(10);

                        // Title block per §7(4)
                        column.Item().Text(material.Title).FontSize(bodyFontSize + 12).Bold();
                        column.Item().Text(deviceDisplayName).FontSize(bodyFontSize + 4);
                        column.Item().Text($"Exported on {DateTime.Now:dd MMM yyyy}").FontSize(bodyFontSize - 2).FontColor(Colors.Grey.Medium);
                        column.Item().LineHorizontal(1).LineColor(Colors.Grey.Medium);
                        column.Item().PaddingBottom(10);

                        // Render questions in content order per §7(5)
                        var questionNumber = 0;
                        foreach (var questionId in orderedQuestionIds)
                        {
                            if (!questionLookup.TryGetValue(questionId, out var question))
                                continue;

                            questionNumber++;
                            responseLookup.TryGetValue(questionId, out var response);

                            column.Item().PaddingTop(10).Column(questionColumn =>
                            {
                                // Question header per §7(5)(a) and §4(2)
                                questionColumn.Item().Row(row =>
                                {
                                    row.RelativeItem().Text(text =>
                                    {
                                        text.Span($"Question {questionNumber}. ").Bold();
                                        text.Span(question.QuestionText ?? string.Empty);
                                    });
                                    if (question.MaxScore.HasValue)
                                    {
                                        var marksFontSize = bodyFontSize - 2;
                                        row.ConstantItem(80).AlignRight().Text($"[{question.MaxScore.Value}]").FontSize(marksFontSize).Bold();
                                    }
                                });

                                if (question.QuestionType == QuestionType.MULTIPLE_CHOICE)
                                {
                                    RenderMcqResponse(questionColumn, question, response, includeMarkScheme, bodyFontSize);
                                }
                                else
                                {
                                    RenderWrittenResponse(questionColumn, question, response, includeMarkScheme, bodyFontSize);
                                }

                                // Feedback per §7(5)(d)
                                if (includeFeedback && response != null && feedbackLookup.TryGetValue(response.Id, out var feedback))
                                {
                                    RenderFeedbackBlock(questionColumn, feedback, question.MaxScore, bodyFontSize);
                                }
                            });
                        }
                    });
                });
            });
        });

        var pdfBytes = document.GeneratePdf();
        return AddPageNumbers(pdfBytes);
    }

    /// <summary>
    /// Extracts question IDs from material content in the order they appear.
    /// Per MaterialConversionSpecification §7(5): "Questions shall be rendered
    /// in the order they appear in the material content."
    /// </summary>
    private static List<Guid> ExtractQuestionIdsFromContent(string content)
    {
        var result = new List<Guid>();
        var questionPattern = new Regex(@"!!! question id=""([^""]+)""", RegexOptions.Multiline);
        foreach (Match match in questionPattern.Matches(content))
        {
            if (Guid.TryParse(match.Groups[1].Value, out var id))
                result.Add(id);
        }
        return result;
    }

    /// <summary>
    /// Renders a multiple choice question's response for the Response PDF.
    /// Per MaterialConversionSpecification §7(5)(b).
    /// </summary>
    private static void RenderMcqResponse(
        ColumnDescriptor questionColumn,
        QuestionEntity question,
        ResponseEntity? response,
        bool includeMarkScheme,
        float bodyFontSize)
    {
        if (response == null)
        {
            // §7(5)(b)(iv): No response
            RenderNoResponse(questionColumn, bodyFontSize);
            return;
        }

        if (question is not MultipleChoiceQuestionEntity mcq || mcq.Options == null)
            return;

        var selectedIndex = response is MultipleChoiceResponseEntity mcr ? mcr.AnswerIndex : -1;
        var correctIndex = mcq.CorrectAnswerIndex;
        var optionLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for (int i = 0; i < mcq.Options.Count && i < 26; i++)
        {
            var optionLetter = optionLetters[i];
            var optionText = mcq.Options[i];
            var isSelected = i == selectedIndex;
            // §7(5)(b)(iii): highlight correct if includeMarkScheme and correctAnswer exists
            var isCorrectOption = includeMarkScheme && correctIndex.HasValue && i == correctIndex.Value;

            questionColumn.Item().PaddingLeft(20).Row(row =>
            {
                row.ConstantItem(30).Text(text =>
                {
                    var span = text.Span($"{optionLetter} ").FontSize(11);
                    if (isSelected) span.Bold();
                });

                row.RelativeItem().Text(text =>
                {
                    var span = text.Span(optionText).FontSize(11);
                    if (isSelected)
                        span.Bold();
                    if (isCorrectOption)
                        span.FontColor(Colors.Green.Medium);
                });

                if (isSelected)
                {
                    row.ConstantItem(20).AlignCenter().Text("●").FontSize(10).Bold();
                }
            });
        }
    }

    /// <summary>
    /// Renders a written answer question's response for the Response PDF.
    /// Per MaterialConversionSpecification §7(5)(c).
    /// </summary>
    private static void RenderWrittenResponse(
        ColumnDescriptor questionColumn,
        QuestionEntity question,
        ResponseEntity? response,
        bool includeMarkScheme,
        float bodyFontSize)
    {
        if (response == null)
        {
            // §7(5)(c)(iv): No response
            RenderNoResponse(questionColumn, bodyFontSize);
        }
        else
        {
            // §7(5)(c)(i): written answer in distinct block with left border
            var answerText = response is WrittenAnswerResponseEntity war ? war.Answer : response.ResponseText ?? string.Empty;
            questionColumn.Item().PaddingTop(5).PaddingLeft(10)
                .BorderLeft(2).BorderColor(Colors.Grey.Medium).PaddingLeft(10)
                .Text(answerText).FontSize(bodyFontSize);
        }

        if (includeMarkScheme)
        {
            // §7(5)(c)(iii): correct answer if exists
            if (question is WrittenAnswerQuestionEntity waq && !string.IsNullOrEmpty(waq.CorrectAnswer))
            {
                questionColumn.Item().PaddingTop(5).Row(row =>
                {
                    row.RelativeItem().Text(text =>
                    {
                        text.Span("Correct Answer: ").Bold().FontSize(bodyFontSize - 1);
                        text.Span(waq.CorrectAnswer).FontSize(bodyFontSize - 1);
                    });
                });

                // Indicate whether the response is correct
                if (response != null && response.IsCorrect.HasValue)
                {
                    var correctText = response.IsCorrect.Value ? "Correct" : "Incorrect";
                    var correctColor = response.IsCorrect.Value ? Colors.Green.Medium : Colors.Red.Medium;
                    questionColumn.Item().PaddingTop(2).Text(correctText).FontSize(bodyFontSize - 1).FontColor(correctColor).Bold();
                }
            }

            // §7(5)(c)(ii): mark scheme if exists
            if (question is WrittenAnswerQuestionEntity waq2 && !string.IsNullOrEmpty(waq2.MarkScheme))
            {
                questionColumn.Item().PaddingTop(5).Row(row =>
                {
                    row.RelativeItem().Text(text =>
                    {
                        text.Span("Mark Scheme: ").Bold().FontSize(bodyFontSize - 1);
                        text.Span(waq2.MarkScheme).FontSize(bodyFontSize - 1);
                    });
                });
            }
        }
    }

    /// <summary>
    /// Renders a feedback block for the Response PDF.
    /// Per MaterialConversionSpecification §7(5)(d).
    /// </summary>
    private static void RenderFeedbackBlock(
        ColumnDescriptor questionColumn,
        FeedbackEntity feedback,
        int? maxScore,
        float bodyFontSize)
    {
        questionColumn.Item().PaddingTop(8)
            .Background(Colors.Grey.Lighten4)
            .Border(1).BorderColor(Colors.Grey.Lighten2)
            .Padding(8)
            .Column(fbColumn =>
            {
                // §7(5)(d)(i): Marks
                if (feedback.Marks.HasValue)
                {
                    var marksText = maxScore.HasValue
                        ? $"Marks: {feedback.Marks.Value} / {maxScore.Value}"
                        : $"Marks: {feedback.Marks.Value}";
                    fbColumn.Item().Text(marksText).FontSize(bodyFontSize - 1).Bold();
                }

                // §7(5)(d)(ii): Feedback text
                if (!string.IsNullOrEmpty(feedback.Text))
                {
                    fbColumn.Item().PaddingTop(feedback.Marks.HasValue ? 4 : 0).Text(text =>
                    {
                        text.Span("Feedback: ").Bold().FontSize(bodyFontSize - 1);
                        text.Span(feedback.Text).FontSize(bodyFontSize - 1);
                    });
                }
            });
    }

    /// <summary>
    /// Renders a "[No response]" placeholder in grey italic.
    /// Per MaterialConversionSpecification §7(5)(b)(iv) and §7(5)(c)(iv).
    /// </summary>
    private static void RenderNoResponse(ColumnDescriptor questionColumn, float bodyFontSize)
    {
        questionColumn.Item().PaddingTop(5).PaddingLeft(10)
            .Text("[No response]").FontSize(bodyFontSize).FontColor(Colors.Grey.Medium).Italic();
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
                // Per §3C(3): Invalid references are not rendered
            }
            // Per §3C(3): Invalid references are not rendered

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
        Dictionary<string, AttachmentEntity> attachmentLookup,
        EffectivePdfSettings effectiveSettings)
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
                    effectiveSettings,
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
        EffectivePdfSettings effectiveSettings,
        ref int questionNumber)
    {
        // Capture the current question number for use in the closure
        var startQuestionNumber = questionNumber;
        var bodyFontSize = effectiveSettings.BodyFontSizePt;
        
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

                // Default text style uses effective font size per §3A(2)
                page.DefaultTextStyle(x => x.FontSize(bodyFontSize));

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
                        // Title uses H1 size: effective font size + 12pt per §3A(1)(a)
                        if (includeTitle)
                        {
                            column.Item().Text(material.Title).FontSize(bodyFontSize + 12).Bold();
                            column.Item().LineHorizontal(1).LineColor(Colors.Grey.Medium);
                            column.Item().PaddingBottom(10);
                        }

                        // Render content (without PDF markers - they're already split out)
                        var localQuestionNumber = startQuestionNumber;
                        RenderContentWithMarkers(column, content, questionLookup, attachmentLookup, effectiveSettings, ref localQuestionNumber);
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
        EffectivePdfSettings effectiveSettings,
        ref int questionNumber)
    {
        // Pattern for markers (excluding PDF which is handled at higher level)
        // Note: Inline LaTeX ($...$) is NOT extracted here; it is handled inside
        // RenderPureMarkdown using the QuestPDF Text API for inline rendering.
        var combinedPattern = new Regex(
            @"(!!! question id=""([^""]+)"")|" +  // Group 1,2: question marker
            @"(!\[([^\]]*)\]\(/attachments/([^)]+)\))|" + // Group 3,4,5: image attachment
            @"(!!! center\s*\n((?:    .+\n?)+))|" + // Group 6,7: center marker
            @"(\$\$([\s\S]*?)\$\$)",                  // Group 8,9: block LaTeX per §3(6)(b)
            RegexOptions.Multiline);

        var matches = combinedPattern.Matches(content);
        var lastIndex = 0;

        foreach (Match match in matches)
        {
            // Render markdown content before this marker
            if (match.Index > lastIndex)
            {
                var markdownSegment = content.Substring(lastIndex, match.Index - lastIndex);
                RenderPureMarkdown(column, markdownSegment, effectiveSettings);
            }

            // Determine which marker type matched
            if (match.Groups[1].Success)
            {
                // Question marker
                var questionId = match.Groups[2].Value;
                questionNumber++;
                RenderQuestion(column, questionId, questionNumber, questionLookup, effectiveSettings);
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
            else if (match.Groups[8].Success)
            {
                // Block LaTeX per §3(6)(b)
                var latex = match.Groups[9].Value.Trim();
                RenderBlockLatex(column, latex);
            }

            lastIndex = match.Index + match.Length;
        }

        // Render remaining markdown after the last marker
        if (lastIndex < content.Length)
        {
            var markdownSegment = content.Substring(lastIndex);
            RenderPureMarkdown(column, markdownSegment, effectiveSettings);
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
    /// Per MaterialConversionSpecification §3A and §3(6)(a).
    /// If the segment contains inline LaTeX ($...$), it is rendered using the QuestPDF
    /// Text API with inline element embedding. Otherwise, QuestPDF.Markdown is used.
    /// </summary>
    private void RenderPureMarkdown(ColumnDescriptor column, string markdown, EffectivePdfSettings effectiveSettings)
    {
        if (string.IsNullOrWhiteSpace(markdown))
            return;

        // Check for inline LaTeX
        var inlineLatexPattern = new Regex(@"(?<!\$)\$(?!\$)([^$]+?)\$(?!\$)");
        if (inlineLatexPattern.IsMatch(markdown))
        {
            RenderTextWithInlineLatex(column, markdown, inlineLatexPattern, effectiveSettings);
        }
        else
        {
            // QuestPDF.Markdown inherits the container's DefaultTextStyle (set at page level
            // to the effective font size), so body text and headers scale accordingly.
            column.Item().Markdown(markdown);
        }
    }

    /// <summary>
    /// Renders a text segment containing inline LaTeX using the QuestPDF Text API.
    /// Per MaterialConversionSpecification §3A(6)(a): inline LaTeX is rendered within
    /// the text flow using text.Element() for inline image embedding.
    /// Per §3A(6)(a): markdown formatting is not preserved in such paragraphs.
    /// Per §3A(2): body text uses the effective font size.
    /// </summary>
    private void RenderTextWithInlineLatex(ColumnDescriptor column, string text, Regex inlineLatexPattern, EffectivePdfSettings effectiveSettings)
    {
        var bodyFontSize = effectiveSettings.BodyFontSizePt;

        column.Item().Text(textDescriptor =>
        {
            textDescriptor.DefaultTextStyle(x => x.FontSize(bodyFontSize).LineHeight(1.5f));

            var matches = inlineLatexPattern.Matches(text);
            var lastIndex = 0;

            foreach (Match match in matches)
            {
                // Render text before this LaTeX expression
                if (match.Index > lastIndex)
                {
                    var textSegment = text.Substring(lastIndex, match.Index - lastIndex);
                    textDescriptor.Span(UnescapeMarkdown(textSegment));
                }

                // Render inline LaTeX as an embedded image
                var latex = match.Groups[1].Value.Trim();
                var imageBytes = _latexRenderer.RenderToImage(latex, displayMode: false, fontSize: 14f);
                if (imageBytes != null)
                {
                    var (ptW, ptH) = GetImagePointDimensions(imageBytes);
                    textDescriptor.Element().Width(ptW).Height(ptH).Image(imageBytes);
                }
                else
                {
                    // Fallback per §6(4): display raw LaTeX source as plain text
                    textDescriptor.Span($"${latex}$");
                }

                lastIndex = match.Index + match.Length;
            }

            // Render remaining text after the last LaTeX expression
            if (lastIndex < text.Length)
            {
                var remaining = text.Substring(lastIndex);
                textDescriptor.Span(UnescapeMarkdown(remaining));
            }
        });
    }

    /// <summary>
    /// Removes markdown backslash escapes from text.
    /// When bypassing QuestPDF.Markdown, raw escapes like \[ appear literally.
    /// This restores the intended characters.
    /// </summary>
    private static string UnescapeMarkdown(string text)
    {
        // Markdown escape: backslash followed by a punctuation character
        return Regex.Replace(text, @"\\([\[\]\\*_{}()#+\-.!|`~>])", "$1");
    }

    /// <summary>
    /// Renders block LaTeX as a centred image at natural size.
    /// Per MaterialConversionSpecification §3A(6)(b).
    /// Falls back to raw text per §6(4) on rendering failure.
    /// </summary>
    private void RenderBlockLatex(ColumnDescriptor column, string latex)
    {
        var imageBytes = _latexRenderer.RenderToImage(latex, displayMode: true, fontSize: 24f);
        if (imageBytes != null)
        {
            var (ptW, ptH) = GetImagePointDimensions(imageBytes);
            column.Item().AlignCenter().Width(ptW).Height(ptH).Image(imageBytes);
        }
        else
        {
            // Fallback per §6(4): display raw LaTeX source as plain text
            column.Item().AlignCenter().Text($"$${latex}$$").FontSize(12);
        }
    }

    /// <summary>
    /// Reads pixel dimensions from a PNG's IHDR chunk and converts to PDF points.
    /// Assumes 96 DPI (1px = 0.75pt). Caps width to available content area (~170mm).
    /// </summary>
    private static (float width, float height) GetImagePointDimensions(byte[] pngBytes)
    {
        // Validate PNG: must be at least 24 bytes (8-byte signature + 16-byte IHDR)
        // and start with the PNG magic signature
        if (pngBytes.Length < 24 ||
            pngBytes[0] != 0x89 || pngBytes[1] != 0x50 ||
            pngBytes[2] != 0x4E || pngBytes[3] != 0x47)
        {
            // Fallback: return a small default size for malformed data
            return (50f, 14f);
        }

        // PNG IHDR: bytes 16-19 = width, 20-23 = height (big-endian uint32)
        int pxW = (pngBytes[16] << 24) | (pngBytes[17] << 16) | (pngBytes[18] << 8) | pngBytes[19];
        int pxH = (pngBytes[20] << 24) | (pngBytes[21] << 16) | (pngBytes[22] << 8) | pngBytes[23];

        // Convert pixels to PDF points at 96 DPI (1px = 72/96 = 0.75pt)
        float ptW = pxW * 0.75f;
        float ptH = pxH * 0.75f;

        // Cap to available content width: A4 (210mm) - margins (20mm + 20mm) = 170mm ≈ 481pt
        const float maxContentWidth = 481f;
        if (ptW > maxContentWidth)
        {
            float scale = maxContentWidth / ptW;
            ptW = maxContentWidth;
            ptH *= scale;
        }

        return (ptW, ptH);
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
            // Per §3B(2): Invalid references are not rendered
            return;
        }

        var filePath = _fileService.GetAttachmentFilePath(attachment.Id, attachment.FileExtension);
        if (!_fileService.FileExists(filePath))
        {
            // Per §3B(2): Invalid references are not rendered
            return;
        }

        var ext = attachment.FileExtension.ToLowerInvariant();
        if (ext is "png" or "jpg" or "jpeg" or "gif" or "bmp" or "webp")
        {
            // Per §3B(1)(a-b): Render image scaled to fit within page width
            try
            {
                column.Item().Image(filePath).FitWidth().UseOriginalImage();
            }
            catch
            {
                // Per §3B(2): Invalid references are not rendered
            }
        }
        // Per §3B(2): Unsupported formats are not rendered
    }

    /// <summary>
    /// Renders a question block.
    /// Per MaterialConversionSpecification §4.
    /// </summary>
    private static void RenderQuestion(
        ColumnDescriptor column,
        string questionId,
        int questionNumber,
        Dictionary<string, QuestionEntity> questionLookup,
        EffectivePdfSettings effectiveSettings)
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
                    var marksFontSize = effectiveSettings.BodyFontSizePt - 2; // Marks slightly smaller than body text
                    row.ConstantItem(80).AlignRight().Text($"[{question.MaxScore.Value}]").FontSize(marksFontSize).Bold();
                }
            });

            // Render question type-specific content
            if (question.QuestionType == QuestionType.MULTIPLE_CHOICE)
            {
                RenderMultipleChoiceQuestion(questionColumn, question);
            }
            else
            {
                RenderWrittenAnswerQuestion(questionColumn, question, effectiveSettings);
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
    /// Renders a written answer question with a patterned answer area.
    /// Per MaterialConversionSpecification §4(4) and §2A.
    /// Large answer areas are split into page-safe chunks so that
    /// the non-breakable Canvas element never exceeds a single page.
    /// </summary>
    private static void RenderWrittenAnswerQuestion(
        ColumnDescriptor questionColumn,
        QuestionEntity question,
        EffectivePdfSettings effectiveSettings)
    {
        // Calculate line count per §4(4)(b)
        int lineCount;
        if (question.MaxScore.HasValue)
        {
            var score = question.MaxScore.Value;
            if (score <= 2)
            {
                // §4(4)(b)(i): MaxScore 1 or 2 → 3 lines
                lineCount = 3;
            }
            else if (score <= 5)
            {
                // §4(4)(b)(ii): MaxScore 3 to 5 → 5 lines
                lineCount = 5;
            }
            else
            {
                // §4(4)(b)(iii): MaxScore > 5 → 2 × MaxScore lines
                lineCount = score * 2;
            }
        }
        else
        {
            lineCount = 3; // Default when no MaxScore
        }

        // §4(4)(b)(iv): answer area height = lineCount × effective_spacing_mm
        var spacingMm = effectiveSettings.LineSpacingMm;

        // §4(4)(b)(v): answer area spans content width, filled with effective line pattern
        var linePattern = effectiveSettings.LinePatternType;

        // Canvas elements are non-breakable in QuestPDF, so large answer areas
        // must be split into small chunks that can flow across page boundaries.
        // Max 2 lines per chunk keeps each canvas at 12–28 mm, small enough to
        // render on the same page as the question text in almost all cases.
        const int maxLinesPerChunk = 2;
        var totalHeightPt = lineCount * spacingMm * 2.8346f;
        var remainingLines = lineCount;
        var isFirst = true;
        var verticalOffsetPt = 0f;

        while (remainingLines > 0)
        {
            var chunkLines = Math.Min(remainingLines, maxLinesPerChunk);
            var chunkHeightMm = chunkLines * spacingMm;
            var capturedOffsetPt = verticalOffsetPt;

            questionColumn.Item().PaddingTop(isFirst ? 5 : 0)
                .Height(chunkHeightMm, Unit.Millimetre)
                .Canvas((canvas, size) =>
                {
                    DrawAnswerAreaPattern(canvas, size, spacingMm, linePattern,
                        capturedOffsetPt, totalHeightPt);
                });

            verticalOffsetPt += chunkHeightMm * 2.8346f;
            remainingLines -= chunkLines;
            isFirst = false;
        }
    }

    /// <summary>
    /// Draws the line pattern for an answer area chunk using SkiaSharp.
    /// Per MaterialConversionSpecification §2A.
    /// <paramref name="verticalOffsetPt"/> and <paramref name="totalAreaHeightPt"/>
    /// allow ISOMETRIC diagonal lines to connect seamlessly across chunks.
    /// </summary>
    private static void DrawAnswerAreaPattern(
        SKCanvas canvas, Size size, float spacingMm, LinePatternType linePattern,
        float verticalOffsetPt, float totalAreaHeightPt)
    {
        var areaWidth = size.Width;
        var areaHeight = size.Height;
        var spacingPt = spacingMm * 2.8346f;

        using var paint = new SKPaint
        {
            Color = new SKColor(158, 158, 158),
            StrokeWidth = 1f,
            Style = SKPaintStyle.Stroke,
            IsAntialias = true
        };

        switch (linePattern)
        {
            case LinePatternType.RULED:
                // §2A(1)(a): Horizontal lines at effective spacing
                for (float y = 0; y <= areaHeight; y += spacingPt)
                {
                    canvas.DrawLine(0, y, areaWidth, y, paint);
                }
                break;

            case LinePatternType.SQUARE:
                // §2A(1)(b): Horizontal + vertical lines forming a grid
                for (float y = 0; y <= areaHeight; y += spacingPt)
                {
                    canvas.DrawLine(0, y, areaWidth, y, paint);
                }
                for (float x = 0; x <= areaWidth; x += spacingPt)
                {
                    canvas.DrawLine(x, 0, x, areaHeight, paint);
                }
                break;

            case LinePatternType.ISOMETRIC:
                // §2A(1)(c): Lines at 0°, 60°, 120° forming equilateral triangle grid
                for (float y = 0; y <= areaHeight; y += spacingPt)
                {
                    canvas.DrawLine(0, y, areaWidth, y, paint);
                }

                // Diagonal lines use global coordinates (via verticalOffsetPt)
                // so they connect seamlessly across adjacent chunks.
                // Clip to canvas bounds so diagonals don't extend beyond horizontal lines.
                canvas.Save();
                canvas.ClipRect(new SKRect(0, 0, areaWidth, areaHeight));

                var tan60 = MathF.Tan(MathF.PI / 3f);
                var stepX60 = spacingPt / MathF.Sin(MathF.PI / 3f);

                // 60° lines (bottom-left to top-right in global area)
                for (float offset = -totalAreaHeightPt; offset <= areaWidth + totalAreaHeightPt; offset += stepX60)
                {
                    float xTop = offset + (totalAreaHeightPt - verticalOffsetPt) / tan60;
                    float xBottom = offset + (totalAreaHeightPt - verticalOffsetPt - areaHeight) / tan60;
                    canvas.DrawLine(xBottom, areaHeight, xTop, 0, paint);
                }

                // 120° lines (top-left to bottom-right in global area)
                for (float offset = -totalAreaHeightPt; offset <= areaWidth + totalAreaHeightPt; offset += stepX60)
                {
                    float xTop = offset - verticalOffsetPt / tan60;
                    float xBottom = offset - (verticalOffsetPt + areaHeight) / tan60;
                    canvas.DrawLine(xTop, 0, xBottom, areaHeight, paint);
                }

                canvas.Restore();
                break;

            case LinePatternType.NONE:
                // §2A(1)(d): Blank space, no lines drawn
                break;
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

/// <summary>
/// Resolved effective PDF export settings for a material.
/// Per MaterialConversionSpecification §1(5)(h): material.{Field} ?? globalDefault.{Field}.
/// </summary>
public record EffectivePdfSettings(
    LinePatternType LinePatternType,
    LineSpacingPreset LineSpacingPreset,
    FontSizePreset FontSizePreset)
{
    /// <summary>
    /// Effective body font size in points. Per §1(5)(g).
    /// </summary>
    public float BodyFontSizePt => FontSizePreset switch
    {
        FontSizePreset.SMALL => 10f,
        FontSizePreset.MEDIUM => 12f,
        FontSizePreset.LARGE => 14f,
        FontSizePreset.EXTRA_LARGE => 16f,
        _ => 12f
    };

    /// <summary>
    /// Effective line spacing in millimetres. Per §1(5)(f).
    /// </summary>
    public float LineSpacingMm => LineSpacingPreset switch
    {
        LineSpacingPreset.SMALL => 6f,
        LineSpacingPreset.MEDIUM => 8f,
        LineSpacingPreset.LARGE => 10f,
        LineSpacingPreset.EXTRA_LARGE => 14f,
        _ => 8f
    };
}
