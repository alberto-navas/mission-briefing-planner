package com.albertonavas.missionbriefing.server.service;

import com.albertonavas.missionbriefing.model.Mission;
import com.albertonavas.missionbriefing.model.MissionPhase;
import com.albertonavas.missionbriefing.model.Resource;
import com.albertonavas.missionbriefing.model.Waypoint;
import com.albertonavas.missionbriefing.server.search.BriefingDocument;
import com.albertonavas.missionbriefing.server.search.BriefingSearchRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/** Genera el texto de briefing de una mision a partir de su ruta, cronograma y recursos. */
@Service
public class BriefingService {

    private final MissionService missionService;
    private final BriefingSearchRepository briefingSearchRepository;

    public BriefingService(MissionService missionService, BriefingSearchRepository briefingSearchRepository) {
        this.missionService = missionService;
        this.briefingSearchRepository = briefingSearchRepository;
    }

    public BriefingDocument generateBriefing(Long missionId) {
        Mission mission = missionService.findMission(missionId)
                .orElseThrow(() -> new NoSuchElementException("Mission not found: " + missionId));

        String summary = buildSummary(mission);
        BriefingDocument document = new BriefingDocument(mission.getId(), mission.getName(), summary, Instant.now());
        return briefingSearchRepository.save(document);
    }

    public List<BriefingDocument> search(String query) {
        return briefingSearchRepository.searchBySummary(query);
    }

    private String buildSummary(Mission mission) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mision: ").append(mission.getName())
                .append(" (").append(mission.getType()).append(")\n");
        if (mission.getDescription() != null && !mission.getDescription().isBlank()) {
            sb.append(mission.getDescription()).append("\n");
        }

        sb.append("Ruta:\n");
        for (Waypoint w : mission.getWaypoints()) {
            sb.append(" - #%d %s en (%.5f, %.5f)%s%n".formatted(
                    w.getSequenceOrder(), w.getTaskType(), w.getLatitude(), w.getLongitude(),
                    w.getNotes() != null && !w.getNotes().isBlank() ? ": " + w.getNotes() : ""));
        }

        sb.append("Cronograma:\n");
        for (MissionPhase p : mission.getPhases()) {
            sb.append(" - %s: min %d a %d%s%n".formatted(
                    p.getName(), p.getStartOffsetMinutes(), p.getEndOffsetMinutes(),
                    p.getNotes() != null && !p.getNotes().isBlank() ? " (" + p.getNotes() + ")" : ""));
        }

        sb.append("Recursos asignados:\n");
        for (Resource r : mission.getAssignedResources()) {
            sb.append(" - %s [%s] %s%n".formatted(r.getName(), r.getType(), r.getCallSign()));
        }

        return sb.toString();
    }
}
