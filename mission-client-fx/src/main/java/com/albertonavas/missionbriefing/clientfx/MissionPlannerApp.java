package com.albertonavas.missionbriefing.clientfx;

import com.albertonavas.missionbriefing.clientfx.dto.ExtractionPointDto;
import com.albertonavas.missionbriefing.clientfx.dto.GeoPointDto;
import com.albertonavas.missionbriefing.clientfx.dto.MissionDto;
import com.albertonavas.missionbriefing.clientfx.dto.NearestExtractionDto;
import com.albertonavas.missionbriefing.clientfx.dto.ResourceDto;
import com.albertonavas.missionbriefing.clientfx.dto.RiskZoneDto;
import com.albertonavas.missionbriefing.clientfx.dto.WaypointDto;
import com.albertonavas.missionbriefing.legacymap.Escort;
import com.albertonavas.missionbriefing.legacymap.ExtractionPoint;
import com.albertonavas.missionbriefing.legacymap.LegacyMapPanel;
import com.albertonavas.missionbriefing.legacymap.RiskLevel;
import com.albertonavas.missionbriefing.legacymap.RiskZone;
import com.albertonavas.missionbriefing.model.TaskType;
import com.albertonavas.missionbriefing.model.Waypoint;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.swing.SwingUtilities;
import org.jxmapviewer.viewer.GeoPosition;

/**
 * Cliente de escritorio principal. La lista de misiones y el marco de la ventana son
 * JavaFX; el visor de mapa es el modulo Swing heredado (LegacyMapPanel), embebido
 * mediante SwingNode en vez de reescrito, el mismo patron de modernizacion parcial que
 * se da en aplicaciones de defensa con paneles graficos antiguos ya probados.
 *
 * <p>Incluye una simulacion de movimiento del convoy sobre su ruta con aviso en vivo al
 * entrar en una zona de riesgo, y una simulacion de perdida de escolta que redirige a la
 * mision hacia el punto de extraccion seguro mas cercano: son herramientas de aviso y
 * retirada defensivos para la mision propia, no de planeamiento de ataque -- ver
 * "Limites eticos" en el README.
 */
public class MissionPlannerApp extends Application {

    private final ApiClient apiClient =
            new ApiClient(System.getProperty("mission.server.url", "http://localhost:8080"));
    private final LegacyMapPanel mapPanel = new LegacyMapPanel();
    private final ListView<MissionDto> missionList = new ListView<>();
    private final VBox escortListBox = new VBox(4);
    private final Label alertBanner = new Label("Sin misión seleccionada");
    private final Button startButton = new Button("▶ Iniciar movimiento");
    private final Button pauseButton = new Button("⏸ Pausar");
    private final Button resetButton = new Button("⟲ Reiniciar");

    private boolean securityCompromised;

    @Override
    public void start(Stage stage) {
        missionList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(MissionDto mission, boolean empty) {
                super.updateItem(mission, empty);
                setText(empty || mission == null ? null : mission.toString());
            }
        });
        missionList.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> {
            boolean hasSelection = selected != null;
            startButton.setDisable(!hasSelection);
            pauseButton.setDisable(!hasSelection);
            resetButton.setDisable(!hasSelection);
            securityCompromised = false;
            if (hasSelection) {
                mapPanel.showWaypoints(toModelWaypoints(selected.waypoints()));
                mapPanel.showEscorts(toEscorts(selected.resources()), firstPosition(selected));
                rebuildEscortList(selected);
                setAlertBanner("Misión seleccionada: " + selected, false);
            } else {
                escortListBox.getChildren().clear();
            }
        });

        mapPanel.setRiskZoneListener(zone -> Platform.runLater(() -> updateZoneAlert(zone)));
        mapPanel.setEscortLostListener((escort, lastKnownPosition) ->
                Platform.runLater(() -> handleEscortLost(escort, lastKnownPosition)));

        Button refreshButton = new Button("Actualizar misiones");
        refreshButton.setOnAction(event -> refreshMissions());

        startButton.setDisable(true);
        pauseButton.setDisable(true);
        resetButton.setDisable(true);
        startButton.setOnAction(event -> {
            MissionDto selected = missionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                startConvoyMovement(selected);
            }
        });
        pauseButton.setOnAction(event -> {
            if (mapPanel.isAnimationRunning()) {
                mapPanel.pauseConvoyAnimation();
                pauseButton.setText("▶ Reanudar");
            } else {
                mapPanel.resumeConvoyAnimation();
                pauseButton.setText("⏸ Pausar");
            }
        });
        resetButton.setOnAction(event -> {
            mapPanel.resetConvoyAnimation();
            pauseButton.setText("⏸ Pausar");
            securityCompromised = false;
            MissionDto selected = missionList.getSelectionModel().getSelectedItem();
            setAlertBanner(selected != null ? "Misión seleccionada: " + selected : "Sin misión seleccionada", false);
            if (selected != null) {
                rebuildEscortList(selected);
            }
        });
        HBox convoyControls = new HBox(6, startButton, pauseButton, resetButton);

        VBox leftPane = new VBox(8, new Label("Misiones"), refreshButton, missionList,
                new Label("Escoltas"), escortListBox,
                new Label("Simulación de convoy"), convoyControls);
        leftPane.setPadding(new Insets(8));
        leftPane.setPrefWidth(300);

        alertBanner.setPadding(new Insets(8));
        alertBanner.setMaxWidth(Double.MAX_VALUE);
        alertBanner.setStyle(bannerStyle(null));

        SwingNode mapNode = new SwingNode();
        SwingUtilities.invokeLater(() -> mapNode.setContent(mapPanel));

        BorderPane root = new BorderPane();
        root.setLeft(leftPane);
        root.setTop(alertBanner);
        root.setCenter(mapNode);

        stage.setTitle("Mission Briefing Planner");
        stage.setScene(new Scene(root, 1050, 680));
        stage.show();

        refreshMissions();
        loadRiskZones();
        loadExtractionPoints();
    }

    private void refreshMissions() {
        Task<List<MissionDto>> loadMissions = new Task<>() {
            @Override
            protected List<MissionDto> call() throws Exception {
                return apiClient.listMissions();
            }
        };
        loadMissions.setOnSucceeded(event -> missionList.getItems().setAll(loadMissions.getValue()));
        loadMissions.setOnFailed(event -> Platform.runLater(() -> showConnectionError(loadMissions.getException())));
        new Thread(loadMissions, "mission-fetch").start();
    }

    /**
     * Pide al servidor la ruta real por carretera (OSRM) y anima el convoy sobre ella;
     * si el servicio de rutas falla (sin red, límite de uso del demo público...), cae
     * automáticamente a la animación en línea recta entre waypoints.
     */
    private void startConvoyMovement(MissionDto mission) {
        Task<List<GeoPointDto>> loadRoute = new Task<>() {
            @Override
            protected List<GeoPointDto> call() throws Exception {
                return apiClient.getRoadRoute(mission.id());
            }
        };
        loadRoute.setOnSucceeded(event ->
                mapPanel.startConvoyAnimationAlongRoute(toGeoPositions(loadRoute.getValue())));
        loadRoute.setOnFailed(event -> Platform.runLater(() -> {
            mapPanel.startConvoyAnimation(toModelWaypoints(mission.waypoints()));
            setAlertBanner("Ruta real no disponible (sin conexión al servicio de rutas) — animando en línea recta.", false);
        }));
        new Thread(loadRoute, "road-route-fetch").start();
    }

    private void loadRiskZones() {
        Task<List<RiskZoneDto>> loadZones = new Task<>() {
            @Override
            protected List<RiskZoneDto> call() throws Exception {
                return apiClient.listRiskZones();
            }
        };
        loadZones.setOnSucceeded(event -> mapPanel.showRiskZones(toModelRiskZones(loadZones.getValue())));
        // Si falla, simplemente no se pintan zonas -- no es un fallo critico para ver las misiones.
        new Thread(loadZones, "risk-zone-fetch").start();
    }

    private void loadExtractionPoints() {
        Task<List<ExtractionPointDto>> loadPoints = new Task<>() {
            @Override
            protected List<ExtractionPointDto> call() throws Exception {
                return apiClient.listExtractionPoints();
            }
        };
        loadPoints.setOnSucceeded(event -> mapPanel.showExtractionPoints(toExtractionPoints(loadPoints.getValue())));
        new Thread(loadPoints, "extraction-point-fetch").start();
    }

    private void rebuildEscortList(MissionDto mission) {
        escortListBox.getChildren().clear();
        for (ResourceDto resource : mission.resources()) {
            Label label = new Label(displayName(resource));
            Button lostButton = new Button("✕ Marcar perdido");
            lostButton.setOnAction(event -> {
                mapPanel.markEscortLost(String.valueOf(resource.id()));
                lostButton.setDisable(true);
                lostButton.setText("Perdido");
            });
            HBox row = new HBox(8, label, lostButton);
            row.setAlignment(Pos.CENTER_LEFT);
            escortListBox.getChildren().add(row);
        }
    }

    /**
     * Al perder un escolta, la seguridad de la mision se considera comprometida: se
     * calcula el punto de extraccion mas cercano a la ultima posicion conocida del
     * convoy y se desvia la animacion hacia el, en vez de continuar la ruta original.
     */
    private void handleEscortLost(Escort escort, GeoPosition lastKnownPosition) {
        securityCompromised = true;
        setAlertBanner("🚨 SEGURIDAD COMPROMETIDA — escolta %s perdido. Calculando ruta de extracción..."
                .formatted(escort.callSign()), true);

        MissionDto selected = missionList.getSelectionModel().getSelectedItem();
        GeoPosition from = lastKnownPosition != null ? lastKnownPosition
                : (selected != null ? firstPosition(selected) : null);
        if (from == null) {
            setAlertBanner("🚨 SEGURIDAD COMPROMETIDA — escolta %s perdido (sin posición conocida para calcular la extracción)."
                    .formatted(escort.callSign()), true);
            return;
        }

        Task<NearestExtractionDto> loadExtraction = new Task<>() {
            @Override
            protected NearestExtractionDto call() throws Exception {
                return apiClient.getNearestExtractionRoute(from.getLatitude(), from.getLongitude());
            }
        };
        loadExtraction.setOnSucceeded(event -> {
            NearestExtractionDto result = loadExtraction.getValue();
            mapPanel.rerouteToExtraction(toGeoPositions(result.route()));
            setAlertBanner("🚨 SEGURIDAD COMPROMETIDA — redirigiendo a punto de extracción: %s"
                    .formatted(result.point().label()), true);
        });
        loadExtraction.setOnFailed(event -> Platform.runLater(() -> setAlertBanner(
                "🚨 SEGURIDAD COMPROMETIDA — escolta %s perdido, pero no se pudo calcular la ruta de extracción (sin conexión)."
                        .formatted(escort.callSign()), true)));
        new Thread(loadExtraction, "nearest-extraction-fetch").start();
    }

    private void updateZoneAlert(RiskZone zone) {
        if (securityCompromised) {
            return;
        }
        if (zone == null) {
            MissionDto selected = missionList.getSelectionModel().getSelectedItem();
            setAlertBanner(selected != null ? "Misión seleccionada: " + selected : "Sin misión seleccionada", false);
        } else {
            setAlertBanner("⚠ ALERTA (%s): %s — %s".formatted(zone.level(), zone.label(), zone.reason()), true);
        }
    }

    private void setAlertBanner(String text, boolean alarmed) {
        alertBanner.setText(text);
        alertBanner.setStyle(bannerStyle(alarmed));
    }

    private String bannerStyle(Boolean alarmed) {
        if (Boolean.TRUE.equals(alarmed)) {
            return "-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-weight: bold;";
        }
        return "-fx-background-color: #eceff1; -fx-text-fill: #37474f;";
    }

    private void showConnectionError(Throwable cause) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "No se pudo conectar con mission-server.%n%s"
                        .formatted(cause != null ? cause.getMessage() : "error desconocido"));
        alert.setHeaderText("Servidor no disponible");
        alert.showAndWait();
    }

    private String displayName(ResourceDto resource) {
        return resource.callSign() != null && !resource.callSign().isBlank() ? resource.callSign() : resource.name();
    }

    private GeoPosition firstPosition(MissionDto mission) {
        return mission.waypoints().stream()
                .min((a, b) -> Integer.compare(a.sequenceOrder(), b.sequenceOrder()))
                .map(w -> new GeoPosition(w.latitude(), w.longitude()))
                .orElse(null);
    }

    private List<Escort> toEscorts(List<ResourceDto> resources) {
        return resources.stream()
                .map(r -> new Escort(String.valueOf(r.id()), displayName(r)))
                .collect(Collectors.toList());
    }

    private List<ExtractionPoint> toExtractionPoints(List<ExtractionPointDto> points) {
        return points.stream()
                .map(p -> new ExtractionPoint(p.id(), p.label(), p.latitude(), p.longitude()))
                .collect(Collectors.toList());
    }

    private List<Waypoint> toModelWaypoints(List<WaypointDto> waypoints) {
        return waypoints.stream()
                .map(w -> new Waypoint(w.sequenceOrder(), w.latitude(), w.longitude(), TaskType.valueOf(w.taskType()), w.notes()))
                .collect(Collectors.toList());
    }

    private List<GeoPosition> toGeoPositions(List<GeoPointDto> points) {
        return points.stream()
                .map(p -> new GeoPosition(p.latitude(), p.longitude()))
                .collect(Collectors.toList());
    }

    private List<RiskZone> toModelRiskZones(List<RiskZoneDto> zones) {
        return zones.stream()
                .map(z -> new RiskZone(z.id(), z.label(), z.reason(), RiskLevel.valueOf(z.level()),
                        z.latitude(), z.longitude(), z.radiusMeters()))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
