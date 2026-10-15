package dev.windblown.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShaderPatches {
    public static final String VANILLA_GLOBALS = "vanilla-globals";
    public static final String SODIUM_GLOBALS = "sodium-globals";
    public static final String VANILLA_TERRAIN = "vanilla-terrain";
    public static final String SODIUM_TERRAIN = "sodium-terrain";

    private static final Logger LOGGER = LogManager.getLogger(WindblownClient.MOD_ID);
    private static final String FIELDS = "\n    vec4 WindblownInfo;\n    vec4 WindblownCamera;\n    vec4 WindblownWind;";
    private static final String PAD = "\n    vec4 WindblownPad[65];";
    private static final String FIRELIGHT_FIELDS = "vec4FirelightInfo;vec4FirelightSources[64];";
    private static final String DEFINE = "#define WINDBLOWN_GLOBALS\n";
    private static final String RAW = "uniform vec4 WindblownInfo;\nuniform vec4 WindblownCamera;\nuniform vec4 WindblownWind;\n#define WINDBLOWN_GLOBALS\n";

    private record Block(String name, String layout) {
    }

    private record Terrain(String name, Pattern input, String color, Pattern position, String sky, boolean sodium) {
    }

    private static final List<Block> BLOCKS = List.of(
            new Block(VANILLA_GLOBALS, "layout(std140)uniformGlobals{ivec3CameraBlockPos;floatGlintAlpha;vec3CameraOffset;"
                    + "floatGameTime;vec2ScreenSize;intMenuBlurRadius;intUseRgss;"),
            new Block(VANILLA_GLOBALS, "layout(std140)uniformGlobals{ivec3CameraBlockPos;vec3CameraOffset;vec2ScreenSize;"
                    + "floatGlintAlpha;floatGameTime;intMenuBlurRadius;intUseRgss;"),
            new Block(SODIUM_GLOBALS, "layout(std140)uniformu_Globals{mat4u_ProjectionMatrix;mat4u_ModelViewMatrix;"
                    + "vec4u_FogColor;vec2u_EnvironmentFog;vec2u_RenderFog;vec2u_TexelSize;vec2u_TexCoordShrink;floatu_FadePeriodInv;boolu_UseRGSS;"));

    private static final List<Terrain> TERRAINS = List.of(
            new Terrain(VANILLA_TERRAIN, Pattern.compile("\\bin\\s+vec4\\s+Color\\s*;"), "windblown_Color",
                    Pattern.compile("vec3\\s+(pos)\\s*=\\s*Position\\b[^;]*;"), "float(UV2.y) / 240.0", false),
            new Terrain(SODIUM_TERRAIN, null, "_vert_color",
                    Pattern.compile("vec3\\s+(position)\\s*=[^;]*\\b_vert_position\\b[^;]*;"), "_vert_tex_light_coord.y * 256.0 / 240.0", true));

    private static final String COLOR = "\nvec4 windblown_rawColor() {\n    return Color;\n}\nvec4 windblown_Color;\n#define Color windblown_Color\n";

    private static final Pattern CHUNK = Pattern.compile("\\bPosition\\s*\\+\\s*\\(?\\s*(ChunkOffset|ModelOffset|ChunkPosition)\\b");
    private static final Pattern LIGHT = Pattern.compile("\\bin\\s+ivec2\\s+UV2\\s*;");

    private static final Set<String> APPLIED = ConcurrentHashMap.newKeySet();
    private static String code;

    private ShaderPatches() {
    }

    public static boolean applied(String name) {
        return APPLIED.contains(name);
    }

    public static String patch(String name, String source) {
        if (source == null) {
            return null;
        }
        try {
            String patched = source.contains("WindblownInfo;") ? source : globals(source);
            return patched.contains("windblown_offset(") ? patched : terrain(name, patched);
        } catch (RuntimeException | IOException e) {
            LOGGER.warn("Windblown could not patch {}", name, e);
            return source;
        }
    }

    private static String globals(String source) {
        int search = 0;
        while (true) {
            int start = source.indexOf("layout(std140)", search);
            if (start < 0) {
                return source;
            }
            int close = source.indexOf("};", start);
            if (close < 0) {
                return source;
            }
            String squashed = squash(source.substring(start, close));
            search = close + 2;
            boolean firelight = squashed.endsWith(FIRELIGHT_FIELDS);
            String own = firelight ? squashed.substring(0, squashed.length() - FIRELIGHT_FIELDS.length()) : squashed;
            for (Block block : BLOCKS) {
                if (own.equals(block.layout()) && !(Backend.RAW_UNIFORMS && block.name().equals(VANILLA_GLOBALS))) {
                    String withFields = source.substring(0, close).stripTrailing() + (firelight ? "" : PAD) + FIELDS + "\n"
                            + source.substring(close);
                    APPLIED.add(block.name());
                    if (firelight) {
                        APPLIED.add(block.name() + "-firelight");
                    }
                    if (Boolean.getBoolean("windblown.debug")) {
                        LOGGER.info("Windblown patched globals:{}{}", System.lineSeparator(), withFields.substring(0, start) + DEFINE + withFields.substring(start));
                    }
                    return withFields.substring(0, start) + DEFINE + withFields.substring(start);
                }
            }
            if (!Backend.RAW_UNIFORMS && (squashed.startsWith("layout(std140)uniformGlobals{") || squashed.startsWith("layout(std140)uniformu_Globals{"))) {
                LOGGER.warn("Windblown does not know this layout of the shader globals; plants will not move: {}", squashed);
            }
        }
    }

    private static String terrain(String name, String source) throws IOException {
        for (Terrain terrain : TERRAINS) {
            if (terrain.sodium() && !source.contains("_vert_tex_light_coord")) {
                continue;
            }
            Matcher position = terrain.position().matcher(source);
            if (!position.find()) {
                continue;
            }
            Matcher input = terrain.input() == null ? null : terrain.input().matcher(source);
            if (input != null && (!input.find() || !terrainLike(name, source))) {
                continue;
            }
            int main = source.indexOf("void main()");
            int body = main < 0 ? -1 : source.indexOf('{', main);
            if (body < 0 || position.start() < body || (input != null && input.start() > main)) {
                continue;
            }
            boolean raw = terrain.sodium() ? !usesSodiumGlobals(source) : Backend.RAW_UNIFORMS;
            String variable = position.group(1);
            String alpha = terrain.color() + ".a";
            String sky = terrain.sodium() && source.contains("_sample_lightmap(u_LightTex") ? "float(_vert_tex_light_coord.y) / 240.0" : terrain.sky();
            String move = "\n    " + variable + " += windblown_offset(" + variable + ", " + alpha + ", clamp(" + sky + ", 0.0, 1.0));"
                    + "\n    " + terrain.color() + ".a = windblown_alpha(" + alpha + ");";
            String header = (raw ? RAW : "") + code() + "\n";

            StringBuilder out = new StringBuilder(source);
            out.insert(position.end(), move);
            if (input != null) {
                out.insert(body + 1, "\n    windblown_Color = windblown_rawColor();");
            }
            out.insert(main, header);
            if (input != null) {
                out.insert(input.end(), COLOR);
            }
            APPLIED.add(terrain.name());
            if (raw) {
                APPLIED.add(terrain.name() + "-raw");
                RawUniforms.shadersChanged();
            }
            if (Boolean.getBoolean("windblown.debug")) {
                LOGGER.info("Windblown patched {}:{}{}", name, System.lineSeparator(), out);
            }
            return out.toString();
        }
        return source;
    }

    public static String legacyTerrain(String version, String body) throws IOException {
        APPLIED.add(VANILLA_TERRAIN);
        APPLIED.add(VANILLA_TERRAIN + "-raw");
        return version + RAW + code() + "\n" + body;
    }

    private static boolean terrainLike(String name, String source) {
        return name.contains("terrain") || (CHUNK.matcher(source).find() && LIGHT.matcher(source).find());
    }

    private static boolean usesSodiumGlobals(String source) {
        return source.contains("sodium:include/globals.glsl") || source.contains("sodium:globals.glsl") || source.contains("uniform u_Globals");
    }

    private static String code() throws IOException {
        if (code == null) {
            try (InputStream in = ShaderPatches.class.getResourceAsStream("/assets/windblown/glsl/wind.glsl")) {
                if (in == null) {
                    throw new IOException("wind.glsl is missing from the jar");
                }
                code = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return code;
    }

    private static String squash(String text) {
        return text.replaceAll("//[^\n]*", "").replaceAll("\\s+", "");
    }
}
