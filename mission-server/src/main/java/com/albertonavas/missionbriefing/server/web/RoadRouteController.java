package com.albertonavas.missionbriefing.server.web;

import com.albertonavas.missionbriefing.server.route.GeoPoint;
import com.albertonavas.missionbriefing.server.route.RoadRouteService;
import com.albertonavas.missionbriefing.server.route.RoadRouteUnavailableException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/missions")
public class RoadRouteController {

    private final RoadRouteService roadRouteService;

    public RoadRouteController(RoadRouteService roadRouteService) {
        this.roadRouteService = roadRouteService;
    }

    @GetMapping("/{id}/road-route")
    public ResponseEntity<List<GeoPoint>> roadRoute(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(roadRouteService.buildRoadRoute(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (RoadRouteUnavailableException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
