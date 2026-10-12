package nl.tivek.multiversepowers.creature.wizard.special;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.field.MarkedStrike;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The great spell each of the Wizard's phases opens with (WizardMove.special), each its own: the Archmage, landed, cuts
// a great circle of runes round him that sends three rings of force along the ground (SIGIL: jump them); the Ascended
// calls a rain of light, shafts marked on the ground round his foes coming down one after another (RAIN); the Unbound
// becomes a sun and sends three waves of fire rolling out along the ground (FLARE: jump them); the Voidborn darkens the
// sky with an eclipse, the void erupting under each foe twice (ECLIPSE); the Eternal stops time, holding every foe
// nearly still under a golden clock while lances of light are marked round each, falling as time runs again
// (JUDGMENT).
public final class PhaseSpecials {
    private static final double REACH = 30.0;
    // The rain of light: a shaft every RAIN_EVERY ticks for RAIN_TICKS, each marked RAIN_MARK ticks before it falls.
    private static final int RAIN_EVERY = 3;
    private static final int RAIN_TICKS = 42;
    private static final int RAIN_MARK = 14;
    // The eclipse erupts under each foe at its release and again ECLIPSE_AGAIN ticks later, marked ECLIPSE_MARK before.
    private static final int ECLIPSE_AGAIN = 26;
    private static final int ECLIPSE_MARK = 20;
    private static final int DARK = 140;
    // The judgment: time stands still for STILL ticks; the lances fall LANCE_FALL ticks after its release.
    private static final int STILL = 34;
    private static final int LANCE_FALL = 46;

    private PhaseSpecials() {
    }

    public static void release(ServerLevel level, Wizard wizard, int kind) {
        Vec3 feet = Wizard.groundUnder(level, wizard.position());
        float harm = wizard.harm(kind);
        switch (kind) {
            case WizardMove.SIGIL -> GrandSigil.cut(level, wizard, feet, harm);
            case WizardMove.FLARE -> SolarWaves.roll(level, wizard, feet, harm);
            case WizardMove.ECLIPSE -> {
                Omen.raise(level, wizard, Omen.ECLIPSE, WizardMove.length(kind) - WizardMove.release(kind)
                        + ECLIPSE_AGAIN + 30);
                for (Player player : level.getEntitiesOfClass(Player.class, wizard.getBoundingBox().inflate(48.0),
                        player -> !player.isSpectator() && !player.isCreative())) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARK, 0), wizard);
                }
                rifts(level, wizard, harm);
            }
            case WizardMove.JUDGMENT -> judgment(level, wizard, harm);
            default -> {
            }
        }
        Vec3 at = wizard.position().add(0.0, 1.2 + wizard.hover(), 0.0);
        // Each special is told to those near on their action bar: what comes and how to live through it.
        String name = switch (kind) {
            case WizardMove.SIGIL -> "sigil";
            case WizardMove.RAIN -> "rain";
            case WizardMove.FLARE -> "flare";
            case WizardMove.ECLIPSE -> "eclipse";
            default -> "judgment";
        };
        Component warning = Component.translatable("boss." + MultiversePowers.MODID + ".wizard." + name);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, wizard.getBoundingBox()
                .inflate(REACH + 8.0))) {
            player.displayClientMessage(warning, true);
        }
        int light = Wizard.light(wizard.phase());
        ParticleFx.shockwave(level, Wizard.dust(light, 2.2F), at, 60, 0.8);
        ParticleFx.sphereOut(level, ParticleTypes.END_ROD, at, 30, 0.5);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 2.5F, 0.5F);
    }

    public static void during(ServerLevel level, Wizard wizard, int kind, int t) {
        int from = WizardMove.release(kind);
        Vec3 at = wizard.position().add(0.0, 1.2 + wizard.hover(), 0.0);
        if (t < from && t % 3 == 0) {
            ParticleFx.implosion(level, Wizard.dust(Wizard.light(wizard.phase()), 1.5F), at, 4.0 + t * 0.05, 10,
                    0.45);
        }
        switch (kind) {
            case WizardMove.RAIN -> {
                if (t >= from && t < from + RAIN_TICKS && (t - from) % RAIN_EVERY == 0) {
                    shaft(level, wizard, wizard.harm(kind));
                }
            }
            case WizardMove.ECLIPSE -> {
                if (t == from + ECLIPSE_AGAIN) {
                    rifts(level, wizard, wizard.harm(kind));
                }
            }
            default -> {
            }
        }
    }

    // One shaft of light, marked near a foe: right on them now and then, else a step or two off where they might go.
    private static void shaft(ServerLevel level, Wizard wizard, float harm) {
        List<LivingEntity> foes = foes(level, wizard);
        if (foes.isEmpty()) {
            return;
        }
        LivingEntity foe = foes.get(wizard.getRandom().nextInt(foes.size()));
        Vec3 at = foe.position();
        if (wizard.getRandom().nextInt(3) != 0) {
            Vec3 ahead = foe.getDeltaMovement().multiply(1.0, 0.0, 1.0).scale(RAIN_MARK * 0.6);
            double angle = wizard.getRandom().nextDouble() * Mth.TWO_PI;
            double far = 1.0 + wizard.getRandom().nextDouble() * 3.0;
            at = at.add(ahead).add(Math.cos(angle) * far, 0.0, Math.sin(angle) * far);
        }
        MarkedStrike.mark(level, wizard, Wizard.groundUnder(level, at.add(0.0, 2.0, 0.0)), MarkedStrike.SHAFT,
                RAIN_MARK, harm);
    }

    // The void cracks open under every foe.
    private static void rifts(ServerLevel level, Wizard wizard, float harm) {
        for (LivingEntity foe : foes(level, wizard)) {
            MarkedStrike.mark(level, wizard, Wizard.groundUnder(level, foe.position().add(0.0, 1.0, 0.0)),
                    MarkedStrike.RIFT, ECLIPSE_MARK, harm);
        }
    }

    // Time stands still for every foe under his golden clock, while five lances are marked on and round each.
    private static void judgment(ServerLevel level, Wizard wizard, float harm) {
        Omen.raise(level, wizard, Omen.CLOCK, LANCE_FALL + 16);
        for (LivingEntity foe : foes(level, wizard)) {
            foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STILL, 7), wizard);
            foe.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, STILL, 3), wizard);
            foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STILL, 1), wizard);
            Vec3 at = foe.position();
            MarkedStrike.mark(level, wizard, Wizard.groundUnder(level, at.add(0.0, 1.0, 0.0)), MarkedStrike.LANCE,
                    LANCE_FALL, harm);
            double turn = wizard.getRandom().nextDouble() * Mth.TWO_PI;
            for (int k = 0; k < 4; k++) {
                double angle = turn + k * Mth.HALF_PI;
                Vec3 spot = at.add(Math.cos(angle) * 2.4, 1.0, Math.sin(angle) * 2.4);
                MarkedStrike.mark(level, wizard, Wizard.groundUnder(level, spot), MarkedStrike.LANCE,
                        LANCE_FALL + 2 + k * 2, harm * 0.8F);
            }
            Vec3 hit = foe.getBoundingBox().getCenter();
            ParticleFx.ring(level, Wizard.dust(Wizard.TIME, 1.4F), hit, 1.0, 18, 0.0);
        }
        Vec3 at = wizard.position();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    // His foes within reach that he sees, the nearest twelve.
    private static List<LivingEntity> foes(ServerLevel level, Wizard wizard) {
        List<LivingEntity> foes = MonsterBlows.foes(wizard, wizard.getBoundingBox().inflate(REACH));
        foes.removeIf(living -> !Wizard.prey(living) || living.distanceTo(wizard) > REACH);
        foes.sort((a, b) -> Double.compare(a.distanceToSqr(wizard), b.distanceToSqr(wizard)));
        return foes.subList(0, Math.min(12, foes.size()));
    }
}
