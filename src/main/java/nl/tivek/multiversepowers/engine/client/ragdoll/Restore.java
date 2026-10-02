package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;

// A model is shared by every creature of its kind, and not every model sets all its parts again each frame: what a
// ragdoll changes for one creature is put back as it was once that creature is drawn.
final class Restore {
    private static final int STRIDE = 10;

    private final List<ModelPart> parts = new ArrayList<>();
    private float[] values = new float[STRIDE * 32];

    void keep(ModelPart part) {
        for (ModelPart kept : this.parts) {
            if (kept == part) {
                return;
            }
        }
        int o = this.parts.size() * STRIDE;
        if (o + STRIDE > this.values.length) {
            this.values = Arrays.copyOf(this.values, this.values.length * 2);
        }
        this.parts.add(part);
        float[] v = this.values;
        v[o] = part.x;
        v[o + 1] = part.y;
        v[o + 2] = part.z;
        v[o + 3] = part.xRot;
        v[o + 4] = part.yRot;
        v[o + 5] = part.zRot;
        v[o + 6] = part.xScale;
        v[o + 7] = part.yScale;
        v[o + 8] = part.zScale;
        v[o + 9] = part.visible ? 1.0F : 0.0F;
    }

    boolean any() {
        return !this.parts.isEmpty();
    }

    // How the part was turned and placed before it was changed (as ModelPart.translateAndRotate puts it, in blocks);
    // false when it was not changed.
    boolean was(ModelPart part, Matrix4f out) {
        for (int i = 0; i < this.parts.size(); i++) {
            if (this.parts.get(i) != part) {
                continue;
            }
            float[] v = this.values;
            int o = i * STRIDE;
            out.translation(v[o] / 16.0F, v[o + 1] / 16.0F, v[o + 2] / 16.0F).rotateZYX(v[o + 5], v[o + 4], v[o + 3])
                    .scale(v[o + 6], v[o + 7], v[o + 8]);
            return true;
        }
        return false;
    }

    void undo() {
        for (int i = 0; i < this.parts.size(); i++) {
            ModelPart part = this.parts.get(i);
            float[] v = this.values;
            int o = i * STRIDE;
            part.x = v[o];
            part.y = v[o + 1];
            part.z = v[o + 2];
            part.xRot = v[o + 3];
            part.yRot = v[o + 4];
            part.zRot = v[o + 5];
            part.xScale = v[o + 6];
            part.yScale = v[o + 7];
            part.zScale = v[o + 8];
            part.visible = v[o + 9] != 0.0F;
        }
        this.parts.clear();
    }
}
