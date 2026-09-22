package nl.tivek.welcomescreen.spell;

import com.mojang.math.Transformation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import net.minecraft.world.entity.Display;

public final class DisplayHelper {
    private static final MethodHandle SET_TRANSFORMATION;
    private static final MethodHandle SET_BILLBOARD;
    private static final MethodHandle SET_VIEW_RANGE;

    static {
        MethodHandle setTransform = null;
        MethodHandle setBillboard = null;
        MethodHandle setViewRange = null;
        try {
            Method m1 = Display.class.getDeclaredMethod("setTransformation", Transformation.class);
            m1.setAccessible(true);
            setTransform = MethodHandles.lookup().unreflect(m1);

            Method m2 = Display.class.getDeclaredMethod("setBillboardConstraints", Display.BillboardConstraints.class);
            m2.setAccessible(true);
            setBillboard = MethodHandles.lookup().unreflect(m2);

            Method m3 = Display.class.getDeclaredMethod("setViewRange", float.class);
            m3.setAccessible(true);
            setViewRange = MethodHandles.lookup().unreflect(m3);
        } catch (Exception ignored) {
        }
        SET_TRANSFORMATION = setTransform;
        SET_BILLBOARD = setBillboard;
        SET_VIEW_RANGE = setViewRange;
    }

    private DisplayHelper() {
    }

    public static void setTransformation(Display display, Transformation transformation) {
        if (SET_TRANSFORMATION != null) {
            try {
                SET_TRANSFORMATION.invoke(display, transformation);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void setBillboard(Display display, Display.BillboardConstraints billboard) {
        if (SET_BILLBOARD != null) {
            try {
                SET_BILLBOARD.invoke(display, billboard);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void setViewRange(Display display, float viewRange) {
        if (SET_VIEW_RANGE != null) {
            try {
                SET_VIEW_RANGE.invoke(display, viewRange);
            } catch (Throwable ignored) {
            }
        }
    }
}
