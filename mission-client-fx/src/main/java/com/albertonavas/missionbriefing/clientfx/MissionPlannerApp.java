package com.albertonavas.missionbriefing.clientfx;

import com.albertonavas.missionbriefing.clientfx.dto.MissionDto;
import com.albertonavas.missionbriefing.clientfx.dto.WaypointDto;
import com.albertonavas.missionbriefing.legacymap.LegacyMapPanel;
import com.albertonavas.missionbriefing.model.TaskType;
import com.albertonavas.missionbriefing.model.Waypoint;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.swing.SwingUtilities;

/**
 * Cliente de escritorio principal. La lista de misiones y el marco de la ventana son
 * JavaFX; el visor de mapa es el modulo Swing heredado (LegacyMapPanel), embebido
 * mediante SwingNode en vez de reescrito, el mismo patron de modernizacion parcial que
 * se da en aplicaciones de defensa con paneles graficos antiguos ya probados.
 */
public class MissionPlannerApp extends Application {

    private final ApiClient apiClient =
            new ApiClient(System.getProperty("mission.server.url", "http://localhost:8080"));
    private final LegacyMapPanel mapPanel = new LegacyMapPanel();
    private final ListView<MissionDto> missionList = new ListView<>();

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
            if (selected != null) {
                mapPanel.showWaypoints(toModelWaypoints(selected.waypoints()));
            }
        });

        Button refreshButton = new Button("Actualizar misiones");
        refreshButton.setOnAction(event -> refreshMissions());

        VBox leftPane = new VBox(8, new Label("Misiones"), refreshButton, missionList);
        leftPane.setPadding(new Insets(8));
        leftPane.setPrefWidth(280);

        SwingNode mapNode = new SwingNode();
        SwingUtilities.invokeLater(() -> mapNode.setContent(mapPanel));

        BorderPane root = new BorderPane();
        root.setLeft(leftPane);
        root.setCenter(mapNode);

        stage.setTitle("Mission Briefing Planner");
        stage.setScene(new Scene(root, 1000, 650));
        stage.show();

        refreshMissions();
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

    private void showConnectionError(Throwable cause) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "No se pudo conectar con mission-server.%n%s"
                        .formatted(cause != null ? cause.getMessage() : "error desconocido"));
        alert.setHeaderText("Servidor no disponible");
        alert.showAndWait();
    }

    private List<Waypoint> toModelWaypoints(List<WaypointDto> waypoints) {
        return waypoints.stream()
                .map(w -> new Waypoint(w.sequenceOrder(), w.latitude(), w.longitude(), TaskType.valueOf(w.taskType()), w.notes()))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
