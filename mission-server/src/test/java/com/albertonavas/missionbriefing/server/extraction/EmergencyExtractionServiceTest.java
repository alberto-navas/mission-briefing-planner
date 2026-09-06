package com.albertonavas.missionbriefing.server.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.albertonavas.missionbriefing.server.route.GeoPoint;
import com.albertonavas.missionbriefing.server.route.OsrmRouteClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmergencyExtractionServiceTest {

    @Mock
    private OsrmRouteClient osrmRouteClient;

    @Test
    void routesToTheClosestExtractionPointNotJustTheFirstInTheCatalog() {
        // "La Linea de la Concepcion" (36.169, -5.349) esta mucho mas cerca de este punto
        // que "Puerto de Tarifa" (36.0128, -5.6058), aunque Tarifa sea el primero del catalogo.
        double fromLat = 36.15;
        double fromLon = -5.35;

        when(osrmRouteClient.fetchRoute(fromLat, fromLon, 36.169, -5.349))
                .thenReturn(List.of(new GeoPoint(fromLat, fromLon), new GeoPoint(36.169, -5.349)));

        EmergencyExtractionService service = new EmergencyExtractionService(new ExtractionPointCatalog(), osrmRouteClient);
        NearestExtractionResult result = service.routeToNearestExtraction(fromLat, fromLon);

        assertThat(result.point().id()).isEqualTo("ep2");
        assertThat(result.route()).hasSize(2);
    }
}
