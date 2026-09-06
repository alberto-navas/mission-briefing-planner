package com.albertonavas.missionbriefing.server.route;

/** El servicio de rutas por carretera (OSRM) no respondio o devolvio un error. */
public class RoadRouteUnavailableException extends RuntimeException {

    public RoadRouteUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
