package nl.tivek.multiversepowers.engine.rig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A skeleton: bones in order, each after its parent, each at an offset in its parent's frame (model units) with the
// joint it turns on there. A bone without a parent hangs on the frame the rig is posed on.
public final class Rig {
    private final String[] names;
    private final int[] parents;
    private final double[] offsets;
    private final Joint[] joints;
    private final int[] firstValue;
    private final int values;
    private final Map<String, Integer> byName;

    private Rig(Builder builder) {
        int size = builder.names.size();
        this.names = builder.names.toArray(String[]::new);
        this.parents = new int[size];
        this.offsets = new double[size * 3];
        this.joints = builder.joints.toArray(Joint[]::new);
        this.firstValue = new int[size + 1];
        for (int b = 0; b < size; b++) {
            this.parents[b] = builder.parents.get(b);
            System.arraycopy(builder.offsets.get(b), 0, this.offsets, b * 3, 3);
            this.firstValue[b + 1] = this.firstValue[b] + this.joints[b].size();
        }
        this.values = this.firstValue[size];
        this.byName = Map.copyOf(builder.byName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public int size() {
        return this.names.length;
    }

    public String name(int bone) {
        return this.names[bone];
    }

    public int index(String name) {
        Integer bone = this.byName.get(name);
        if (bone == null) {
            throw new IllegalArgumentException("No bone named " + name);
        }
        return bone;
    }

    public int parent(int bone) {
        return this.parents[bone];
    }

    public double offsetX(int bone) {
        return this.offsets[bone * 3];
    }

    public double offsetY(int bone) {
        return this.offsets[bone * 3 + 1];
    }

    public double offsetZ(int bone) {
        return this.offsets[bone * 3 + 2];
    }

    public Joint joint(int bone) {
        return this.joints[bone];
    }

    public int values() {
        return this.values;
    }

    public int value(int bone, int turn) {
        if (turn < 0 || turn >= this.joints[bone].size()) {
            throw new IndexOutOfBoundsException("Bone " + this.names[bone] + " has no turn " + turn);
        }
        return this.firstValue[bone] + turn;
    }

    public static final class Builder {
        private final List<String> names = new ArrayList<>();
        private final List<Integer> parents = new ArrayList<>();
        private final List<double[]> offsets = new ArrayList<>();
        private final List<Joint> joints = new ArrayList<>();
        private final Map<String, Integer> byName = new HashMap<>();

        private Builder() {
        }

        public int bone(String name, int parent, double x, double y, double z, Joint joint) {
            if (this.byName.containsKey(name)) {
                throw new IllegalArgumentException("Two bones named " + name);
            }
            if (parent < -1 || parent >= this.names.size()) {
                throw new IllegalArgumentException("Bone " + name + " comes before its parent " + parent);
            }
            int index = this.names.size();
            this.names.add(name);
            this.parents.add(parent);
            this.offsets.add(new double[] { x, y, z });
            this.joints.add(joint);
            this.byName.put(name, index);
            return index;
        }

        public Rig build() {
            return new Rig(this);
        }
    }
}
