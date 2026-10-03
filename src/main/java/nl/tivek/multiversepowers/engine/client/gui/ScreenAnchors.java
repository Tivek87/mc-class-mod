package nl.tivek.multiversepowers.engine.client.gui;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import nl.tivek.multiversepowers.MultiversePowers;

// Where named parts of the screen were drawn this frame, in GUI units, for whatever points at them later in the same
// frame (a guided tour); a part not drawn this frame is not there.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ScreenAnchors {
    private static final Map<String, Rect> RECTS = new HashMap<>();
    private static long frame;

    public record Rect(float x, float y, float width, float height, long frame) {
        public float right() {
            return this.x + this.width;
        }

        public float bottom() {
            return this.y + this.height;
        }
    }

    private ScreenAnchors() {
    }

    public static void report(String id, float x, float y, float width, float height) {
        RECTS.put(id, new Rect(x, y, width, height, frame));
    }

    @Nullable
    public static Rect get(String id) {
        Rect rect = RECTS.get(id);
        return rect != null && rect.frame() == frame ? rect : null;
    }

    public static boolean shown(String id) {
        return get(id) != null;
    }

    @SubscribeEvent
    static void onFrame(RenderFrameEvent.Pre event) {
        frame++;
    }
}
