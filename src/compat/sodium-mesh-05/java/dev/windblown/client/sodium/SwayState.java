package dev.windblown.client.sodium;

import dev.windblown.client.Foliage;

/**
 * The sway of the block Embeddium is currently rendering on this chunk-build thread.
 * BlockRendererMixin sets it for the length of {@code renderModel}; paths that buffer
 * vertices and flush them later inside that call (custom block renderers and the FRAPI
 * renderer used by Continuity, both via SinkingVertexBuilder) read it back here. On Fabric,
 * IndiumTerrainContextMixin sets it for Indium's {@code tessellateBlock} instead.
 */
public final class SwayState {
    private static final ThreadLocal<SwayState> CURRENT = ThreadLocal.withInitial(SwayState::new);

    public final Foliage.Sway sway = new Foliage.Sway();
    public boolean swaying;
    /** Block origin y in chunk space, for the Indium path where vertex y is chunk-relative. */
    public float originY;

    private SwayState() {
    }

    public static SwayState get() {
        return CURRENT.get();
    }
}
