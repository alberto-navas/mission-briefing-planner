package com.albertonavas.missionbriefing.server.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Cliente del servicio demo publico de OSRM (Open Source Routing Machine): dado un
 * origen y un destino, devuelve la geometria de una ruta real por carretera (no la
 * linea recta entre los dos puntos). Pensado para una demo; un despliegue real usaria
 * una instancia propia de OSRM o un proveedor de pago con SLA.
 */
@Component
public class OsrmRouteClient {

    private static final String BASE_URL = "http://router.project-osrm.org/route/v1/driving/";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<GeoPoint> fetchRoute(double fromLat, double fromLon, double toLat, double toLon) {
        String url = BASE_URL + "%s,%s;%s,%s?overview=full&geometries=geojson"
                .formatted(fmt(fromLon), fmt(fromLat), fmt(toLon), fmt(toLat));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RoadRouteUnavailableException("OSRM devolvio HTTP " + response.statusCode(), null);
            }
            return parseCoordinates(response.body());
        } catch (IOException e) {
            throw new RoadRouteUnavailableException("Fallo de red llamando a OSRM", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RoadRouteUnavailableException("Interrumpido esperando a OSRM", e);
        }
    }

    private List<GeoPoint> parseCoordinates(String body) throws IOException {
        JsonNode coordinates = objectMapper.readTree(body).at("/routes/0/geometry/coordinates");
        List<GeoPoint> points = new ArrayList<>();
        for (JsonNode coordinate : coordinates) {
            // GeoJSON es [longitud, latitud], al reves que el resto de este proyecto.
            points.add(new GeoPoint(coordinate.get(1).asDouble(), coordinate.get(0).asDouble()));
        }
        return points;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
