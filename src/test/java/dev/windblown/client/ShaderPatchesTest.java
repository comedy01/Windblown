package dev.windblown.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderPatchesTest {
    @Test
    void commentedPositionLineIsSkipped() {
        // Shape of Litematica's legacy_terrain.vsh: a commented-out "vec3 pos = Position ..." comes first.
        String source = """
                #version 330
                in vec3 Position;
                in vec4 Color;
                in ivec2 UV2;
                void main() {
                    // vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
                    vec3 pos = Position + ModelOffset;
                    gl_Position = vec4(pos, 1.0);
                }
                """;
        String patched = ShaderPatches.patch("litematica:core/legacy_terrain", source);
        int declared = patched.indexOf("vec3 pos = Position + ModelOffset;");
        int moved = patched.indexOf("pos += windblown_offset(");
        assertTrue(moved > declared && declared > 0, patched);
        String commented = "// vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;\n";
        assertTrue(patched.contains(commented), "comment line must stay intact:\n" + patched);
    }

    @Test
    void blankCommentsKeepsOffsets() {
        String source = "a // b\nc /* d\ne */ f";
        String clean = ShaderPatches.blankComments(source);
        assertEquals(source.length(), clean.length());
        assertEquals("a     \nc     \n     f", clean);
    }

    @Test
    void sodiumBeta3GlobalsArePatched() {
        String source = """
                layout(std140) uniform u_Globals {
                    mat4 u_ProjectionMatrix;
                    mat4 u_ModelViewMatrix;

                    vec4 u_FogColor;
                    vec3 u_CameraOffset;
                    vec2 u_EnvironmentFog;
                    vec2 u_RenderFog;

                    vec2 u_TexelSize;
                    vec2 u_TexCoordShrink;

                    float u_FadePeriodInv;
                    bool u_UseRGSS;
                };
                """;
        String patched = ShaderPatches.patch("sodium", source);
        assertTrue(patched.contains("WindblownPad[65]") && patched.contains("WindblownWind"), patched);
        assertTrue(ShaderPatches.applied(ShaderPatches.SODIUM_GLOBALS));
        // std140: u_UseRGSS ends at 200, so the pad array starts at 208
        assertEquals(208, ShaderPatches.sodiumPadStart());
    }
}
