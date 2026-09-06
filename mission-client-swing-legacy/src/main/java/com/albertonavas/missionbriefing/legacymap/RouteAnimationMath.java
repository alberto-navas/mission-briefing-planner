package com.albertonavas.missionbriefing.legacymap;

import java.util.List;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * Matematica de la animacion del convoy, separada de LegacyMapPanel para poder probarla
 * sin depender de un javax.swing.Timer real ni del hilo de Swing.
 *
 * <p>La interpolacion es por distancia acumulada, no por indice de punto: una ruta real
 * (OSRM) tiene muchos mas puntos en los tramos curvos que en los rectos, y repartir el
 * progreso por indice haria que el convoy acelerase y frenase sin motivo. Repartiendolo
 * por distancia real, la velocidad visual es constante independientemente de cuantos
 * puntos tenga cada tramo.
 */
final class RouteAnimationMath {

    private RouteAnimationMath() {
    }

    /** Posicion interpolada a lo largo de {@code route} (ordenada) para {@code progress} en [0,1]. */
    static GeoPosition interpolate(List<GeoPosition> route, double progress) {
        if (route.size() == 1) {
            return route.get(0);
        }

        double[] cumulative = cumulativeDistances(route);
        double totalDistance = cumulative[cumulative.length - 1];
        double targetDistance = clamp(progress) * totalDistance;

        int segmentIndex = findSegment(cumulative, targetDistance);
        double segmentStart = cumulative[segmentIndex];
        double segmentLength = cumulative[segmentIndex + 1] - segmentStart;
        double segmentFraction = segmentLength <= 0 ? 0 : (targetDistance - segmentStart) / segmentLength;

        GeoPosition from = route.get(segmentIndex);
        GeoPosition to = route.get(segmentIndex + 1);
        double lat = from.getLatitude() + (to.getLatitude() - from.getLatitude()) * segmentFraction;
        double lon = from.getLongitude() + (to.getLongitude() - from.getLongitude()) * segmentFraction;
        return new GeoPosition(lat, lon);
    }

    /** Primera zona (en el orden dado) cuyo radio cubre {@code position}, o null si ninguna. */
    static RiskZone findContainingZone(GeoPosition position, List<RiskZone> zones) {
        for (RiskZone zone : zones) {
            double distance = GeoMath.distanceMeters(
                    position.getLatitude(), position.getLongitude(), zone.latitude(), zone.longitude());
            if (distance <= zone.radiusMeters()) {
                return zone;
            }
        }
        return null;
    }

    private static double[] cumulativeDistances(List<GeoPosition> route) {
        double[] cumulative = new double[route.size()];
        for (int i = 1; i < route.size(); i++) {
            GeoPosition previous = route.get(i - 1);
            GeoPosition current = route.get(i);
            double segmentDistance = GeoMath.distanceMeters(
                    previous.getLatitude(), previous.getLongitude(), current.getLatitude(), current.getLongitude());
            cumulative[i] = cumulative[i - 1] + segmentDistance;
        }
        return cumulative;
    }

    private static int findSegment(double[] cumulative, double targetDistance) {
        int lastSegment = cumulative.length - 2;
        for (int i = 0; i <= lastSegment; i++) {
            if (targetDistance <= cumulative[i + 1] || i == lastSegment) {
                return i;
            }
        }
        return lastSegment;
    }

    private static double clamp(double progress) {
        return Math.max(0.0, Math.min(1.0, progress));
    }
}
