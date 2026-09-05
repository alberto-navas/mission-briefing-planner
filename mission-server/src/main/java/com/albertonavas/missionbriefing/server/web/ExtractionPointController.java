package com.albertonavas.missionbriefing.server.web;

import com.albertonavas.missionbriefing.server.extraction.ExtractionPoint;
import com.albertonavas.missionbriefing.server.extraction.ExtractionPointCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/extraction-points")
public class ExtractionPointController {

    private final ExtractionPointCatalog catalog;

    public ExtractionPointController(ExtractionPointCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<ExtractionPoint> listExtractionPoints() {
        return catalog.all();
    }
}
