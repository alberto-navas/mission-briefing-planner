package com.albertonavas.missionbriefing.legacymap;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.List;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

/** Dibuja las zonas de riesgo como circulos translucidos, coloreados segun su nivel. */
public class RiskZonePainter implements Painter<JXMapViewer> {

    private volatile List<RiskZone> zones = List.of();

    public void setZones(List<RiskZone> zones) {
        this.zones = zones == null ? List.of() : List.copyOf(zones);
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (zones.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle viewport = map.getViewportBounds();
        g2.translate(-viewport.x, -viewport.y);

        for (RiskZone zone : zones) {
            paintZone(g2, map, zone);
        }

        g2.dispose();
    }

    private void paintZone(Graphics2D g2, JXMapViewer map, RiskZone zone) {
        Point2D center = map.getTileFactory().geoToPixel(
                new GeoPosition(zone.latitude(), zone.longitude()), map.getZoom());

        double radiusPixels = metersToPixels(map, zone);

        Ellipse2D circle = new Ellipse2D.Double(
                center.getX() - radiusPixels, center.getY() - radiusPixels, radiusPixels * 2, radiusPixels * 2);

        Color fill = fillColor(zone.level());
        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g2.setColor(fill);
        g2.fill(circle);
        g2.setComposite(original);

        g2.setColor(fill.darker());
        g2.draw(circle);
    }

    /** Aproximacion valida para el radio de zonas de pocos kilometros a las latitudes de la demo. */
    private double metersToPixels(JXMapViewer map, RiskZone zone) {
        GeoPosition center = new GeoPosition(zone.latitude(), zone.longitude());
        double metersPerDegreeLon = GeoMath.distanceMeters(
                zone.latitude(), zone.longitude(), zone.latitude(), zone.longitude() + 1.0);
        double radiusDegrees = zone.radiusMeters() / metersPerDegreeLon;

        Point2D centerPx = map.getTileFactory().geoToPixel(center, map.getZoom());
        Point2D edgePx = map.getTileFactory().geoToPixel(
                new GeoPosition(zone.latitude(), zone.longitude() + radiusDegrees), map.getZoom());
        return centerPx.distance(edgePx);
    }

    private Color fillColor(RiskLevel level) {
        return switch (level) {
            case HIGH -> new Color(200, 40, 40);
            case MEDIUM -> new Color(220, 140, 30);
            case LOW -> new Color(210, 190, 40);
        };
    }
}
