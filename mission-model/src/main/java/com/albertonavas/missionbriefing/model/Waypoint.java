package com.albertonavas.missionbriefing.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;

/** Un punto de la ruta de una mision, con la tarea que se realiza alli. */
@Entity
@Table(name = "waypoints")
public class Waypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "mission_id")
    private Mission mission;

    private int sequenceOrder;

    private double latitude;
    private double longitude;

    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    private String notes;

    protected Waypoint() {
        // requerido por JPA
    }

    public Waypoint(int sequenceOrder, double latitude, double longitude, TaskType taskType, String notes) {
        this.sequenceOrder = sequenceOrder;
        this.latitude = latitude;
        this.longitude = longitude;
        this.taskType = taskType;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public Mission getMission() {
        return mission;
    }

    void setMission(Mission mission) {
        this.mission = mission;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Waypoint other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "Waypoint{id=%s, order=%d, lat=%.5f, lon=%.5f, task=%s}",
                id, sequenceOrder, latitude, longitude, taskType);
    }
}
