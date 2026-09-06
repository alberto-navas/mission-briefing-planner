package com.albertonavas.missionbriefing.legacymap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.List;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

/** Dibuja los puntos de extraccion (retirada segura) como un marcador verde con etiqueta. */
public class ExtractionPointPainter implements Painter<JXMapViewer> {

    private static final int RADIUS_PX = 7;
    private static final Color FILL = new Color(30, 140, 90);

    private volatile List<ExtractionPoint> points = List.of();

    public void setPoints(List<ExtractionPoint> points) {
        this.points = points == null ? List.of() : List.copyOf(points);
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (points.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));

        Rectangle viewport = map.getViewportBounds();
        g2.translate(-viewport.x, -viewport.y);

        for (ExtractionPoint point : points) {
            Point2D p = map.getTileFactory().geoToPixel(
                    new GeoPosition(point.latitude(), point.longitude()), map.getZoom());

            g2.setColor(FILL);
            g2.fill(new Ellipse2D.Double(p.getX() - RADIUS_PX, p.getY() - RADIUS_PX, RADIUS_PX * 2.0, RADIUS_PX * 2.0));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Double(p.getX() - RADIUS_PX, p.getY() - RADIUS_PX, RADIUS_PX * 2.0, RADIUS_PX * 2.0));

            g2.setColor(FILL.darker());
            g2.drawString("⛑ " + point.label(), (float) p.getX() + RADIUS_PX + 4, (float) p.getY() + 4);
        }

        g2.dispose();
    }
}
