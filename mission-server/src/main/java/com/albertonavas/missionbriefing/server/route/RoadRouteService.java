package com.albertonavas.missionbriefing.server.route;

import com.albertonavas.missionbriefing.model.Mission;
import com.albertonavas.missionbriefing.model.Waypoint;
import com.albertonavas.missionbriefing.server.service.MissionService;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * Construye la ruta real por carretera de una mision, tramo a tramo entre waypoints
 * consecutivos, concatenando los tramos de OSRM sin duplicar el punto de union.
 */
@Service
public class RoadRouteService {

    private final MissionService missionService;
    private final OsrmRouteClient osrmRouteClient;

    public RoadRouteService(MissionService missionService, OsrmRouteClient osrmRouteClient) {
        this.missionService = missionService;
        this.osrmRouteClient = osrmRouteClient;
    }

    public List<GeoPoint> buildRoadRoute(Long missionId) {
        Mission mission = missionService.findMission(missionId)
                .orElseThrow(() -> new NoSuchElementException("Mission not found: " + missionId));

        List<Waypoint> waypoints = mission.getWaypoints();
        if (waypoints.size() < 2) {
            return waypoints.stream().map(w -> new GeoPoint(w.getLatitude(), w.getLongitude())).toList();
        }

        List<GeoPoint> route = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Waypoint from = waypoints.get(i);
            Waypoint to = waypoints.get(i + 1);
            List<GeoPoint> leg = osrmRouteClient.fetchRoute(
                    from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());

            boolean dropFirstPoint = i > 0 && !leg.isEmpty();
            route.addAll(dropFirstPoint ? leg.subList(1, leg.size()) : leg);
        }
        return route;
    }
}
