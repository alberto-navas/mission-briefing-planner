package com.albertonavas.missionbriefing.server.web;

import com.albertonavas.missionbriefing.server.search.BriefingDocument;
import com.albertonavas.missionbriefing.server.service.BriefingService;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BriefingController {

    private final BriefingService briefingService;

    public BriefingController(BriefingService briefingService) {
        this.briefingService = briefingService;
    }

    @PostMapping("/missions/{id}/briefing")
    public ResponseEntity<BriefingDocument> generateBriefing(@PathVariable("id") Long missionId) {
        try {
            return ResponseEntity.ok(briefingService.generateBriefing(missionId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/briefings/search")
    public List<BriefingDocument> search(@RequestParam("q") String query) {
        return briefingService.search(query);
    }
}
