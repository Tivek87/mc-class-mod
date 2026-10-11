package nl.tivek.multiversepowers.schematic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.schematic.format.Packed;
import org.junit.jupiter.api.Test;

class SchematicMathTest {
    @Test
    void varintsComeBackAsWritten() {
        Random random = new Random(7);
        int[] values = new int[500];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int k = 0; k < values.length; k++) {
            values[k] = k < 3 ? new int[] { 0, 127, 128 }[k] : random.nextInt(1 << random.nextInt(31));
            int value = values[k];
            while ((value & ~0x7F) != 0) {
                out.write(value & 0x7F | 0x80);
                value >>>= 7;
            }
            out.write(value);
        }
        byte[] bytes = out.toByteArray();
        assertArrayEquals(values, Packed.varints(bytes, values.length));
        assertNull(Packed.varints(bytes, values.length + 1));
    }

    @Test
    void bitsAsLitematicaGivesThem() {
        assertEquals(2, Packed.bits(1));
        assertEquals(2, Packed.bits(4));
        assertEquals(3, Packed.bits(5));
        assertEquals(8, Packed.bits(256));
        assertEquals(9, Packed.bits(257));
    }

    @Test
    void tightCellsComeBackAsPacked() {
        Random random = new Random(11);
        for (int bits : new int[] { 2, 3, 5, 7, 12, 17 }) {
            int count = 1000;
            int[] values = new int[count];
            long[] longs = new long[(int) ((count * (long) bits + 63) / 64)];
            for (int k = 0; k < count; k++) {
                values[k] = random.nextInt(1 << bits);
                long start = (long) k * bits;
                int first = (int) (start >> 6);
                int offset = (int) (start & 63);
                longs[first] |= (long) values[k] << offset;
                if (offset + bits > 64) {
                    longs[first + 1] |= (long) values[k] >>> (64 - offset);
                }
            }
            for (int k = 0; k < count; k++) {
                assertEquals(values[k], Packed.tight(longs, bits, k), "bits " + bits + " cell " + k);
            }
            assertEquals(-1, Packed.tight(longs, bits, longs.length * 64L / bits + 1));
        }
    }

    // Every cell of the turned box comes from one cell of the schematic, and that cell's middle lands in it.
    @Test
    void placementTurnsEveryCellOnce() {
        Schematic schematic = new Schematic("box", 5, 2, 3, List.of());
        BlockPos origin = new BlockPos(10, 64, -20);
        for (Rotation rotation : Rotation.values()) {
            for (Mirror mirror : Mirror.values()) {
                SchematicPlacement placement = new SchematicPlacement(origin, rotation, mirror);
                int su = placement.sizeX(schematic);
                int sw = placement.sizeZ(schematic);
                boolean[] seen = new boolean[schematic.sizeX * schematic.sizeZ];
                int[] from = new int[2];
                for (int u = 0; u < su; u++) {
                    for (int w = 0; w < sw; w++) {
                        placement.source(schematic, u, w, from);
                        assertTrue(from[0] >= 0 && from[0] < schematic.sizeX && from[1] >= 0
                                && from[1] < schematic.sizeZ, rotation + " " + mirror);
                        int cell = from[1] * schematic.sizeX + from[0];
                        assertTrue(!seen[cell], rotation + " " + mirror + " twice");
                        seen[cell] = true;
                        Vec3 lands = placement.world(schematic, new Vec3(from[0] + 0.5, 1.5, from[1] + 0.5));
                        assertEquals(origin.getX() + u + 0.5, lands.x, 1e-9, rotation + " " + mirror);
                        assertEquals(origin.getY() + 1.5, lands.y, 1e-9);
                        assertEquals(origin.getZ() + w + 0.5, lands.z, 1e-9, rotation + " " + mirror);
                    }
                }
            }
        }
    }

    // A quarter turn right, as the game turns its own structures: the schematic's +x runs to the world's +z.
    @Test
    void quarterRightAsTheGameTurns() {
        Schematic schematic = new Schematic("row", 4, 1, 1, List.of());
        SchematicPlacement placement = new SchematicPlacement(BlockPos.ZERO, Rotation.CLOCKWISE_90, Mirror.NONE);
        Vec3 start = placement.world(schematic, new Vec3(0.5, 0.0, 0.5));
        Vec3 end = placement.world(schematic, new Vec3(3.5, 0.0, 0.5));
        assertEquals(3.0, end.z - start.z, 1e-9);
        assertEquals(0.0, end.x - start.x, 1e-9);
    }
}
