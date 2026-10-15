package dev.windblown.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindTest {
    @Test
    void codesRoundTrip() {
        for (int kind = 0; kind < 4; kind++) {
            for (int level = 0; level <= WindCode.LEVELS; level++) {
                float weight = level / (float) WindCode.LEVELS;
                int alpha = WindCode.alpha(kind, weight);
                assertTrue(alpha >= 191 && alpha <= 254, "alpha out of range: " + alpha);
                assertEquals(kind, WindCode.kind(alpha));
                assertEquals(weight, WindCode.weight(alpha), 1.0e-6f);
            }
        }
    }

    @Test
    void opaqueVerticesAreUntouched() {
        assertEquals(-1, WindCode.kind(255));
        assertEquals(-1, WindCode.kind(128));
        assertEquals(0f, WindCode.weight(255));
    }

    @Test
    void weightsAreClamped() {
        assertEquals(WindCode.alpha(WindCode.PLANT, 1f), WindCode.alpha(WindCode.PLANT, 3f));
        assertEquals(WindCode.alpha(WindCode.PLANT, 0f), WindCode.alpha(WindCode.PLANT, -2f));
        assertEquals(WindCode.alpha(WindCode.PLANT, 0f), WindCode.alpha(WindCode.PLANT, Float.NaN));
    }

    @Test
    void alphaKeepsColor() {
        int tinted = 0xFF48B518;
        int tagged = WindCode.withAlpha(tinted, WindCode.alpha(WindCode.LEAVES, 1f));
        assertEquals(tinted & 0xFFFFFF, tagged & 0xFFFFFF);
        assertEquals(WindCode.LEAVES, WindCode.kind(tagged >>> 24));
    }

    @Test
    void shadeFoldsIntoColor() {
        int folded = WindCode.foldShade(0x80FF4020, WindCode.NONE);
        assertEquals(WindCode.NONE, folded >>> 24);
        assertEquals(0x80, folded >> 16 & 0xFF);
        assertEquals(0x20, folded >> 8 & 0xFF);
        assertEquals(0x10, folded & 0xFF);
        assertEquals(0xFF123456, WindCode.foldShade(0xFF123456, 255));
    }

    @Test
    void gustsStayInRange() {
        for (double t = 0; t < 4000; t += 0.37) {
            float gust = Wind.gust(t);
            assertTrue(gust >= 0f && gust <= 1f, "gust out of range at " + t);
        }
    }

    @Test
    void stormsBlowHarder() {
        assertEquals(1f, Wind.weather(0f, 0f), 1.0e-6f);
        assertTrue(Wind.weather(1f, 0f) > 1.5f);
        assertTrue(Wind.weather(1f, 1f) > Wind.weather(1f, 0f));
        assertEquals(Wind.weather(1f, 1f), Wind.weather(5f, 5f), 1.0e-6f);
    }

    @Test
    void directionDriftsSlowly() {
        double step = Math.abs(Wind.angle(100.05) - Wind.angle(100.0));
        assertTrue(step < 0.002, "wind direction jumps: " + step);
    }

    @Test
    void wrapStaysContinuousInsideAPeriod() {
        assertEquals(10.5, Wind.wrap(10.5), 1.0e-9);
        assertEquals(Wind.WRAP - 1.5, Wind.wrap(-1.5), 1.0e-9);
        assertEquals(3.25, Wind.wrap(Wind.WRAP * 7 + 3.25), 1.0e-9);
    }
}
