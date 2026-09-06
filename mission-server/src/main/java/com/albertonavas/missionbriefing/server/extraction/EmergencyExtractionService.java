package com.albertonavas.missionbriefing.server.extraction;

import com.albertonavas.missionbriefing.server.route.GeoPoint;
import com.albertonavas.missionbriefing.server.route.OsrmRouteClient;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * Si la seguridad de una mision se ve comprometida (p.ej. se pierde un escolta), calcula
 * el punto de extraccion mas cercano desde la posicion actual y la ruta real mas rapida
 * hasta el. Herramienta de retirada a un lugar seguro, no de ataque.
 */
@Service
public class EmergencyExtractionService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final ExtractionPointCatalog catalog;
    private final OsrmRouteClient osrmRouteClient;

    public EmergencyExtractionService(ExtractionPointCatalog catalog, OsrmRouteClient osrmRouteClient) {
        this.catalog = catalog;
        this.osrmRouteClient = osrmRouteClient;
    }

    public NearestExtractionResult routeToNearestExtraction(double fromLat, double fromLon) {
        ExtractionPoint nearest = catalog.all().stream()
                .min(Comparator.comparingDouble(p -> distanceMeters(fromLat, fromLon, p.latitude(), p.longitude())))
                .orElseThrow(() -> new NoSuchElementException("No hay puntos de extraccion en el catalogo"));

        List<GeoPoint> route = osrmRouteClient.fetchRoute(fromLat, fromLon, nearest.latitude(), nearest.longitude());
        return new NearestExtractionResult(nearest, route);
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
