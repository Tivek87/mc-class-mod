package nl.tivek.multiversepowers.spell;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.faction.Factions;

final class SpellTargets {
    private SpellTargets() {
    }

    // Area spells only touch what is hostile to the caster, and players only where they may be hurt.
    static boolean hits(@Nullable ServerPlayer caster, Entity entity) {
        return caster != null && entity != caster && entity.isAlive() && !entity.isSpectator()
                && Factions.hostile(caster, entity)
                && !(entity instanceof Player player && !Targeting.isTargetable(caster, player));
    }

    static void push(LivingEntity target, Vec3 way, double strength, double lift) {
        double keep = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        target.setDeltaMovement(target.getDeltaMovement().add(way.x * strength * keep, lift * keep,
                way.z * strength * keep));
        target.hasImpulse = true;
        // Players move themselves client-side; this flag tells them about the push.
        target.hurtMarked = true;
    }
}
