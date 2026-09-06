package com.albertonavas.missionbriefing.server.web;

import com.albertonavas.missionbriefing.server.extraction.EmergencyExtractionService;
import com.albertonavas.missionbriefing.server.extraction.NearestExtractionResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmergencyExtractionController {

    private final EmergencyExtractionService emergencyExtractionService;

    public EmergencyExtractionController(EmergencyExtractionService emergencyExtractionService) {
        this.emergencyExtractionService = emergencyExtractionService;
    }

    @GetMapping("/api/extraction-points/nearest-route")
    public NearestExtractionResult nearestRoute(
            @RequestParam("lat") double lat, @RequestParam("lon") double lon) {
        return emergencyExtractionService.routeToNearestExtraction(lat, lon);
    }
}
