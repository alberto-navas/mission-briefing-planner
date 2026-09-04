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
import java.util.Objects;

/** Un activo o equipo propio (dron, vehiculo, embarcacion, equipo de personas) asignable a una mision. */
@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private ResourceType type;

    private String callSign;
    private boolean available = true;

    @ManyToOne
    @JoinColumn(name = "mission_id")
    private Mission mission;

    protected Resource() {
        // requerido por JPA
    }

    public Resource(String name, ResourceType type, String callSign) {
        this.name = name;
        this.type = type;
        this.callSign = callSign;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public String getCallSign() {
        return callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = callSign;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Mission getMission() {
        return mission;
    }

    public void assignTo(Mission mission) {
        this.mission = mission;
        this.available = mission == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resource other)) {
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
        return "Resource{id=%s, name='%s', type=%s, callSign='%s', available=%s}"
                .formatted(id, name, type, callSign, available);
    }
}
