package com.albertonavas.missionbriefing.clientfx;

import com.albertonavas.missionbriefing.clientfx.dto.GeoPointDto;
import com.albertonavas.missionbriefing.clientfx.dto.MissionDto;
import com.albertonavas.missionbriefing.clientfx.dto.RiskZoneDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/** Cliente HTTP fino sobre la API REST de mission-server. Sin frameworks: HttpClient del JDK + Jackson. */
public class ApiClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<MissionDto> listMissions() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/missions");
        return mapper.readValue(response.body(), new TypeReference<List<MissionDto>>() {
        });
    }

    public MissionDto getMission(long id) throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/missions/" + id);
        return mapper.readValue(response.body(), MissionDto.class);
    }

    public List<RiskZoneDto> listRiskZones() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/risk-zones");
        return mapper.readValue(response.body(), new TypeReference<List<RiskZoneDto>>() {
        });
    }

    /** Ruta real por carretera (OSRM). Lanza IOException si el servicio de rutas falla o no responde 200. */
    public List<GeoPointDto> getRoadRoute(long missionId) throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/missions/" + missionId + "/road-route");
        if (response.statusCode() != 200) {
            throw new IOException("road-route respondio HTTP " + response.statusCode());
        }
        return mapper.readValue(response.body(), new TypeReference<List<GeoPointDto>>() {
        });
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
