package com.albertonavas.missionbriefing.server.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.albertonavas.missionbriefing.model.Mission;
import com.albertonavas.missionbriefing.model.MissionType;
import com.albertonavas.missionbriefing.model.TaskType;
import com.albertonavas.missionbriefing.model.Waypoint;
import com.albertonavas.missionbriefing.server.service.MissionService;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * No llama a OSRM de verdad (evita depender de red/servicio externo en CI): el cliente
 * OSRM esta mockeado, esta prueba solo verifica que RoadRouteService concatena bien los
 * tramos. La llamada real se verifico manualmente con el servidor en marcha.
 */
@ExtendWith(MockitoExtension.class)
class RoadRouteServiceTest {

    @Mock
    private MissionService missionService;

    @Mock
    private OsrmRouteClient osrmRouteClient;

    @Test
    void concatenatesLegsWithoutDuplicatingJunctionPoints() {
        Mission mission = new Mission("Escolta urbana", MissionType.ESCORT, Instant.now(), Instant.now(), "");
        mission.addWaypoint(new Waypoint(1, 36.14, -5.45, TaskType.TRANSIT, ""));
        mission.addWaypoint(new Waypoint(2, 36.13, -5.44, TaskType.TRANSIT, ""));
        mission.addWaypoint(new Waypoint(3, 36.12, -5.43, TaskType.TRANSIT, ""));
        when(missionService.findMission(1L)).thenReturn(Optional.of(mission));

        when(osrmRouteClient.fetchRoute(36.14, -5.45, 36.13, -5.44)).thenReturn(List.of(
                new GeoPoint(36.14, -5.45), new GeoPoint(36.135, -5.445), new GeoPoint(36.13, -5.44)));
        when(osrmRouteClient.fetchRoute(36.13, -5.44, 36.12, -5.43)).thenReturn(List.of(
                new GeoPoint(36.13, -5.44), new GeoPoint(36.125, -5.435), new GeoPoint(36.12, -5.43)));

        RoadRouteService service = new RoadRouteService(missionService, osrmRouteClient);
        List<GeoPoint> route = service.buildRoadRoute(1L);

        assertThat(route).hasSize(5);
        assertThat(route.get(0)).isEqualTo(new GeoPoint(36.14, -5.45));
        assertThat(route.get(2)).isEqualTo(new GeoPoint(36.13, -5.44));
        assertThat(route.get(4)).isEqualTo(new GeoPoint(36.12, -5.43));
    }

    @Test
    void missingMissionThrowsNoSuchElement() {
        when(missionService.findMission(99L)).thenReturn(Optional.empty());
        RoadRouteService service = new RoadRouteService(missionService, osrmRouteClient);

        assertThatThrownBy(() -> service.buildRoadRoute(99L)).isInstanceOf(NoSuchElementException.class);
    }
}
