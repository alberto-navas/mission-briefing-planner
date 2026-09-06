package com.albertonavas.missionbriefing.clientfx;

import static org.assertj.core.api.Assertions.assertThat;

import com.albertonavas.missionbriefing.clientfx.dto.MissionDto;
import com.albertonavas.missionbriefing.clientfx.dto.PhaseDto;
import com.albertonavas.missionbriefing.clientfx.dto.ResourceDto;
import com.albertonavas.missionbriefing.clientfx.dto.WaypointDto;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BriefingPdfExporterTest {

    @TempDir
    File tempDir;

    @Test
    void generatesAReadablePdfWithMissionData() throws Exception {
        MissionDto mission = new MissionDto(
                1L, "Escolta de prueba", "ESCORT", "DRAFT",
                Instant.parse("2026-09-10T08:00:00Z"), Instant.parse("2026-09-10T10:00:00Z"),
                "Descripción de prueba",
                List.of(new WaypointDto(1L, 1, 36.15, -5.35, "OBSERVE", "Punto norte")),
                List.of(new PhaseDto(1L, "Transito", 0, 30, "Salida")),
                List.of(new ResourceDto(1L, "Escolta 1", "PERSONNEL_TEAM", "BRAVO-1", false)));
        BufferedImage snapshot = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);

        File output = new File(tempDir, "briefing.pdf");
        BriefingPdfExporter.export(output, mission, snapshot);

        assertThat(output).exists();
        assertThat(output.length()).isGreaterThan(0);

        try (PDDocument document = PDDocument.load(output)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Escolta de prueba");
            assertThat(text).contains("BRAVO-1");
            assertThat(text).contains("Transito");
        }
    }

    @Test
    void doesNotFailOnCharactersOutsideLatin1() throws Exception {
        MissionDto mission = new MissionDto(
                2L, "Misión con emoji 🚨", "ESCORT", "DRAFT",
                Instant.parse("2026-09-10T08:00:00Z"), Instant.parse("2026-09-10T10:00:00Z"),
                "Notas con acentos: áéíóú, ñ, ¿?",
                List.of(), List.of(), List.of());

        File output = new File(tempDir, "briefing-emoji.pdf");

        BriefingPdfExporter.export(output, mission, null);

        assertThat(output).exists();
    }
}
