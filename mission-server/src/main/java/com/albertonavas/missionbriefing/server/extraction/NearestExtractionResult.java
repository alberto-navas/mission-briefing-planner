package com.albertonavas.missionbriefing.server.extraction;

import com.albertonavas.missionbriefing.server.route.GeoPoint;
import java.util.List;

public record NearestExtractionResult(ExtractionPoint point, List<GeoPoint> route) {
}
