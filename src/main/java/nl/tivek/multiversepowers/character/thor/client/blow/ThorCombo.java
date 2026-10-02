package nl.tivek.multiversepowers.character.thor.client.blow;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorMotion;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.spell.client.ClientClaps;

// Thor's combo in his own game: every click throws the next blow, picked to flow out of the last as a fighter's
// combination does (the other hand next, the short blows up close and the long ones further off, low blows down low,
// now and then a kick, the big finishers once the combo has built up). With the hammer in hand it swings the hammer;
// in flight only his right hand strikes. A click that comes while a blow is still going waits for it. It also shows
// his own thunderclap wind-up while the button is held, and tells the server so the others see it.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThorCombo {
    // Within this many ticks after a blow ends the next still counts as the same combo.
    private static final int CHAIN = 12;
    // A waiting click is kept this long at most.
    private static final int WAITS = 10;
    private static final int REMEMBERED = 5;
    // Closer than this (blocks from his eyes to its box) a blow carries him no further forward.
    private static final double CLOSE = 1.2;
    // How far a push of one block a tick carries him before friction stops it: on the ground, and in the short hop of a
    // leap. With nothing aimed at he goes at most FREE_ROOM.
    private static final double CARRY_ON_GROUND = 2.2;
    private static final double CARRY_LEAPING = 6.0;
    private static final double FREE_ROOM = 2.0;
    private static final double HOP = 0.2;
    private static final double LOOK_REACH = 4.5;
    // The wind-up shows once the button has been held this share of its hold.
    private static final float CHARGE_SHOWN = 0.2F;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Deque<ThorBlow> RECENT = new ArrayDeque<>();

    @Nullable
    private static ThorBlow last;
    private static int startedAt = Integer.MIN_VALUE / 2;
    private static int count;
    private static int waitingSince = -1;
    private static int landsAt = -1;
    private static float landsHard;
    private static boolean sending;
    private static boolean charging;
    private static boolean wentOff;

    private ThorCombo() {
    }

    // The combo's click, from ThorMotion: a blow now (sent on with the one it is), or later once this one is done.
    public static int act(LocalPlayer player, CharacterAbility ability, int data) {
        if (sending) {
            return data;
        }
        if (!player.getMainHandItem().isEmpty()) {
            return -1;
        }
        if (!ready()) {
            waitingSince = ClientThor.ticks();
            return -1;
        }
        return data | begin(player).ordinal() << Characters.MOVE_SHIFT;
    }

    private static boolean ready() {
        return last == null || ClientThor.ticks() - startedAt >= last.ready();
    }

    // Which blows he may throw now: the hammer's with it in hand, his right hand's alone in flight (the left holds the
    // hammer), else his fists and feet.
    private static boolean fits(ThorBlow blow, LocalPlayer player) {
        if (ThorMotion.flying()) {
            return blow.oneHanded();
        }
        ClientThor.View view = ClientThor.view(player);
        boolean armed = view != null && view.has(ThorStatePayload.ARMED) && !view.has(ThorStatePayload.THROWN);
        return blow.kit() == (armed ? ThorBlow.Kit.HAMMER : ThorBlow.Kit.FISTS);
    }

    private static ThorBlow begin(LocalPlayer player) {
        int now = ClientThor.ticks();
        if (last == null || now - startedAt > last.ticks() + CHAIN || last.finisher() || !fits(last, player)) {
            count = 0;
            RECENT.clear();
        }
        LivingEntity target = target(player);
        ThorBlow blow = choose(player, target);
        last = blow;
        startedAt = now;
        count++;
        RECENT.addFirst(blow);
        int kept = 0;
        for (ThorBlow any : ThorBlow.values()) {
            kept += fits(any, player) ? 1 : 0;
        }
        while (RECENT.size() > Math.min(REMEMBERED, kept / 3)) {
            RECENT.removeLast();
        }
        ClientThor.predictBlow(player, blow.ordinal());
        lunge(player, blow, target);
        boolean heavy = blow.kick() || blow.finisher();
        player.level().playLocalSound(player.getX(), player.getEyeY(), player.getZ(),
                heavy ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS,
                heavy ? 0.45F : 0.55F, heavy ? 0.8F : 1.35F + 0.2F * RANDOM.nextFloat(), false);
        landsAt = target != null && target.distanceTo(player) <= blow.reach() * player.getScale() + 0.6
                ? now + blow.hit() : -1;
        landsHard = (float) blow.power();
        return blow;
    }

    // Some blows carry him forward into them; a leap (the superman punch) off the ground as well. Never further than
    // the room left before the target: up close he stays where he stands instead of running into it.
    private static void lunge(LocalPlayer player, ThorBlow blow, @Nullable LivingEntity target) {
        if (blow.lunge() <= 0.0 || !player.onGround()) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        boolean leap = blow == ThorBlow.SUPERMAN_PUNCH;
        double room = target == null ? FREE_ROOM
                : Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition())) - CLOSE * player.getScale();
        if (flat.lengthSqr() < 1.0E-6 || room <= 0.0) {
            return;
        }
        double speed = Math.min(blow.lunge(), room / (leap ? CARRY_LEAPING : CARRY_ON_GROUND));
        flat = flat.normalize().scale(speed);
        player.setDeltaMovement(player.getDeltaMovement().add(flat.x, leap ? HOP : 0.0, flat.z));
    }

    // The creature the look passes closest by within a blow's reach, if any.
    @Nullable
    private static LivingEntity target(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(LOOK_REACH * player.getScale()));
        AABB near = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, near,
                entity -> entity instanceof LivingEntity living && living.isAlive() && !entity.isSpectator())) {
            Vec3 on = entity.getBoundingBox().inflate(0.4).clip(eye, end).orElse(null);
            if (on != null && on.distanceToSqr(eye) < nearest) {
                nearest = on.distanceToSqr(eye);
                best = (LivingEntity) entity;
            }
        }
        return best;
    }

    // Picks the next blow by weight: none of the last few again, and each weighed by how well it follows the last
    // one and suits where the target stands.
    private static ThorBlow choose(LocalPlayer player, @Nullable LivingEntity target) {
        // How far off it stands in his own sizes, so a bigger Thor picks the same blows at the same reach.
        double size = player.getScale();
        double far = target == null ? 2.6
                : Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition())) / size;
        boolean low = player.getXRot() > 28.0F
                || target != null && target.getBoundingBox().maxY < player.getEyeY() - 1.1 * size;
        boolean high = player.getXRot() < -18.0F;
        boolean running = player.isSprinting();
        ThorBlow[] all = ThorBlow.values();
        double[] weights = new double[all.length];
        double total = 0.0;
        ThorBlow first = null;
        for (ThorBlow blow : all) {
            if (!fits(blow, player)) {
                continue;
            }
            first = first == null ? blow : first;
            double weight = weight(blow, far, low, high, running);
            weights[blow.ordinal()] = weight;
            total += weight;
        }
        if (total <= 0.0) {
            return first == null ? ThorBlow.JAB : first;
        }
        double pick = RANDOM.nextDouble() * total;
        for (ThorBlow blow : all) {
            pick -= weights[blow.ordinal()];
            if (pick <= 0.0) {
                return blow;
            }
        }
        return first == null ? ThorBlow.CROSS : first;
    }

    private static double weight(ThorBlow blow, double far, boolean low, boolean high, boolean running) {
        if (RECENT.contains(blow)) {
            return 0.0;
        }
        boolean leap = blow == ThorBlow.SUPERMAN_PUNCH && running && far > 2.4;
        if (blow.finisher() && count < 4 && !leap || blow.kick() && (count < 2 || last != null && last.kick())) {
            return 0.0;
        }
        double weight = blow.kick() ? 0.35 : blow.finisher() ? Math.min(1.4, 0.25 + 0.2 * (count - 4)) : 1.0;
        if (leap) {
            weight = Math.max(weight, 1.0) * 3.0;
        }
        if (last == null || count == 0) {
            boolean opener = blow == ThorBlow.JAB || blow == ThorBlow.CROSS || blow == ThorBlow.LEAD_STRAIGHT
                    || blow == ThorBlow.BACKFIST || blow == ThorBlow.PALM_STRIKE || blow == ThorBlow.HAMMER_THRUST
                    || blow == ThorBlow.HAMMER_SWING;
            weight *= opener ? 2.0 : 0.6;
        } else {
            // The hands take turns, as a boxer's combinations flow: the other side comes next.
            int before = last.limb().side();
            int now = blow.limb().side();
            if (before != 0 && now == -before) {
                weight *= 2.4;
            } else if (before != 0 && now == before) {
                weight *= 0.45;
            }
        }
        if (far < 1.7) {
            weight *= blow.reach() <= 2.6 ? 2.0 : blow.reach() >= 3.3 ? 0.5 : 1.0;
        } else if (far > 2.8) {
            weight *= blow.reach() >= 3.2 ? 2.2 : blow.reach() <= 2.3 ? 0.15 : 1.0;
        }
        if (low) {
            weight *= blow.height() == ThorBlow.Height.LOW ? 3.5 : blow.height() == ThorBlow.Height.HIGH ? 0.35 : 1.0;
        } else if (blow.height() == ThorBlow.Height.LOW) {
            weight *= 0.5;
        }
        if (high && blow.lift() >= 0.4) {
            weight *= 2.5;
        }
        if (running && (blow == ThorBlow.LEAD_STRAIGHT || blow == ThorBlow.FRONT_KICK)) {
            weight *= 2.0;
        }
        return weight;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused()) {
            return;
        }
        boolean thor = ClientCharacter.active() == GameCharacter.THOR;
        int now = ClientThor.ticks();
        if (thor && waitingSince >= 0 && ready()) {
            if (now - waitingSince <= WAITS && player.getMainHandItem().isEmpty()) {
                send(player, begin(player));
            }
            waitingSince = -1;
        }
        if (landsAt >= 0 && now >= landsAt) {
            landsAt = -1;
            CameraShake.add(0.35F + 0.35F * landsHard, 4);
        }
        windUp(player, thor);
    }

    private static void send(LocalPlayer player, ThorBlow blow) {
        CharacterAbility combo = GameCharacter.THOR.byName("combo");
        if (combo == null) {
            return;
        }
        int data = (player.isShiftKeyDown() ? Characters.SNEAKING : 0) | Characters.TAP
                | blow.ordinal() << Characters.MOVE_SHIFT;
        sending = true;
        try {
            ClientCharacter.sendAction(combo, true, data);
        } finally {
            sending = false;
        }
    }

    // The thunderclap's wind-up: shown from a fifth into the hold (a quick click is a blow, not a wind-up) while it is
    // off cooldown, kept as it goes off until the clap takes over, and let go of with the button.
    private static void windUp(LocalPlayer player, boolean thor) {
        CharacterAbility clap = GameCharacter.THOR.byName("thunderclap");
        float held = clap == null ? -1.0F : MouseHold.progress(clap, 0.0F);
        if (held < 0.0F) {
            wentOff = false;
            if (charging) {
                charge(player, clap, false);
            }
            return;
        }
        if (held >= 1.0F && charging) {
            wentOff = true;
        }
        if (wentOff) {
            return;
        }
        boolean want = thor && held >= CHARGE_SHOWN && ClientCharacter.inPlay(clap, player)
                && ClientCharacter.cooldownLeft(clap.slot()) == 0;
        if (want != charging) {
            charge(player, clap, want);
        }
    }

    private static void charge(LocalPlayer player, @Nullable CharacterAbility clap, boolean on) {
        charging = on;
        ClientClaps.charging(player.getId(), on);
        if (clap != null) {
            ClientCharacter.sendAction(clap, on, Characters.CHARGE);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        last = null;
        count = 0;
        RECENT.clear();
        waitingSince = -1;
        landsAt = -1;
        charging = false;
        wentOff = false;
    }
}
