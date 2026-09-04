package com.albertonavas.missionbriefing.server.web;

import com.albertonavas.missionbriefing.model.Mission;
import com.albertonavas.missionbriefing.server.service.MissionService;
import com.albertonavas.missionbriefing.server.web.dto.CreateMissionRequest;
import com.albertonavas.missionbriefing.server.web.dto.MissionResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping
    public List<MissionResponse> listMissions() {
        return missionService.listMissions().stream().map(MissionResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MissionResponse> getMission(@PathVariable Long id) {
        return missionService.findMission(id)
                .map(MissionResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MissionResponse> createMission(@Valid @RequestBody CreateMissionRequest request) {
        Mission created = missionService.createMission(request);
        return ResponseEntity.created(URI.create("/api/missions/" + created.getId()))
                .body(MissionResponse.from(created));
    }
}
