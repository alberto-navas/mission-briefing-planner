package com.albertonavas.missionbriefing.server.service;

import com.albertonavas.missionbriefing.model.Mission;
import com.albertonavas.missionbriefing.model.MissionPhase;
import com.albertonavas.missionbriefing.model.Resource;
import com.albertonavas.missionbriefing.model.Waypoint;
import com.albertonavas.missionbriefing.server.repository.MissionRepository;
import com.albertonavas.missionbriefing.server.web.dto.CreateMissionRequest;
import com.albertonavas.missionbriefing.server.web.dto.CreatePhaseRequest;
import com.albertonavas.missionbriefing.server.web.dto.CreateResourceRequest;
import com.albertonavas.missionbriefing.server.web.dto.CreateWaypointRequest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissionService {

    private final MissionRepository missionRepository;

    public MissionService(MissionRepository missionRepository) {
        this.missionRepository = missionRepository;
    }

    public List<Mission> listMissions() {
        return missionRepository.findAll();
    }

    public Optional<Mission> findMission(Long id) {
        return missionRepository.findById(id);
    }

    public Mission createMission(CreateMissionRequest request) {
        Mission mission = new Mission(
                request.name(), request.type(), request.startTime(), request.endTime(), request.description());
        populateChildren(mission, request);
        return missionRepository.save(mission);
    }

    /**
     * Sustituye por completo nombre/tipo/fechas/descripcion y todos los waypoints, fases
     * y recursos de una mision existente (no hay actualizacion parcial: mas simple y sin
     * ambiguedad sobre que pasa con los hijos no mencionados).
     */
    @Transactional
    public Mission updateMission(Long id, CreateMissionRequest request) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Mission not found: " + id));

        mission.setName(request.name());
        mission.setType(request.type());
        mission.setStartTime(request.startTime());
        mission.setEndTime(request.endTime());
        mission.setDescription(request.description());

        mission.getWaypoints().clear();
        mission.getPhases().clear();
        mission.getAssignedResources().clear();
        populateChildren(mission, request);

        return mission;
    }

    @Transactional
    public void deleteMission(Long id) {
        if (!missionRepository.existsById(id)) {
            throw new NoSuchElementException("Mission not found: " + id);
        }
        missionRepository.deleteById(id);
    }

    private void populateChildren(Mission mission, CreateMissionRequest request) {
        if (request.waypoints() != null) {
            for (CreateWaypointRequest w : request.waypoints()) {
                mission.addWaypoint(new Waypoint(w.sequenceOrder(), w.latitude(), w.longitude(), w.taskType(), w.notes()));
            }
        }
        if (request.phases() != null) {
            for (CreatePhaseRequest p : request.phases()) {
                mission.addPhase(new MissionPhase(p.name(), p.startOffsetMinutes(), p.endOffsetMinutes(), p.notes()));
            }
        }
        if (request.resources() != null) {
            for (CreateResourceRequest r : request.resources()) {
                mission.addResource(new Resource(r.name(), r.type(), r.callSign()));
            }
        }
    }
}
