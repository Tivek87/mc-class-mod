package nl.tivek.multiversepowers.classes.ceremony;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.tivek.multiversepowers.classes.ceremony.Ceremonies.Ceremony;
import nl.tivek.multiversepowers.classes.ceremony.Ceremonies.Mode;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import org.joml.Vector3f;

final class Fx {
    private final ServerLevel level;
    private final Mode mode;
    private final double x;
    private final double y;
    private final double z;
    private final double forwardX;
    private final double forwardZ;
    private final BlockState ground;
    final int age;
    private final RandomSource random = RandomSource.create();

    Fx(ServerLevel level, Ceremony ceremony) {
        this.level = level;
        this.mode = ceremony.mode;
        this.x = ceremony.x;
        this.y = ceremony.y;
        this.z = ceremony.z;
        float rad = (float) Math.toRadians(ceremony.yaw);
        this.forwardX = -Math.sin(rad);
        this.forwardZ = Math.cos(rad);
        this.ground = ceremony.ground;
        this.age = ceremony.age;
    }

    boolean grand() {
        return this.mode.grand();
    }

    int age() {
        return this.age;
    }

    boolean every(int interval) {
        return this.age % interval == 0;
    }

    double span(int from, int to) {
        if (this.age < from || this.age >= to) {
            return -1;
        }
        return (double) (this.age - from) / (to - from);
    }

    double rand() {
        return this.random.nextDouble();
    }

    double spread(double range) {
        return (this.random.nextDouble() * 2.0 - 1.0) * range;
    }

    boolean chance(double probability) {
        return this.random.nextDouble() < probability;
    }

    private static Vector3f color(int rgb) {
        return new Vector3f(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
    }

    ParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(color(rgb), size);
    }

    ParticleOptions fade(int fromRgb, int toRgb, float size) {
        return new DustColorTransitionOptions(color(fromRgb), color(toRgb), size);
    }

    BlockState ground() {
        return this.ground.isAir() ? Blocks.DIRT.defaultBlockState() : this.ground;
    }

    double lx(double forward, double right) {
        return this.forwardX * forward - this.forwardZ * right;
    }

    double lz(double forward, double right) {
        return this.forwardZ * forward + this.forwardX * right;
    }

    void local(ParticleOptions particle, double forward, double up, double right) {
        this.at(particle, this.lx(forward, right), up, this.lz(forward, right));
    }

    void localLine(ParticleOptions particle, double forward1, double up1, double right1,
                           double forward2, double up2, double right2, double spacing, double keep) {
        this.line(particle, this.lx(forward1, right1), up1, this.lz(forward1, right1),
                this.lx(forward2, right2), up2, this.lz(forward2, right2), spacing, keep);
    }

    void glyphPoint(ParticleOptions particle, double ux, double uz, double radius, double dy, double rot) {
        double cos = Math.cos(rot);
        double sin = Math.sin(rot);
        double right = (ux * cos - uz * sin) * radius;
        double forward = (ux * sin + uz * cos) * radius;
        this.local(particle, forward, dy, right);
    }

    // Routed through ParticleFx so many calls per tick become one packet per player.
    void at(ParticleOptions particle, double dx, double dy, double dz) {
        ParticleFx.sendNear(this.level, particle, this.x + dx, this.y + dy, this.z + dz, 1, 0.0, 0.0, 0.0, 0.0);
    }

    void fly(ParticleOptions particle, double dx, double dy, double dz,
                     double vx, double vy, double vz, double speed) {
        ParticleFx.sendNear(this.level, particle, this.x + dx, this.y + dy, this.z + dz, 0, vx, vy, vz, speed);
    }

    void cloud(ParticleOptions particle, double dx, double dy, double dz, int count,
                       double spreadX, double spreadY, double spreadZ, double speed) {
        ParticleFx.sendNear(this.level, particle, this.x + dx, this.y + dy, this.z + dz, count,
                spreadX, spreadY, spreadZ, speed);
    }

    void flash(double dy) {
        this.at(ParticleTypes.FLASH, 0, dy, 0);
    }

    void line(ParticleOptions particle, double x1, double y1, double z1,
                      double x2, double y2, double z2, double spacing, double keep) {
        double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        int count = Math.max(1, (int) Math.ceil(length / spacing));
        for (int i = 0; i <= count; i++) {
            if (keep < 1 && !this.chance(keep)) {
                continue;
            }
            double f = (double) i / count;
            this.at(particle, Mth.lerp(f, x1, x2), Mth.lerp(f, y1, y2), Mth.lerp(f, z1, z2));
        }
    }

    void ring(ParticleOptions particle, double radius, double dy, double spacing, double keep) {
        this.ringAt(particle, 0, dy, 0, radius, spacing, keep);
    }

    void ringAt(ParticleOptions particle, double cx, double cy, double cz, double radius, double spacing,
                        double keep) {
        this.arcAt(particle, cx, cy, cz, radius, 0, 2.0 * Math.PI, spacing, keep);
    }

    void arcAt(ParticleOptions particle, double cx, double cy, double cz, double radius, double from,
                       double to, double spacing, double keep) {
        int count = Math.max(4, (int) Math.ceil(Math.abs(to - from) * radius / spacing));
        for (int i = 0; i < count; i++) {
            if (keep < 1 && !this.chance(keep)) {
                continue;
            }
            double angle = from + (to - from) * i / count;
            this.at(particle, cx + Math.sin(angle) * radius, cy, cz + Math.cos(angle) * radius);
        }
    }

    private static double[] basis(double nx, double ny, double nz) {
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= len;
        ny /= len;
        nz /= len;
        double ux = -nz;
        double uz = nx;
        double ul = Math.sqrt(ux * ux + uz * uz);
        if (ul < 1e-4) {
            ux = 1;
            uz = 0;
            ul = 1;
        }
        ux /= ul;
        uz /= ul;
        double vx = ny * uz;
        double vy = nz * ux - nx * uz;
        double vz = -ny * ux;
        return new double[] {ux, 0, uz, vx, vy, vz};
    }

    void ring3(ParticleOptions particle, double cx, double cy, double cz, double radius,
                       double nx, double ny, double nz, double a1, double a2, double spacing, double keep) {
        double[] b = basis(nx, ny, nz);
        int count = Math.max(4, (int) Math.ceil(Math.abs(a2 - a1) * radius / spacing));
        for (int i = 0; i < count; i++) {
            if (keep < 1 && !this.chance(keep)) {
                continue;
            }
            double a = a1 + (a2 - a1) * i / count;
            double c = Math.cos(a) * radius;
            double s = Math.sin(a) * radius;
            double py = cy + c * b[1] + s * b[4];
            if (py < 0.02) {
                continue;
            }
            this.at(particle, cx + c * b[0] + s * b[3], py, cz + c * b[2] + s * b[5]);
        }
    }

    static double[] ring3Point(double cx, double cy, double cz, double radius, double nx, double ny,
                                       double nz, double a) {
        double[] b = basis(nx, ny, nz);
        double c = Math.cos(a) * radius;
        double s = Math.sin(a) * radius;
        return new double[] {cx + c * b[0] + s * b[3], cy + c * b[1] + s * b[4], cz + c * b[2] + s * b[5]};
    }

    void panelLine(ParticleOptions particle, double cx, double cz, double tx, double tz,
                           double u1, double v1, double u2, double v2, double spacing, double keep) {
        double length = Math.hypot(u2 - u1, v2 - v1);
        int count = Math.max(1, (int) Math.ceil(length / spacing));
        for (int i = 0; i <= count; i++) {
            double f = (double) i / count;
            double u = Mth.lerp(f, u1, u2);
            double v = Mth.lerp(f, v1, v2);
            if (v < 0.02 || keep < 1 && !this.chance(keep)) {
                continue;
            }
            this.at(particle, cx + tx * u, v, cz + tz * u);
        }
    }

    void panelEllipse(ParticleOptions particle, double cx, double cz, double tx, double tz,
                              double cu, double cv, double w, double h, double from, double to,
                              double spacing, double keep) {
        int count = Math.max(6, (int) Math.ceil(Math.abs(to - from) * Math.max(w, h) / spacing));
        for (int i = 0; i <= count; i++) {
            double a = from + (to - from) * i / count;
            double v = cv + Math.sin(a) * h;
            if (v < 0.02 || keep < 1 && !this.chance(keep)) {
                continue;
            }
            double u = cu + Math.cos(a) * w;
            this.at(particle, cx + tx * u, v, cz + tz * u);
        }
    }

    void shield(ParticleOptions edge, ParticleOptions cross, double angle, double dist, double base,
                        double scale, double keep) {
        double cx = Math.sin(angle) * dist;
        double cz = Math.cos(angle) * dist;
        double tx = Math.cos(angle);
        double tz = -Math.sin(angle);
        double[][] outline = {{-0.35, 1.0}, {0.35, 1.0}, {0.35, 0.45}, {0, 0}, {-0.35, 0.45}, {-0.35, 1.0}};
        for (int i = 0; i < outline.length - 1; i++) {
            this.panelLine(edge, cx, cz, tx, tz, outline[i][0] * scale, base + outline[i][1] * scale,
                    outline[i + 1][0] * scale, base + outline[i + 1][1] * scale, 0.08, keep);
        }
        this.panelLine(cross, cx, cz, tx, tz, 0, base + 0.15 * scale, 0, base + 0.9 * scale, 0.08, keep);
        this.panelLine(cross, cx, cz, tx, tz, -0.22 * scale, base + 0.7 * scale, 0.22 * scale,
                base + 0.7 * scale, 0.08, keep);
    }

    void figure(ParticleOptions particle, double cx, double cz, double keep) {
        double a = Math.atan2(cx, cz);
        double tx = Math.cos(a);
        double tz = -Math.sin(a);
        this.panelLine(particle, cx, cz, tx, tz, -0.1, 0.02, -0.07, 0.75, 0.09, keep);
        this.panelLine(particle, cx, cz, tx, tz, 0.1, 0.02, 0.07, 0.75, 0.09, keep);
        this.panelLine(particle, cx, cz, tx, tz, 0, 0.75, 0, 1.35, 0.09, keep);
        this.panelLine(particle, cx, cz, tx, tz, -0.25, 1.3, 0.25, 1.3, 0.09, keep);
        this.panelLine(particle, cx, cz, tx, tz, -0.25, 1.3, -0.3, 0.8, 0.09, keep);
        this.panelLine(particle, cx, cz, tx, tz, 0.25, 1.3, 0.3, 0.8, 0.09, keep);
        this.panelEllipse(particle, cx, cz, tx, tz, 0, 1.55, 0.17, 0.19, 0, 2.0 * Math.PI, 0.08, keep);
    }

    void eye(ParticleOptions lid, ParticleOptions pupil, double up, double width, double open,
                     double angle) {
        double tx = Math.cos(angle);
        double tz = -Math.sin(angle);
        this.panelEllipse(lid, 0, 0, tx, tz, 0, up, width, width * 0.45 * open, 0, 2.0 * Math.PI, 0.07, 1);
        if (open > 0.3) {
            this.at(pupil, 0, up, 0);
            this.at(pupil, 0, up + 0.06, 0);
            this.at(pupil, 0, up - 0.06, 0);
        }
    }

    void sword(ParticleOptions blade, ParticleOptions hilt, double guardY, int dir, double length,
                       double guardHalf, double width, double keep) {
        double tip = guardY + dir * length;
        this.localLine(blade, 0, guardY, -width, 0, tip, 0, 0.08, keep);
        this.localLine(blade, 0, guardY, width, 0, tip, 0, 0.08, keep);
        this.localLine(hilt, 0, guardY, -guardHalf, 0, guardY, guardHalf, 0.08, keep);
        this.localLine(hilt, 0, guardY, 0, 0, guardY - dir * 0.35, 0, 0.08, keep);
        this.localLine(hilt, 0, guardY - 0.06, -guardHalf, 0, guardY + 0.06, -guardHalf, 0.06, keep);
        this.localLine(hilt, 0, guardY - 0.06, guardHalf, 0, guardY + 0.06, guardHalf, 0.06, keep);
        this.at(hilt, 0, guardY - dir * 0.42, 0);
    }

    void sphere(ParticleOptions particle, double dx, double dy, double dz, double radius, int count,
                        double keep) {
        for (int i = 0; i < count; i++) {
            if (keep < 1 && !this.chance(keep)) {
                continue;
            }
            double[] d = sphereDirection(i, count);
            this.at(particle, dx + d[0] * radius, dy + d[1] * radius, dz + d[2] * radius);
        }
    }

    void sphereOut(ParticleOptions particle, double dx, double dy, double dz, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double[] d = sphereDirection(i, count);
            this.fly(particle, dx, dy, dz, d[0], d[1], d[2], speed);
        }
    }

    static double[] sphereDirection(int i, int count) {
        double yy = 1.0 - 2.0 * (i + 0.5) / count;
        double r = Math.sqrt(1.0 - yy * yy);
        double angle = i * 2.399963;
        return new double[] {Math.cos(angle) * r, yy, Math.sin(angle) * r};
    }

    void radial(ParticleOptions particle, double dy, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = 2.0 * Math.PI * i / count;
            this.fly(particle, 0, dy, 0, Math.sin(angle), 0, Math.cos(angle), speed);
        }
    }

    void radialIn(ParticleOptions particle, double radius, double dy, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = 2.0 * Math.PI * i / count + this.spread(0.3);
            this.fly(particle, Math.sin(angle) * radius, dy, Math.cos(angle) * radius,
                    -Math.sin(angle), 0, -Math.cos(angle), speed);
        }
    }

    void zigzag(ParticleOptions particle, double x1, double y1, double z1,
                        double x2, double y2, double z2, int segments, double jitter, double spacing) {
        double px = x1;
        double py = y1;
        double pz = z1;
        for (int i = 1; i <= segments; i++) {
            double f = (double) i / segments;
            double j = i == segments ? 0.0 : jitter;
            double nx = Mth.lerp(f, x1, x2) + this.spread(j);
            double ny = Mth.lerp(f, y1, y2) + (y1 == y2 ? 0 : this.spread(j * 0.5));
            double nz = Mth.lerp(f, z1, z2) + this.spread(j);
            this.line(particle, px, py, pz, nx, ny, nz, spacing, 1);
            px = nx;
            py = ny;
            pz = nz;
        }
    }

    void bolt(int glowRgb, double x1, double y1, double z1, double x2, double y2, double z2) {
        int segments = Math.max(3, (int) Math.ceil(Math.abs(y1 - y2) / 0.6));
        this.zigzag(this.dust(0xFFFFFF, 1.0F), x1, y1, z1, x2, y2, z2, segments, 0.3, 0.07);
        this.zigzag(this.dust(glowRgb, 1.6F), x1, y1, z1, x2, y2, z2, segments, 0.3, 0.12);
    }

    void burstBlock(Block block, double dx, double dy, double dz, int count, double speed) {
        this.cloud(new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState()),
                dx, dy, dz, count, 0.2, 0.2, 0.2, speed);
    }

    void burstItem(Item item, double dx, double dy, double dz, int count, double speed) {
        this.cloud(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(item)),
                dx, dy, dz, count, 0.15, 0.15, 0.15, speed);
    }

    void debrisAt(double dx, double dz, int count) {
        this.cloud(new BlockParticleOption(ParticleTypes.BLOCK, this.ground()), dx, 0.1, dz, count,
                0.08, 0.03, 0.08, 0.12);
    }

    void groundDebris(int count, double radius) {
        for (int i = 0; i < count; i++) {
            double angle = this.rand() * 2.0 * Math.PI;
            double d = Math.sqrt(this.rand()) * radius;
            this.debrisAt(Math.sin(angle) * d, Math.cos(angle) * d, 2);
        }
    }

    void wings(ParticleOptions feather, ParticleOptions edge, double spread, double scale, double keep) {
        double span = 1.4 * spread * scale;
        int feathers = 16;
        for (int i = 0; i <= feathers; i++) {
            double f = (double) i / feathers;
            double up = 1.3 + Math.sin(f * Math.PI * 0.9) * 0.7 * scale;
            double back = -0.3 - 0.12 * Math.sin(f * Math.PI);
            double right = 0.15 + span * f;
            for (int side = -1; side <= 1; side += 2) {
                if (!this.chance(keep)) {
                    continue;
                }
                this.local(edge, back, up, right * side);
                this.local(feather, back - 0.04, up - 0.25 - 0.3 * f, right * side * 0.95);
                if (f > 0.4) {
                    this.local(feather, back - 0.06, up - 0.55 - 0.5 * f, right * side * 0.9);
                }
            }
        }
    }

    void sound(SoundEvent sound, float volume, float pitch) {
        this.level.playSound(null, this.x, this.y, this.z, sound, SoundSource.PLAYERS,
                volume * this.mode.volume(), pitch);
    }

    void sound(Holder<SoundEvent> sound, float volume, float pitch) {
        this.sound(sound.value(), volume, pitch);
    }

    static final double FLOOR = 0.08;

    static final boolean MAIN = false;

    static final boolean ACC = true;

    static final int[] FORSAKEN_SHARDS = {0xD8D8E0, 0x7FD46B, 0xE0B45A, 0xA88BE8, 0xF2E6A0, 0x5FD0C8};

    static double fadeOut(int t, int from, int to) {
        return t < from ? 1 : Mth.clamp(1 - (t - from) / (double) (to - from), 0, 1);
    }

    static double hash(int i, int salt) {
        double v = Math.sin(i * 12.9898 + salt * 78.233) * 43758.5453;
        return v - Math.floor(v);
    }

    static int lerpColor(double t, int from, int to) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return (r << 16) | (g << 8) | b;
    }
}
