package com.albertonavas.missionbriefing.clientfx;

import com.albertonavas.missionbriefing.clientfx.dto.CreateMissionRequestDto;
import com.albertonavas.missionbriefing.clientfx.dto.ExtractionPointDto;
import com.albertonavas.missionbriefing.clientfx.dto.GeoPointDto;
import com.albertonavas.missionbriefing.clientfx.dto.MissionDto;
import com.albertonavas.missionbriefing.clientfx.dto.NearestExtractionDto;
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
import java.util.Locale;

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

    public List<ExtractionPointDto> listExtractionPoints() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/extraction-points");
        return mapper.readValue(response.body(), new TypeReference<List<ExtractionPointDto>>() {
        });
    }

    /** Punto de extraccion mas cercano a (lat, lon) y la ruta real por carretera hasta el. */
    public NearestExtractionDto getNearestExtractionRoute(double lat, double lon) throws IOException, InterruptedException {
        String path = String.format(Locale.ROOT, "/api/extraction-points/nearest-route?lat=%.6f&lon=%.6f", lat, lon);
        HttpResponse<String> response = get(path);
        if (response.statusCode() != 200) {
            throw new IOException("nearest-route respondio HTTP " + response.statusCode());
        }
        return mapper.readValue(response.body(), NearestExtractionDto.class);
    }

    /**
     * Crea una mision. Lanza IOException con el cuerpo de la respuesta si el servidor la
     * rechaza (p.ej. validacion: nombre en blanco), para poder mostrarselo al usuario.
     */
    public MissionDto createMission(CreateMissionRequestDto request) throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/api/missions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IOException("El servidor rechazo la mision (HTTP %d): %s"
                    .formatted(response.statusCode(), response.body()));
        }
        return mapper.readValue(response.body(), MissionDto.class);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
