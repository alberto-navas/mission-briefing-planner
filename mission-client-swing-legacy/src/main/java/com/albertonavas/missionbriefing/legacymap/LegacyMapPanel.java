package com.albertonavas.missionbriefing.legacymap;

import com.albertonavas.missionbriefing.model.Waypoint;
import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.jxmapviewer.viewer.WaypointPainter;

/**
 * Visor de mapa Swing (jxmapviewer2 sobre tiles OpenStreetMap), pensado para integrarse
 * sin reescribir en el cliente JavaFX via SwingNode. Sustituye a Luciad (SDK de mapas
 * propietario) por una libreria libre equivalente para este portafolio.
 */
public class LegacyMapPanel extends JPanel {

    // Estrecho de Gibraltar/mar de Alboran: misma zona demo que los hermanos Python del portafolio.
    private static final GeoPosition DEFAULT_CENTER = new GeoPosition(36.0, -5.6);
    private static final int DEFAULT_ZOOM = 7;

    private final JXMapViewer mapViewer = new JXMapViewer();
    private final WaypointPainter<org.jxmapviewer.viewer.Waypoint> waypointPainter = new WaypointPainter<>();

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
        mapViewer.setOverlayPainter(waypointPainter);

        MouseInputListener panListener = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(panListener);
        mapViewer.addMouseMotionListener(panListener);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        add(mapViewer, BorderLayout.CENTER);
    }

    /** Reemplaza los marcadores mostrados por los waypoints de una mision y centra el mapa en el primero. */
    public void showWaypoints(List<Waypoint> waypoints) {
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

    public int getWaypointMarkerCount() {
        return waypointPainter.getWaypoints().size();
    }
}
