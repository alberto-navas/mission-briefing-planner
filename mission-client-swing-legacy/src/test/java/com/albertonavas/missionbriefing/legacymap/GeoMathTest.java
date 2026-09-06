package com.albertonavas.missionbriefing.legacymap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeoMathTest {

    @Test
    void distanceBetweenSamePointIsZero() {
        assertThat(GeoMath.distanceMeters(36.0, -5.5, 36.0, -5.5)).isZero();
    }

    @Test
    void distanceMatchesKnownApproximateValue() {
        // Un grado de latitud son aproximadamente 111.32 km.
        double distance = GeoMath.distanceMeters(36.0, -5.5, 37.0, -5.5);

        assertThat(distance).isCloseTo(111_320, org.assertj.core.data.Offset.offset(2000.0));
    }
}
