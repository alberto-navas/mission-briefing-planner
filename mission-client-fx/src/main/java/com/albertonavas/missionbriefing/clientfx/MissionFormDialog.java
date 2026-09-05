package com.albertonavas.missionbriefing.clientfx;

import com.albertonavas.missionbriefing.clientfx.dto.CreateMissionRequestDto;
import com.albertonavas.missionbriefing.clientfx.dto.CreatePhaseRequestDto;
import com.albertonavas.missionbriefing.clientfx.dto.CreateResourceRequestDto;
import com.albertonavas.missionbriefing.clientfx.dto.CreateWaypointRequestDto;
import com.albertonavas.missionbriefing.clientfx.dto.MissionDto;
import com.albertonavas.missionbriefing.clientfx.dto.PhaseDto;
import com.albertonavas.missionbriefing.clientfx.dto.ResourceDto;
import com.albertonavas.missionbriefing.clientfx.dto.WaypointDto;
import com.albertonavas.missionbriefing.model.MissionType;
import com.albertonavas.missionbriefing.model.ResourceType;
import com.albertonavas.missionbriefing.model.TaskType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Formulario para crear o editar una mision desde el propio cliente de escritorio, en
 * vez de solo por API. Monta el mismo JSON que ya aceptan {@code POST}/{@code PUT}
 * {@code /api/missions} -- no necesita ningun endpoint nuevo en el servidor.
 */
final class MissionFormDialog {

    private final Stage stage;
    private final ApiClient apiClient;
    private final Runnable onSaved;
    private final MissionDto existing;

    private final TextField nameField = new TextField();
    private final ComboBox<MissionType> typeField = new ComboBox<>(FXCollections.observableArrayList(MissionType.values()));
    private final DatePicker startDateField = new DatePicker(LocalDate.now());
    private final Spinner<Integer> startHourField = hourSpinner(8);
    private final Spinner<Integer> startMinuteField = minuteSpinner(0);
    private final DatePicker endDateField = new DatePicker(LocalDate.now());
    private final Spinner<Integer> endHourField = hourSpinner(10);
    private final Spinner<Integer> endMinuteField = minuteSpinner(0);
    private final TextArea descriptionField = new TextArea();
    private final VBox waypointRows = new VBox(4);
    private final VBox phaseRows = new VBox(4);
    private final VBox resourceRows = new VBox(4);
    private final Label errorLabel = new Label();
    private final Button saveButton;

    private MissionFormDialog(Window owner, ApiClient apiClient, MissionDto existing, Runnable onSaved) {
        this.apiClient = apiClient;
        this.existing = existing;
        this.onSaved = onSaved;
        this.saveButton = new Button(existing == null ? "Crear misión" : "Guardar cambios");
        this.stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(existing == null ? "Nueva misión" : "Editar misión");
        stage.setScene(new Scene(buildRoot(), 620, 640));
    }

    static void showCreate(Window owner, ApiClient apiClient, Runnable onSaved) {
        new MissionFormDialog(owner, apiClient, null, onSaved).stage.show();
    }

    static void showEdit(Window owner, ApiClient apiClient, MissionDto existing, Runnable onSaved) {
        new MissionFormDialog(owner, apiClient, existing, onSaved).stage.show();
    }

    private javafx.scene.layout.Region buildRoot() {
        if (existing != null) {
            prefill(existing);
        } else {
            typeField.getSelectionModel().selectFirst();
        }

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        ColumnConstraints label = new ColumnConstraints();
        ColumnConstraints value = new ColumnConstraints();
        value.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        form.getColumnConstraints().addAll(label, value);

        int row = 0;
        form.addRow(row++, new Label("Nombre"), nameField);
        form.addRow(row++, new Label("Tipo"), typeField);
        form.addRow(row++, new Label("Inicio"), new HBox(6, startDateField, startHourField, new Label("h"), startMinuteField, new Label("min (UTC)")));
        form.addRow(row++, new Label("Fin"), new HBox(6, endDateField, endHourField, new Label("h"), endMinuteField, new Label("min (UTC)")));
        descriptionField.setPrefRowCount(2);
        form.addRow(row++, new Label("Descripción"), descriptionField);

        VBox root = new VBox(12,
                form,
                sectionLabel("Waypoints"),
                waypointRows,
                addButton("+ Waypoint", () -> waypointRows.getChildren().add(newWaypointRow(null))),
                sectionLabel("Fases"),
                phaseRows,
                addButton("+ Fase", () -> phaseRows.getChildren().add(newPhaseRow(null))),
                sectionLabel("Escoltas"),
                resourceRows,
                addButton("+ Escolta", () -> resourceRows.getChildren().add(newResourceRow(null))));
        root.setPadding(new Insets(12));

        if (existing == null) {
            waypointRows.getChildren().add(newWaypointRow(null));
        }

        errorLabel.setStyle("-fx-text-fill: #c62828;");
        errorLabel.setWrapText(true);

        Button cancelButton = new Button("Cancelar");
        cancelButton.setOnAction(e -> stage.close());
        saveButton.setOnAction(e -> submit());
        HBox buttons = new HBox(8, saveButton, cancelButton);

        VBox outer = new VBox(10, root, errorLabel, buttons);
        outer.setPadding(new Insets(12));
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(outer);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private void prefill(MissionDto mission) {
        nameField.setText(mission.name());
        typeField.getSelectionModel().select(MissionType.valueOf(mission.type()));
        descriptionField.setText(mission.description());

        LocalDateTime start = LocalDateTime.ofInstant(mission.startTime(), ZoneOffset.UTC);
        startDateField.setValue(start.toLocalDate());
        startHourField.getValueFactory().setValue(start.getHour());
        startMinuteField.getValueFactory().setValue(start.getMinute());

        LocalDateTime end = LocalDateTime.ofInstant(mission.endTime(), ZoneOffset.UTC);
        endDateField.setValue(end.toLocalDate());
        endHourField.getValueFactory().setValue(end.getHour());
        endMinuteField.getValueFactory().setValue(end.getMinute());

        for (WaypointDto w : mission.waypoints()) {
            waypointRows.getChildren().add(newWaypointRow(w));
        }
        for (PhaseDto p : mission.phases()) {
            phaseRows.getChildren().add(newPhaseRow(p));
        }
        for (ResourceDto r : mission.resources()) {
            resourceRows.getChildren().add(newResourceRow(r));
        }
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private Button addButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(e -> action.run());
        return button;
    }

    private HBox newWaypointRow(WaypointDto existingWaypoint) {
        TextField lat = new TextField();
        lat.setPromptText("lat");
        lat.setPrefWidth(80);
        TextField lon = new TextField();
        lon.setPromptText("lon");
        lon.setPrefWidth(80);
        ComboBox<TaskType> taskType = new ComboBox<>(FXCollections.observableArrayList(TaskType.values()));
        TextField notes = new TextField();
        notes.setPromptText("notas");
        HBox.setHgrow(notes, javafx.scene.layout.Priority.ALWAYS);

        if (existingWaypoint != null) {
            lat.setText(String.valueOf(existingWaypoint.latitude()));
            lon.setText(String.valueOf(existingWaypoint.longitude()));
            taskType.getSelectionModel().select(TaskType.valueOf(existingWaypoint.taskType()));
            notes.setText(existingWaypoint.notes());
        } else {
            taskType.getSelectionModel().selectFirst();
        }

        HBox row = new HBox(6, lat, lon, taskType, notes);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(new WaypointFields(lat, lon, taskType, notes));
        Button remove = new Button("✕");
        remove.setOnAction(e -> waypointRows.getChildren().remove(row));
        row.getChildren().add(remove);
        return row;
    }

    private HBox newPhaseRow(PhaseDto existingPhase) {
        TextField name = new TextField();
        name.setPromptText("nombre");
        TextField start = new TextField();
        start.setPromptText("min inicio");
        start.setPrefWidth(80);
        TextField end = new TextField();
        end.setPromptText("min fin");
        end.setPrefWidth(80);
        TextField notes = new TextField();
        notes.setPromptText("notas");
        HBox.setHgrow(notes, javafx.scene.layout.Priority.ALWAYS);

        if (existingPhase != null) {
            name.setText(existingPhase.name());
            start.setText(String.valueOf(existingPhase.startOffsetMinutes()));
            end.setText(String.valueOf(existingPhase.endOffsetMinutes()));
            notes.setText(existingPhase.notes());
        }

        HBox row = new HBox(6, name, start, end, notes);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(new PhaseFields(name, start, end, notes));
        Button remove = new Button("✕");
        remove.setOnAction(e -> phaseRows.getChildren().remove(row));
        row.getChildren().add(remove);
        return row;
    }

    private HBox newResourceRow(ResourceDto existingResource) {
        TextField name = new TextField();
        name.setPromptText("nombre");
        ComboBox<ResourceType> type = new ComboBox<>(FXCollections.observableArrayList(ResourceType.values()));
        TextField callSign = new TextField();
        callSign.setPromptText("indicativo");
        HBox.setHgrow(callSign, javafx.scene.layout.Priority.ALWAYS);

        if (existingResource != null) {
            name.setText(existingResource.name());
            type.getSelectionModel().select(ResourceType.valueOf(existingResource.type()));
            callSign.setText(existingResource.callSign());
        } else {
            type.getSelectionModel().selectFirst();
        }

        HBox row = new HBox(6, name, type, callSign);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(new ResourceFields(name, type, callSign));
        Button remove = new Button("✕");
        remove.setOnAction(e -> resourceRows.getChildren().remove(row));
        row.getChildren().add(remove);
        return row;
    }

    private void submit() {
        errorLabel.setText("");

        if (nameField.getText() == null || nameField.getText().isBlank()) {
            errorLabel.setText("El nombre no puede estar en blanco.");
            return;
        }

        Instant startTime;
        Instant endTime;
        try {
            startTime = toInstant(startDateField.getValue(), startHourField.getValue(), startMinuteField.getValue());
            endTime = toInstant(endDateField.getValue(), endHourField.getValue(), endMinuteField.getValue());
        } catch (Exception e) {
            errorLabel.setText("Fecha/hora de inicio o fin no válida.");
            return;
        }

        List<CreateWaypointRequestDto> waypoints = new ArrayList<>();
        int sequence = 1;
        for (javafx.scene.Node node : waypointRows.getChildren()) {
            WaypointFields fields = (WaypointFields) node.getUserData();
            try {
                waypoints.add(new CreateWaypointRequestDto(
                        sequence++,
                        Double.parseDouble(fields.lat.getText().trim()),
                        Double.parseDouble(fields.lon.getText().trim()),
                        fields.taskType.getValue().name(),
                        fields.notes.getText()));
            } catch (NumberFormatException e) {
                errorLabel.setText("Latitud/longitud no válida en uno de los waypoints.");
                return;
            }
        }

        List<CreatePhaseRequestDto> phases = new ArrayList<>();
        for (javafx.scene.Node node : phaseRows.getChildren()) {
            PhaseFields fields = (PhaseFields) node.getUserData();
            if (fields.name.getText() == null || fields.name.getText().isBlank()) {
                continue;
            }
            try {
                phases.add(new CreatePhaseRequestDto(
                        fields.name.getText(),
                        Integer.parseInt(fields.startOffset.getText().trim()),
                        Integer.parseInt(fields.endOffset.getText().trim()),
                        fields.notes.getText()));
            } catch (NumberFormatException e) {
                errorLabel.setText("Los minutos de inicio/fin de fase deben ser números.");
                return;
            }
        }

        List<CreateResourceRequestDto> resources = new ArrayList<>();
        for (javafx.scene.Node node : resourceRows.getChildren()) {
            ResourceFields fields = (ResourceFields) node.getUserData();
            if (fields.name.getText() == null || fields.name.getText().isBlank()) {
                continue;
            }
            resources.add(new CreateResourceRequestDto(fields.name.getText(), fields.type.getValue().name(), fields.callSign.getText()));
        }

        CreateMissionRequestDto request = new CreateMissionRequestDto(
                nameField.getText(), typeField.getValue().name(), startTime, endTime,
                descriptionField.getText(), waypoints, phases, resources);

        saveButton.setDisable(true);
        Task<MissionDto> saveTask = new Task<>() {
            @Override
            protected MissionDto call() throws Exception {
                return existing == null ? apiClient.createMission(request) : apiClient.updateMission(existing.id(), request);
            }
        };
        saveTask.setOnSucceeded(e -> {
            stage.close();
            onSaved.run();
        });
        saveTask.setOnFailed(e -> Platform.runLater(() -> {
            saveButton.setDisable(false);
            errorLabel.setText(saveTask.getException() != null
                    ? saveTask.getException().getMessage() : "No se pudo guardar la misión.");
        }));
        new Thread(saveTask, "save-mission").start();
    }

    private Instant toInstant(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute)).toInstant(ZoneOffset.UTC);
    }

    private static Spinner<Integer> hourSpinner(int initial) {
        Spinner<Integer> spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, initial));
        spinner.setEditable(true);
        spinner.setPrefWidth(70);
        return spinner;
    }

    private static Spinner<Integer> minuteSpinner(int initial) {
        Spinner<Integer> spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, initial));
        spinner.setEditable(true);
        spinner.setPrefWidth(70);
        return spinner;
    }

    private record WaypointFields(TextField lat, TextField lon, ComboBox<TaskType> taskType, TextField notes) {
    }

    private record PhaseFields(TextField name, TextField startOffset, TextField endOffset, TextField notes) {
    }

    private record ResourceFields(TextField name, ComboBox<ResourceType> type, TextField callSign) {
    }
}
