package com.albertonavas.missionbriefing.server.web;

import com.albertonavas.missionbriefing.server.risk.RiskZone;
import com.albertonavas.missionbriefing.server.risk.RiskZoneCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk-zones")
public class RiskZoneController {

    private final RiskZoneCatalog catalog;

    public RiskZoneController(RiskZoneCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<RiskZone> listRiskZones() {
        return catalog.all();
    }
}
