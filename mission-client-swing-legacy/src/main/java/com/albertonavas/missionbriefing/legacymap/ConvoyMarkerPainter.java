package com.albertonavas.missionbriefing.legacymap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

/** Dibuja la posicion actual del convoy en movimiento: un marcador distinto de los waypoints fijos. */
public class ConvoyMarkerPainter implements Painter<JXMapViewer> {

    private static final int RADIUS_PX = 8;

    private volatile GeoPosition position;
    private volatile boolean inRiskZone;

    public void setPosition(GeoPosition position) {
        this.position = position;
    }

    public void setInRiskZone(boolean inRiskZone) {
        this.inRiskZone = inRiskZone;
    }

    public void clear() {
        this.position = null;
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        GeoPosition currentPosition = this.position;
        if (currentPosition == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle viewport = map.getViewportBounds();
        g2.translate(-viewport.x, -viewport.y);

        Point2D point = map.getTileFactory().geoToPixel(currentPosition, map.getZoom());
        Ellipse2D marker = new Ellipse2D.Double(
                point.getX() - RADIUS_PX, point.getY() - RADIUS_PX, RADIUS_PX * 2.0, RADIUS_PX * 2.0);

        g2.setColor(inRiskZone ? new Color(220, 30, 30) : new Color(30, 140, 60));
        g2.fill(marker);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(marker);

        g2.dispose();
    }
}
