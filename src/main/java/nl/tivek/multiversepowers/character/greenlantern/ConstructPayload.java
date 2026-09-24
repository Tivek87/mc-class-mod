package nl.tivek.multiversepowers.character.greenlantern;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Server tells nearby clients where one of Green Lantern's hard-light constructs is this tick. Clients
 * draw it as glowing green light, with a beam from the ring of the one who made it.
 *
 * @param owner  entity id of the Green Lantern it belongs to, or -1 when unknown
 * @param center where it is. A fist that is still held beside its owner gives where it hangs around his eyes
 *               instead (x to his right, y up, z ahead), so every client can hang it on him without lag
 * @param facing the way it points (a fist: the way it punches; the beam: the way it shines)
 * @param size   how wide it is, in blocks; a little wider while it falls apart. The beam: how long it is
 * @param solid  0 = not there, 1 = fully solid. Below 0 = the construct is gone. A slam: how big its construct is,
 *               next to the size it was made at
 * @param charge how far it has been charged: 0 = not at all, 1 = as far as it goes. The shields: 1 right after
 *               a hit landed on them. A bolt: how far it flies each tick, so clients can keep it moving. A slam:
 *               how slowly it plays (1 = the pace its constructs were made for, 1.5 = half again as slow)
 * @param held    true while the ring still holds it beside you (it charges); false once it flies
 * @param shape   which construct it is, see {@link #FIST}
 * @param variant which one of its kind: for {@link #SLAM} which construct makes the shockwave (see
 *                {@link #SLAM_FIST})
 * @param age     ticks since it set off: for a fist since it was let go, for a bolt since it was shot, for a slam
 *                since the landing. Clients run their own clock from it, so it moves smoothly however unevenly
 *                updates arrive
 * @param path    the way a fist or bolt on its way flies, or null (see {@link ConstructPath})
 */
public record ConstructPayload(int id, int owner, Vec3 center, Vec3 facing, float size, float solid,
        float charge, boolean held, int shape, int variant, int age, @Nullable ConstructPath path)
        implements CustomPacketPayload {
    public static final int FIST = 0;
    // How far round where a construct was its removal is told, in blocks: past the 128 its updates reach.
    private static final double REMOVE_RANGE = 176.0;
    /** A small bullet of hard light, shot from the ring. */
    public static final int BOLT = 1;
    /** A round shield of hard light, held up in front of him. */
    public static final int SHIELD = 2;
    /** The beam that pours out of the ring while the attack button is held. */
    public static final int BEAM = 3;
    /** A dome of hard light all around him, while the defend button is held. */
    public static final int DOME = 4;
    /** The shield while he flies: a pointed cone out in front of him that rams whatever he flies into. */
    public static final int RAM = 5;
    /**
     * A landing at full speed: a big construct that strikes where he landed and sends a shockwave over the ground.
     * {@code center} is where it strikes the ground, {@code facing} the way he faced, {@code size} how far the
     * shockwave reaches, {@code age} how long ago he landed, and {@code variant} which construct it is.
     */
    public static final int SLAM = 6;
    /**
     * The ring's scan: a wave of light rolling out through everything. {@code center} is where it set out from,
     * {@code size} how far it reaches, {@code charge} how many seconds what it passes stays marked for its maker,
     * {@code variant} {@link #SCAN_HOSTILE} when it marks only what is out to hurt him, and {@code age} how long ago it
     * set out. Not a construct, only light.
     */
    public static final int SCAN = 7;
    /** A {@link #SCAN} of the air strike's plane: it marks only the creatures out to hurt its maker. */
    public static final int SCAN_HOSTILE = 1;
    /**
     * The air strike, Green Lantern's ultimate: a big hard-light gunship with four propellers high over the
     * battlefield, that drones on in a straight line and plunges down. It flies a path both sides work out alike from
     * how long ago it was called (see {@link PlanePath}): {@code center} is where it takes shape, {@code facing} the
     * way it flies, {@code size} how far below where it takes shape the ground of its dive is, {@code charge} how many
     * ticks it attacks, {@code variant} how far through its dive the first of its parts strikes the ground in
     * hundredths, and {@code age} how long ago he called it.
     */
    public static final int PLANE = 9;
    /**
     * A homing missile of hard light of the air strike, every tick it flies: a big one out of the plane's hatch or a
     * small one off a jet's wing. {@code center} is where its middle is (on the tick it strikes: where it was as its
     * nose struck), {@code facing} the way its nose points, {@code size} the tick of the plane's clock it was let go
     * on, {@code charge} how many ticks after that its motor fires, {@code variant}
     * {@link nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike#BIG_MISSILE} or
     * {@link nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike#JET_MISSILE} plus 2 for each jet
     * before its own plus its pylon (0 the left one, 1 the right one), and {@code age} how long ago it was let go.
     */
    public static final int MISSILE = 10;
    /**
     * A round of hard light from one of the plane's miniguns, sent once as it is fired: clients fly it on by
     * themselves, out of the muzzle of its gun as the gun pointed then (see {@link PlanePath.Turret}). {@code center}
     * is where it strikes, {@code facing} the way its gun is to point next (one long; it swings there as it does on
     * the server), {@code size} how many ticks it flies, {@code charge} the tick of the plane's clock it was fired on,
     * {@code held} true when it strikes nothing (it flies off into the air), {@code variant} the gun (0 the left one,
     * 1 the right one) and {@code age} how long ago it was fired.
     */
    public static final int BULLET = 11;
    /**
     * The Light Bubble: a cage of hard light round a creature the ring holds up. {@code center} is its middle,
     * {@code facing} the way its maker looked as he caught it, {@code size} its radius, {@code solid} how far it has
     * grown round its creature, {@code held} true while the ring still holds it, {@code variant} what it does (see
     * {@link nl.tivek.multiversepowers.character.greenlantern.ability.LightBubble#HOLDING}; a pound is timed from the
     * first update that tells of it), {@code charge} the entity id of its creature until it breaks up and from then on
     * how many ticks ago it began to, and {@code age} how long ago it caught its creature.
     */
    public static final int BUBBLE = 12;
    /**
     * The sword and shield of the construct wheel, in the hands of the one holding them. {@code center} is where he is,
     * {@code facing} the way he looks (or charges), {@code size} how many ticks ago they began to break up (-1 while
     * whole), {@code variant} the move they do (see
     * {@link nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove}), {@code charge} the tick of their
     * clock that move began on, and {@code age} how long ago they took shape.
     */
    public static final int SWORD = 13;
    /**
     * The small blast of one of the air strike's missiles, sent once as it bursts: clients play it out by themselves,
     * from the moment its missile gets there on their screen. {@code center} is its heart, {@code facing} the way the
     * missile came in, {@code size} how far it reaches, {@code variant}
     * {@link nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike#BIG_BLAST} or
     * {@link nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike#SMALL_BLAST}, and {@code age} how long
     * ago it burst. Light, not a construct: the missile itself breaks into solid pieces on its own.
     */
    public static final int BLAST = 14;
    /**
     * One slam of a Light Bubble pounded into the ground, sent once as it strikes: clients play out its shockwave by
     * themselves. {@code center} is where it struck the ground, {@code facing} the way its maker looked as he caught the
     * creature, {@code size} how far the shockwave reaches, {@code variant} which slam of the pound it is (see
     * {@link nl.tivek.multiversepowers.character.greenlantern.ability.LightBubble#POUND}; the last is the hardest) and
     * {@code age} how long ago it struck. Light, not a construct.
     */
    public static final int POUND = 15;
    /**
     * One of the Giant Hands: a hand of hard light that bursts up out of the ground and strikes a creature (see
     * {@link nl.tivek.multiversepowers.character.greenlantern.HandPose}), or a pair of hands out of portals of the
     * ring's light that chop a giant axe down on it (see
     * {@link nl.tivek.multiversepowers.character.greenlantern.HandDuo}). {@code center} is where it comes up out of
     * the ground (for a pair: the ground where it was called), {@code facing} the flat way from there to the spot it
     * reaches for (as long as it is far; for a pair: where its axe strikes), {@code size} how big it is,
     * {@code variant} its move (for a pair also the way it is laid out, see
     * {@link nl.tivek.multiversepowers.character.greenlantern.HandPose#axeVariant}), {@code charge} the entity id of
     * the creature it holds in its fist (-1 for none, and always -1 for a pair) and {@code age} how long ago it was
     * called.
     */
    public static final int HAND = 16;
    /** A giant fist that smashes down out of the sky, knuckles first. */
    public static final int SLAM_FIST = 0;
    /** Two giant open hands that clap together. */
    public static final int SLAM_HANDS = 1;
    /** Two giant fists that bump together. */
    public static final int SLAM_FISTS = 2;
    /** A giant war hammer that drops head first. */
    public static final int SLAM_HAMMER = 3;
    /** The lantern emblem, standing up and falling flat on its face. */
    public static final int SLAM_EMBLEM = 4;
    /** A giant anvil that drops. */
    public static final int SLAM_ANVIL = 5;
    /** Two giant cymbals that crash together. */
    public static final int SLAM_CYMBALS = 6;
    /** A giant fist and arm that burst up out of the ground, an uppercut. */
    public static final int SLAM_UPPERCUT = 7;
    /** Two rings of spikes that shoot up out of the ground around where it strikes. */
    public static final int SLAM_SPIKES = 8;
    /** A giant boot that stomps down out of the sky. */
    public static final int SLAM_BOOT = 9;
    /** A cartoon weight of one ton that drops. */
    public static final int SLAM_WEIGHT = 10;
    /** A giant sword that drops point first out of the sky and stays standing in the ground. */
    public static final int SLAM_SWORD = 11;
    /** A volley of rockets from behind him that come down one after another. */
    public static final int SLAM_ROCKETS = 12;
    /** A giant fly swatter that he swings down flat onto the ground ahead of him. */
    public static final int SLAM_SWATTER = 13;
    /** His own lantern, the power battery, dropping out of the sky. */
    public static final int SLAM_LANTERN = 14;
    /** A giant safe that drops. */
    public static final int SLAM_SAFE = 15;
    /** A ship's anchor on its chain that drops. */
    public static final int SLAM_ANCHOR = 16;
    /** A spiked ball, tumbling as it drops. */
    public static final int SLAM_MACE = 17;
    /** A giant barbell that drops. */
    public static final int SLAM_BARBELL = 18;
    /** A giant bell that drops on its rim and rings. */
    public static final int SLAM_BELL = 19;
    /** A meteor that streaks in out of the sky ahead of him. */
    public static final int SLAM_METEOR = 20;
    /** A giant open hand that slaps down flat on the ground. */
    public static final int SLAM_PALM = 21;
    /** A judge's gavel that strikes its block. */
    public static final int SLAM_GAVEL = 22;
    /** A giant pickaxe he swings over his head into the ground. */
    public static final int SLAM_PICKAXE = 23;
    /** A bear trap whose jaws snap shut. */
    public static final int SLAM_TRAP = 24;
    /** A giant book that slams shut. */
    public static final int SLAM_BOOK = 25;
    /** A giant drum struck by two drumsticks. */
    public static final int SLAM_DRUM = 26;
    /** A pillar that bursts up out of the ground and topples over away from him. */
    public static final int SLAM_PILLAR = 27;
    /** A block of TNT that drops and blows up. */
    public static final int SLAM_TNT = 28;
    /** An upright piano that drops. */
    public static final int SLAM_PIANO = 29;
    /** A giant toy brick that drops. */
    public static final int SLAM_BRICK = 30;
    /** A giant rubber stamp that stamps the lantern emblem into the ground. */
    public static final int SLAM_STAMP = 31;
    /** How many different slam constructs there are. */
    public static final int SLAM_KINDS = 32;

    public static final CustomPacketPayload.Type<ConstructPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "construct"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConstructPayload> STREAM_CODEC = CustomPacketPayload
            .codec(ConstructPayload::write, ConstructPayload::read);

    /** A construct that stands still or hangs on its owner: nothing of its own kind, no clock, no path. */
    public ConstructPayload(int id, int owner, Vec3 center, Vec3 facing, float size, float solid, float charge,
            boolean held, int shape) {
        this(id, owner, center, facing, size, solid, charge, held, shape, 0, 0, null);
    }

    public static ConstructPayload remove(int id) {
        return new ConstructPayload(id, -1, Vec3.ZERO, new Vec3(0, 0, 1), 0.0F, -1.0F, 0.0F, false, FIST);
    }

    /**
     * Tells everyone who could see this construct that it is gone at once: everyone round where it was, as far as its
     * updates reach and a little more. Anyone further off lost it already, as a client does with a construct it
     * hears nothing of for a while.
     */
    public static void sendRemove(ServerLevel level, int id, Vec3 near) {
        PacketDistributor.sendToPlayersNear(level, null, near.x, near.y, near.z, REMOVE_RANGE, remove(id));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeVarInt(this.owner + 1);
        buf.writeDouble(this.center.x);
        buf.writeDouble(this.center.y);
        buf.writeDouble(this.center.z);
        buf.writeFloat((float) this.facing.x);
        buf.writeFloat((float) this.facing.y);
        buf.writeFloat((float) this.facing.z);
        buf.writeFloat(this.size);
        buf.writeFloat(this.solid);
        buf.writeFloat(this.charge);
        buf.writeBoolean(this.held);
        buf.writeByte(this.shape);
        buf.writeByte(this.variant);
        buf.writeVarInt(this.age);
        buf.writeBoolean(this.path != null);
        if (this.path != null) {
            this.path.write(buf);
        }
    }

    private static ConstructPayload read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int owner = buf.readVarInt() - 1;
        Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 facing = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        float size = buf.readFloat();
        float solid = buf.readFloat();
        float charge = buf.readFloat();
        boolean held = buf.readBoolean();
        int shape = buf.readByte();
        int variant = buf.readByte();
        int age = buf.readVarInt();
        ConstructPath path = buf.readBoolean() ? ConstructPath.read(buf) : null;
        return new ConstructPayload(id, owner, center, facing, size, solid, charge, held, shape, variant, age, path);
    }

    @Override
    public CustomPacketPayload.Type<ConstructPayload> type() {
        return TYPE;
    }
}
