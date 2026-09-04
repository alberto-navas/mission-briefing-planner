package com.albertonavas.missionbriefing.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/** Una fase del cronograma de una mision, en minutos relativos al inicio. */
@Entity
@Table(name = "mission_phases")
public class MissionPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "mission_id")
    private Mission mission;

    private String name;
    private int startOffsetMinutes;
    private int endOffsetMinutes;
    private String notes;

    protected MissionPhase() {
        // requerido por JPA
    }

    public MissionPhase(String name, int startOffsetMinutes, int endOffsetMinutes, String notes) {
        this.name = name;
        this.startOffsetMinutes = startOffsetMinutes;
        this.endOffsetMinutes = endOffsetMinutes;
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

    public String getName() {
        return name;
    }

    public int getStartOffsetMinutes() {
        return startOffsetMinutes;
    }

    public int getEndOffsetMinutes() {
        return endOffsetMinutes;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MissionPhase other)) {
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
        return "MissionPhase{id=%s, name='%s', %d-%d min}"
                .formatted(id, name, startOffsetMinutes, endOffsetMinutes);
    }
}
