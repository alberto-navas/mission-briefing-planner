package com.albertonavas.missionbriefing.legacymap;

import com.albertonavas.missionbriefing.model.Waypoint;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /** Notificado cuando un escolta se marca como perdido durante la animacion. */
    public interface EscortLostListener {
        void onEscortLost(Escort escort, GeoPosition lastKnownConvoyPosition);
    }

    // Estrecho de Gibraltar/mar de Alboran: misma zona demo que los hermanos Python del portafolio.
    private static final GeoPosition DEFAULT_CENTER = new GeoPosition(36.0, -5.6);
    private static final int DEFAULT_ZOOM = 7;
    private static final int ANIMATION_DURATION_MS = 12_000;
    private static final int TICK_MS = 40;
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final double ESCORT_LATERAL_OFFSET_METERS = 30;

    private final JXMapViewer mapViewer = new JXMapViewer();
    private final WaypointPainter<org.jxmapviewer.viewer.Waypoint> waypointPainter = new WaypointPainter<>();
    private final RiskZonePainter riskZonePainter = new RiskZonePainter();
    private final ConvoyMarkerPainter convoyMarkerPainter = new ConvoyMarkerPainter();
    private final ExtractionPointPainter extractionPointPainter = new ExtractionPointPainter();
    private final EscortMarkerPainter escortMarkerPainter = new EscortMarkerPainter();

    private Timer animationTimer;
    private List<GeoPosition> animationRoute = List.of();
    private List<RiskZone> riskZones = List.of();
    private RiskZoneListener riskZoneListener;
    private EscortLostListener escortLostListener;
    private RiskZone currentZone;
    private int elapsedMs;
    private GeoPosition currentConvoyPosition;

    private List<Escort> escorts = List.of();
    private final Map<String, EscortMarkerPainter.EscortState> escortStates = new LinkedHashMap<>();

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

        CompoundPainter<JXMapViewer> compoundPainter = new CompoundPainter<>(
                riskZonePainter, waypointPainter, extractionPointPainter, convoyMarkerPainter, escortMarkerPainter);
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

    /** Muestra los puntos de extraccion (retirada segura) sobre el mapa. */
    public void showExtractionPoints(List<ExtractionPoint> points) {
        extractionPointPainter.setPoints(points);
        repaint();
    }

    public void setEscortLostListener(EscortLostListener listener) {
        this.escortLostListener = listener;
    }

    /** Registra los escoltas de la mision seleccionada; se moveran junto al convoy al animar. */
    public void showEscorts(List<Escort> missionEscorts, GeoPosition initialPosition) {
        this.escorts = List.copyOf(missionEscorts);
        escortStates.clear();
        for (int i = 0; i < escorts.size(); i++) {
            Escort escort = escorts.get(i);
            GeoPosition position = initialPosition == null ? DEFAULT_CENTER : offsetPosition(initialPosition, i);
            escortStates.put(escort.id(), new EscortMarkerPainter.EscortState(position, escort.callSign(), false));
        }
        escortMarkerPainter.setEscorts(escortStates);
        repaint();
    }

    /**
     * Marca un escolta como perdido: se congela en su ultima posicion conocida y deja de
     * moverse con el convoy. Avisa por {@link EscortLostListener} para que quien gestione
     * la mision pueda calcular una ruta de emergencia al punto de extraccion mas cercano.
     */
    public void markEscortLost(String escortId) {
        EscortMarkerPainter.EscortState previous = escortStates.get(escortId);
        if (previous == null || previous.lost()) {
            return;
        }
        escortStates.put(escortId, new EscortMarkerPainter.EscortState(previous.position(), previous.callSign(), true));
        escortMarkerPainter.setEscorts(escortStates);
        repaint();

        if (escortLostListener != null) {
            Escort escort = escorts.stream().filter(e -> e.id().equals(escortId)).findFirst().orElse(null);
            if (escort != null) {
                escortLostListener.onEscortLost(escort, currentConvoyPosition);
            }
        }
    }

    /**
     * Desvia la animacion en marcha hacia una ruta de emergencia (p.ej. al punto de
     * extraccion mas cercano tras perder un escolta), sin detener el temporizador:
     * simplemente sustituye la ruta y reinicia el progreso desde la posicion actual.
     */
    public void rerouteToExtraction(List<GeoPosition> emergencyRoute) {
        this.animationRoute = List.copyOf(emergencyRoute);
        this.elapsedMs = 0;
        this.currentZone = null;
    }

    public GeoPosition getCurrentConvoyPosition() {
        return currentConvoyPosition;
    }

    /**
     * Anima el convoy en linea recta entre los waypoints de la mision (sin seguir
     * calles reales). Se usa como reserva cuando no hay ruta real disponible; ver
     * {@link #startConvoyAnimationAlongRoute(List)}.
     */
    public void startConvoyAnimation(List<Waypoint> waypoints) {
        List<Waypoint> sorted = new ArrayList<>(waypoints);
        sorted.sort((a, b) -> Integer.compare(a.getSequenceOrder(), b.getSequenceOrder()));

        List<GeoPosition> route = new ArrayList<>(sorted.size());
        for (Waypoint w : sorted) {
            route.add(new GeoPosition(w.getLatitude(), w.getLongitude()));
        }
        startConvoyAnimationAlongRoute(route);
    }

    /**
     * Anima el movimiento del convoy a lo largo de una ruta ya calculada (p.ej. una ruta
     * real por carretera de OSRM), en tiempo simulado (comprimido a
     * {@value #ANIMATION_DURATION_MS} ms independientemente de la duracion real de la
     * mision), avisando por {@link RiskZoneListener} al entrar o salir de una zona de
     * riesgo. La velocidad visual es constante: el progreso se reparte por distancia
     * real recorrida, no por numero de puntos de la ruta.
     */
    public void startConvoyAnimationAlongRoute(List<GeoPosition> routePoints) {
        stopConvoyAnimation();
        this.animationRoute = List.copyOf(routePoints);

        if (animationRoute.size() < 2) {
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
        currentConvoyPosition = null;
        notifyZoneChange(null);

        if (!animationRoute.isEmpty()) {
            GeoPosition start = animationRoute.get(0);
            for (int i = 0; i < escorts.size(); i++) {
                Escort escort = escorts.get(i);
                escortStates.put(escort.id(),
                        new EscortMarkerPainter.EscortState(offsetPosition(start, i), escort.callSign(), false));
            }
            escortMarkerPainter.setEscorts(escortStates);
        }
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
        currentConvoyPosition = position;
        convoyMarkerPainter.setPosition(position);

        RiskZone zoneNow = RouteAnimationMath.findContainingZone(position, riskZones);
        if (zoneNow != currentZone) {
            currentZone = zoneNow;
            notifyZoneChange(zoneNow);
        }
        convoyMarkerPainter.setInRiskZone(zoneNow != null);

        moveEscortsWithConvoy(position);

        repaint();

        if (elapsedMs >= ANIMATION_DURATION_MS) {
            stopConvoyAnimation();
        }
    }

    /** Los escoltas vivos siguen al convoy con un pequeño desfase lateral; los perdidos se quedan quietos. */
    private void moveEscortsWithConvoy(GeoPosition convoyPosition) {
        if (escorts.isEmpty()) {
            return;
        }
        for (int i = 0; i < escorts.size(); i++) {
            Escort escort = escorts.get(i);
            EscortMarkerPainter.EscortState state = escortStates.get(escort.id());
            if (state != null && state.lost()) {
                continue;
            }
            escortStates.put(escort.id(),
                    new EscortMarkerPainter.EscortState(offsetPosition(convoyPosition, i), escort.callSign(), false));
        }
        escortMarkerPainter.setEscorts(escortStates);
    }

    /** Desplaza {@code base} un poco a un lado u otro segun {@code index}, para separar visualmente varios escoltas. */
    private GeoPosition offsetPosition(GeoPosition base, int index) {
        double side = index % 2 == 0 ? 1 : -1;
        double magnitude = ESCORT_LATERAL_OFFSET_METERS * (1 + index / 2);
        double metersPerDegreeLon = EARTH_RADIUS_METERS * Math.cos(Math.toRadians(base.getLatitude()))
                * (Math.PI / 180.0);
        double lonOffsetDegrees = (side * magnitude) / metersPerDegreeLon;
        return new GeoPosition(base.getLatitude(), base.getLongitude() + lonOffsetDegrees);
    }

    private void notifyZoneChange(RiskZone zone) {
        if (riskZoneListener != null) {
            riskZoneListener.onZoneChange(zone);
        }
    }

    /** Captura el mapa tal como se ve ahora mismo (tiles + waypoints + zonas + convoy/escoltas). */
    public BufferedImage snapshot() {
        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        paint(g);
        g.dispose();
        return image;
    }

    public int getWaypointMarkerCount() {
        return waypointPainter.getWaypoints().size();
    }

    public int getEscortCount() {
        return escortStates.size();
    }

    public boolean isEscortLost(String escortId) {
        EscortMarkerPainter.EscortState state = escortStates.get(escortId);
        return state != null && state.lost();
    }
}
