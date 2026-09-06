package com.albertonavas.missionbriefing.clientfx;

import com.albertonavas.missionbriefing.clientfx.dto.MissionDto;
import com.albertonavas.missionbriefing.clientfx.dto.PhaseDto;
import com.albertonavas.missionbriefing.clientfx.dto.ResourceDto;
import com.albertonavas.missionbriefing.clientfx.dto.WaypointDto;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Genera el briefing de una mision como un PDF real, imprimible: portada con los datos
 * de la mision, una imagen del mapa tal como se ve en el cliente, cronograma, ruta y
 * escoltas asignados. Usa las fuentes base de PDFBox (sin fuentes externas que
 * empaquetar); los caracteres fuera de Latin-1 (p.ej. un emoji colado en unas notas) se
 * sustituyen por '?' en vez de reventar la generacion del documento.
 */
final class BriefingPdfExporter {

    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MAX_IMAGE_HEIGHT = 260;

    private BriefingPdfExporter() {
    }

    static void export(File file, MissionDto mission, BufferedImage mapSnapshot) throws IOException {
        try (PDDocument document = new PDDocument()) {
            Cursor cursor = new Cursor(document);

            cursor.heading(mission.name());
            cursor.line("Tipo: %s    Estado: %s".formatted(mission.type(), mission.status()));
            cursor.line("Inicio: %s".formatted(mission.startTime()));
            cursor.line("Fin: %s".formatted(mission.endTime()));
            if (mission.description() != null && !mission.description().isBlank()) {
                cursor.gap();
                cursor.paragraph(mission.description());
            }

            if (mapSnapshot != null) {
                cursor.gap();
                cursor.image(mapSnapshot);
            }

            cursor.sectionHeading("Cronograma");
            if (mission.phases().isEmpty()) {
                cursor.line("(sin fases definidas)");
            }
            for (PhaseDto p : mission.phases()) {
                String notes = p.notes() != null && !p.notes().isBlank() ? " (%s)".formatted(p.notes()) : "";
                cursor.line("- %s: minuto %d a %d%s".formatted(p.name(), p.startOffsetMinutes(), p.endOffsetMinutes(), notes));
            }

            cursor.sectionHeading("Ruta");
            if (mission.waypoints().isEmpty()) {
                cursor.line("(sin waypoints definidos)");
            }
            for (WaypointDto w : mission.waypoints()) {
                String notes = w.notes() != null && !w.notes().isBlank() ? ": %s".formatted(w.notes()) : "";
                cursor.line(String.format(Locale.ROOT, "#%d %s (%.5f, %.5f)%s",
                        w.sequenceOrder(), w.taskType(), w.latitude(), w.longitude(), notes));
            }

            cursor.sectionHeading("Escoltas asignados");
            if (mission.resources().isEmpty()) {
                cursor.line("(sin escoltas asignados)");
            }
            for (ResourceDto r : mission.resources()) {
                String callSign = r.callSign() != null && !r.callSign().isBlank() ? r.callSign() : "-";
                cursor.line("%s - %s (%s)".formatted(callSign, r.name(), r.type()));
            }

            cursor.close();
            document.save(file);
        }
    }

    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sanitized.append(c <= 0xFF ? c : '?');
        }
        return sanitized.toString();
    }

    private static List<String> wrap(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        for (String paragraphLine : text.split("\n", -1)) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraphLine.split(" ")) {
                if (current.length() > 0 && current.length() + word.length() + 1 > maxChars) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(word);
            }
            lines.add(current.toString());
        }
        return lines;
    }

    /** Escribe texto pagina a pagina, abriendo una pagina nueva cuando no queda espacio vertical. */
    private static final class Cursor {
        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        Cursor(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN) {
                newPage();
            }
        }

        void heading(String text) throws IOException {
            ensureSpace(26);
            writeLine(text, PDType1Font.HELVETICA_BOLD, 18);
            y -= 6;
        }

        void sectionHeading(String text) throws IOException {
            gap();
            ensureSpace(20);
            writeLine(text, PDType1Font.HELVETICA_BOLD, 13);
        }

        void line(String text) throws IOException {
            ensureSpace(16);
            writeLine(text, PDType1Font.HELVETICA, 11);
        }

        void paragraph(String text) throws IOException {
            for (String wrapped : wrap(text, 95)) {
                line(wrapped);
            }
        }

        void gap() {
            y -= 8;
        }

        void image(BufferedImage bufferedImage) throws IOException {
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);
            float maxWidth = PAGE_WIDTH - 2 * MARGIN;
            float scale = maxWidth / bufferedImage.getWidth();
            float drawWidth = maxWidth;
            float drawHeight = bufferedImage.getHeight() * scale;
            if (drawHeight > MAX_IMAGE_HEIGHT) {
                drawHeight = MAX_IMAGE_HEIGHT;
                drawWidth = bufferedImage.getWidth() * (MAX_IMAGE_HEIGHT / bufferedImage.getHeight());
            }
            ensureSpace(drawHeight + 10);
            y -= drawHeight;
            stream.drawImage(pdImage, MARGIN, y, drawWidth, drawHeight);
            y -= 10;
        }

        private void writeLine(String text, PDFont font, float size) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(sanitize(text));
            stream.endText();
            y -= 16f * (size / 11f);
        }

        void close() throws IOException {
            stream.close();
        }
    }
}
