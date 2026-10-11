package nl.tivek.multiversepowers.schematic.format;

// How the formats pack their cells: Sponge's palette ids as variable-length numbers, Litematica's packed tight across
// longs, never padded at a long's end.
public final class Packed {
    private Packed() {
    }

    // Every cell's id from a run of variable-length numbers; null when the run is cut short or a number is too long.
    public static int[] varints(byte[] bytes, int count) {
        int[] out = new int[count];
        int at = 0;
        for (int i = 0; i < count; i++) {
            int value = 0;
            int shift = 0;
            while (true) {
                if (at >= bytes.length || shift > 28) {
                    return null;
                }
                byte b = bytes[at++];
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    break;
                }
                shift += 7;
            }
            out[i] = value;
        }
        return out;
    }

    // The bits Litematica gives each cell for a palette of `size`.
    public static int bits(int size) {
        return Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(size - 1));
    }

    // Cell `index` of a Litematica array of `bits` per cell; -1 past its end.
    public static int tight(long[] longs, int bits, long index) {
        long start = index * bits;
        int first = (int) (start >> 6);
        int last = (int) ((start + bits - 1) >> 6);
        if (last >= longs.length) {
            return -1;
        }
        int offset = (int) (start & 63);
        long mask = (1L << bits) - 1L;
        if (first == last) {
            return (int) ((longs[first] >>> offset) & mask);
        }
        return (int) (((longs[first] >>> offset) | (longs[last] << (64 - offset))) & mask);
    }
}
