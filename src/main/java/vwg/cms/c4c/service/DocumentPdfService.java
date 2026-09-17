package vwg.cms.c4c.service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import vwg.cms.c4c.dto.ApprovalResponse;
import vwg.cms.c4c.dto.DocumentResponse;
import vwg.cms.c4c.entity.DocumentVersion;
import vwg.cms.c4c.model.ApprovalAction;
import vwg.cms.c4c.repository.DocumentVersionRepository;

@Service
@RequiredArgsConstructor
public class DocumentPdfService {

    private static final float MARGIN = 54;
    private static final float LINE_HEIGHT = 15;
    private static final float BODY_FONT_SIZE = 10;
    private static final float USABLE_WIDTH = 504;
    private static final DateTimeFormatter SIGNED_AT_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm z")
            .withZone(ZoneId.of("UTC"));
    private final DocumentVersionRepository documentVersionRepository;

    public byte[] render(Long documentId, DocumentResponse document) throws IOException {
        DocumentVersion sourceVersion = documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream().findFirst().orElse(null);
        if (sourceVersion != null && sourceVersion.getSourceFile() != null) {
            if ("application/pdf".equalsIgnoreCase(sourceVersion.getSourceContentType())) {
                return sourceVersion.getSourceFile();
            }
            return convertOfficeFileToPdf(sourceVersion);
        }
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            List<String> lines = wrap(sanitize(document.versions().isEmpty() ? "" : document.versions().getFirst().content()));
            int lineIndex = 0;
            boolean firstPage = true;
            while (lineIndex < lines.size() || firstPage) {
                PDPage page = new PDPage();
                pdf.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                    float y = page.getMediaBox().getHeight() - MARGIN;
                    if (firstPage) {
                        y = writeTitle(content, y, document);
                        firstPage = false;
                    }
                    lineIndex = writeLines(content, y, lines, lineIndex);
                }
            }
            ApprovalResponse approval = document.approvals().stream()
                    .filter(item -> item.action() == ApprovalAction.APPROVED && item.signature() != null)
                    .findFirst().orElse(null);
            if (approval != null) {
                addSignatureCertificate(pdf, approval);
            }
            pdf.save(output);
            return output.toByteArray();
        }
    }

    private byte[] convertOfficeFileToPdf(DocumentVersion version) throws IOException {
        String filename = version.getSourceFilename() == null ? "document.docx" : version.getSourceFilename();
        Path workingDirectory = Files.createTempDirectory("securesync-pdf-");
        try {
            Path source = workingDirectory.resolve(filename.replaceAll("[^A-Za-z0-9._-]", "_"));
            Files.write(source, version.getSourceFile());
            Process process = new ProcessBuilder(
                    "libreoffice", "--headless", "--convert-to", "pdf", "--outdir", workingDirectory.toString(), source.toString()
            ).redirectErrorStream(true).start();
            if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IOException("Unable to convert the uploaded file to PDF");
            }
            String pdfName = source.getFileName().toString().replaceFirst("\\.[^.]+$", ".pdf");
            Path pdf = workingDirectory.resolve(pdfName);
            if (!Files.exists(pdf)) {
                throw new IOException("The uploaded file could not be converted to PDF");
            }
            return Files.readAllBytes(pdf);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("PDF conversion was interrupted", exception);
        } finally {
            try (var paths = Files.walk(workingDirectory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    private float writeTitle(PDPageContentStream content, float y, DocumentResponse document) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
        content.newLineAtOffset(MARGIN, y);
        content.showText(sanitize(document.title()));
        content.endText();
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
        content.newLineAtOffset(MARGIN, y - 20);
        content.showText("Label: " + sanitize(document.category()) + "    Version: " + document.currentVersion());
        content.endText();
        return y - 42;
    }

    private int writeLines(PDPageContentStream content, float y, List<String> lines, int lineIndex) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        content.setLeading(LINE_HEIGHT);
        content.newLineAtOffset(MARGIN, y);
        while (lineIndex < lines.size()) {
            if (y < MARGIN + LINE_HEIGHT) {
                content.endText();
                return lineIndex;
            }
            content.showText(lines.get(lineIndex));
            content.newLine();
            y -= LINE_HEIGHT;
            lineIndex++;
        }
        content.endText();
        return lineIndex;
    }

    private void addSignatureCertificate(PDDocument pdf, ApprovalResponse approval) throws IOException {
        PDPage page = new PDPage();
        pdf.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
            float y = page.getMediaBox().getHeight() - 130;
            content.addRect(MARGIN, y - 90, USABLE_WIDTH, 100);
            content.stroke();
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            content.newLineAtOffset(MARGIN + 16, y - 18);
            content.showText("DIGITAL APPROVAL SIGNATURE");
            content.newLineAtOffset(0, -26);
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
            content.showText("Signed by: " + sanitize(approval.signature()));
            content.newLineAtOffset(0, -18);
            content.showText("Reviewer: " + sanitize(approval.actorUsername()));
            content.newLineAtOffset(0, -18);
            content.showText("Approved at: " + SIGNED_AT_FORMAT.format(approval.createdAt()));
            content.endText();
        }
    }

    private List<String> wrap(String text) throws IOException {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && font.getStringWidth(candidate) / 1000 * BODY_FONT_SIZE > USABLE_WIDTH) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (!line.isEmpty()) {
                    line.append(' ');
                }
                line.append(word);
            }
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private String sanitize(String value) {
        return (value == null ? "" : value)
                .replaceAll("[\\p{Z}\\t]+", " ")
                .replace('“', '"')
                .replace('”', '"')
                .replace('‘', '\'')
                .replace('’', '\'')
                .replace('–', '-')
                .replace('—', '-')
                .replace('…', '.')
                .replaceAll("[^\\x20-\\x7E]", "");
    }
}