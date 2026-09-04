package com.albertonavas.missionbriefing.clientfx;

import com.albertonavas.missionbriefing.clientfx.dto.MissionDto;
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

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
