package com.albertonavas.missionbriefing.legacymap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * Dibuja cada escolta asignado a la mision como un "monigote" azul (emoji si la fuente
 * del sistema lo soporta, si no un icono vectorial equivalente) con su indicativo al
 * lado. Un escolta marcado como perdido se pinta atenuado, en su ultima posicion
 * conocida, con una X.
 */
public class EscortMarkerPainter implements Painter<JXMapViewer> {

    /** Estado visual de un escolta en un instante dado de la animacion. */
    record EscortState(GeoPosition position, String callSign, boolean lost) {
    }

    private static final Color ACTIVE_COLOR = new Color(30, 90, 200);
    private static final Color LOST_COLOR = new Color(120, 120, 120);
    private static final String WALKING_PERSON_EMOJI = "🚶"; // U+1F6B6 person walking

    private final Font emojiFont = resolveEmojiFont();

    private volatile Map<String, EscortState> escorts = new LinkedHashMap<>();

    public void setEscorts(Map<String, EscortState> escorts) {
        this.escorts = new LinkedHashMap<>(escorts);
    }

    @Override
    public void paint(Graphics2D g, JXMapViewer map, int width, int height) {
        if (escorts.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Rectangle viewport = map.getViewportBounds();
        g2.translate(-viewport.x, -viewport.y);

        for (EscortState escort : escorts.values()) {
            paintEscort(g2, map, escort);
        }

        g2.dispose();
    }

    private void paintEscort(Graphics2D g2, JXMapViewer map, EscortState escort) {
        Point2D p = map.getTileFactory().geoToPixel(escort.position(), map.getZoom());
        Color color = escort.lost() ? LOST_COLOR : ACTIVE_COLOR;

        if (emojiFont != null) {
            g2.setFont(emojiFont.deriveFont(26f));
            g2.setColor(color);
            g2.drawString(WALKING_PERSON_EMOJI, (float) p.getX() - 12, (float) p.getY() + 9);
        } else {
            drawStickFigure(g2, p, color);
        }

        if (escort.lost()) {
            g2.setColor(new Color(200, 30, 30));
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new Line2D.Double(p.getX() - 8, p.getY() - 8, p.getX() + 8, p.getY() + 8));
            g2.draw(new Line2D.Double(p.getX() - 8, p.getY() + 8, p.getX() + 8, p.getY() - 8));
        }

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.setColor(color.darker());
        String label = escort.lost() ? escort.callSign() + " (perdido)" : escort.callSign();
        g2.drawString(label, (float) p.getX() + 10, (float) p.getY() - 6);
    }

    /** Icono vectorial equivalente si la fuente del sistema no tiene el emoji (evita un "tofu box"). */
    private void drawStickFigure(Graphics2D g2, Point2D p, Color color) {
        double x = p.getX();
        double y = p.getY();
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new java.awt.geom.Ellipse2D.Double(x - 4, y - 14, 8, 8)); // cabeza
        g2.draw(new Line2D.Double(x, y - 6, x, y + 6)); // cuerpo
        g2.draw(new Line2D.Double(x, y - 2, x - 6, y + 2)); // brazo izq
        g2.draw(new Line2D.Double(x, y - 2, x + 6, y + 2)); // brazo dcho
        g2.draw(new Line2D.Double(x, y + 6, x - 5, y + 14)); // pierna izq
        g2.draw(new Line2D.Double(x, y + 6, x + 5, y + 14)); // pierna dcha
    }

    private static Font resolveEmojiFont() {
        int codePoint = WALKING_PERSON_EMOJI.codePointAt(0);
        for (String name : new String[] {"Segoe UI Emoji", "Noto Color Emoji", "Noto Emoji", "Apple Color Emoji"}) {
            Font candidate = new Font(name, Font.PLAIN, 18);
            if (candidate.canDisplay(codePoint)) {
                return candidate;
            }
        }
        return null;
    }
}
