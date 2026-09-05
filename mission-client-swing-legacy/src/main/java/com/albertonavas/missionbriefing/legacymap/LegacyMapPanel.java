package com.albertonavas.missionbriefing.legacymap;

import com.albertonavas.missionbriefing.model.Waypoint;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.viewer.WaypointPainter;

/**
 * Visor de mapa Swing (jxmapviewer2 sobre tiles OpenStreetMap), pensado para integrarse
 * sin reescribir en el cliente JavaFX via SwingNode. Sustituye a Luciad (SDK de mapas
 * propietario) por una libreria libre equivalente para este portafolio.
 *
 * <p>Ademas de mostrar los waypoints de una mision, puede pintar zonas de riesgo y
 * animar el movimiento de un convoy por su ruta, avisando cuando entra en una zona
 * (herramienta de aviso defensivo sobre la mision propia, no de ataque).
 */
public class LegacyMapPanel extends JPanel {

    /** Notificado cuando el convoy animado entra o sale de una zona de riesgo. */
    public interface RiskZoneListener {
        void onZoneChange(RiskZone zoneOrNull);
    }

    // Estrecho de Gibraltar/mar de Alboran: misma zona demo que los hermanos Python del portafolio.
    private static final GeoPosition DEFAULT_CENTER = new GeoPosition(36.0, -5.6);
    private static final int DEFAULT_ZOOM = 7;
    private static final int ANIMATION_DURATION_MS = 12_000;
    private static final int TICK_MS = 40;

    private final JXMapViewer mapViewer = new JXMapViewer();
    private final WaypointPainter<org.jxmapviewer.viewer.Waypoint> waypointPainter = new WaypointPainter<>();
    private final RiskZonePainter riskZonePainter = new RiskZonePainter();
    private final ConvoyMarkerPainter convoyMarkerPainter = new ConvoyMarkerPainter();

    private Timer animationTimer;
    private List<Waypoint> animationRoute = List.of();
    private List<RiskZone> riskZones = List.of();
    private RiskZoneListener riskZoneListener;
    private RiskZone currentZone;
    private int elapsedMs;

    public LegacyMapPanel() {
        super(new BorderLayout());

        // El constructor sin argumentos de OSMTileFactoryInfo apunta a
        // http://tile.openstreetmap.org (HTTP, sin subdominio), que la politica actual de
        // OpenStreetMap ya no sirve. Se fuerza HTTPS explicitamente.
        TileFactoryInfo info = new OSMTileFactoryInfo("OpenStreetMap", "https://tile.openstreetmap.org");
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);
        mapViewer.setZoom(DEFAULT_ZOOM);
        mapViewer.setAddressLocation(DEFAULT_CENTER);

        CompoundPainter<JXMapViewer> compoundPainter =
                new CompoundPainter<>(riskZonePainter, waypointPainter, convoyMarkerPainter);
        mapViewer.setOverlayPainter(compoundPainter);

        MouseInputListener panListener = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(panListener);
        mapViewer.addMouseMotionListener(panListener);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        add(mapViewer, BorderLayout.CENTER);
    }

    /** Reemplaza los marcadores mostrados por los waypoints de una mision y centra el mapa en el primero. */
    public void showWaypoints(List<Waypoint> waypoints) {
        stopConvoyAnimation();

        Set<org.jxmapviewer.viewer.Waypoint> markers = new HashSet<>();
        for (Waypoint w : waypoints) {
            markers.add(new DefaultWaypoint(new GeoPosition(w.getLatitude(), w.getLongitude())));
        }
        waypointPainter.setWaypoints(markers);

        if (!waypoints.isEmpty()) {
            Waypoint first = waypoints.get(0);
            mapViewer.setAddressLocation(new GeoPosition(first.getLatitude(), first.getLongitude()));
        }
        repaint();
    }

    /** Muestra las zonas de riesgo como circulos de color sobre el mapa. */
    public void showRiskZones(List<RiskZone> zones) {
        this.riskZones = zones == null ? List.of() : List.copyOf(zones);
        riskZonePainter.setZones(this.riskZones);
        repaint();
    }

    public void setRiskZoneListener(RiskZoneListener listener) {
        this.riskZoneListener = listener;
    }

    /**
     * Anima el movimiento del convoy por la ruta de la mision, en tiempo simulado
     * (comprimido a {@value #ANIMATION_DURATION_MS} ms independientemente de la duracion
     * real de la mision), avisando por {@link RiskZoneListener} al entrar o salir de una
     * zona de riesgo.
     */
    public void startConvoyAnimation(List<Waypoint> waypoints) {
        stopConvoyAnimation();

        List<Waypoint> route = new ArrayList<>(waypoints);
        route.sort((a, b) -> Integer.compare(a.getSequenceOrder(), b.getSequenceOrder()));
        this.animationRoute = route;

        if (route.size() < 2) {
            return;
        }

        elapsedMs = 0;
        currentZone = null;
        animationTimer = new Timer(TICK_MS, e -> tickAnimation());
        animationTimer.start();
    }

    public void pauseConvoyAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    public void resumeConvoyAnimation() {
        if (animationTimer != null && animationRoute.size() >= 2 && elapsedMs < ANIMATION_DURATION_MS) {
            animationTimer.start();
        }
    }

    public void resetConvoyAnimation() {
        stopConvoyAnimation();
        convoyMarkerPainter.clear();
        notifyZoneChange(null);
        repaint();
    }

    public boolean isAnimationRunning() {
        return animationTimer != null && animationTimer.isRunning();
    }

    private void stopConvoyAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
    }

    private void tickAnimation() {
        elapsedMs = Math.min(elapsedMs + TICK_MS, ANIMATION_DURATION_MS);
        double progress = elapsedMs / (double) ANIMATION_DURATION_MS;

        GeoPosition position = RouteAnimationMath.interpolate(animationRoute, progress);
        convoyMarkerPainter.setPosition(position);

        RiskZone zoneNow = RouteAnimationMath.findContainingZone(position, riskZones);
        if (zoneNow != currentZone) {
            currentZone = zoneNow;
            notifyZoneChange(zoneNow);
        }
        convoyMarkerPainter.setInRiskZone(zoneNow != null);

        repaint();

        if (elapsedMs >= ANIMATION_DURATION_MS) {
            stopConvoyAnimation();
        }
    }

    private void notifyZoneChange(RiskZone zone) {
        if (riskZoneListener != null) {
            riskZoneListener.onZoneChange(zone);
        }
    }

    public int getWaypointMarkerCount() {
        return waypointPainter.getWaypoints().size();
    }
}
