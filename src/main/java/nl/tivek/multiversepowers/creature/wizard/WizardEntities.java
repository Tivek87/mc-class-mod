package nl.tivek.multiversepowers.creature.wizard;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.wizard.abyss.SoulDrain;
import nl.tivek.multiversepowers.creature.wizard.abyss.Supernova;
import nl.tivek.multiversepowers.creature.wizard.abyss.VoidOrb;
import nl.tivek.multiversepowers.creature.wizard.abyss.VoidTendrils;
import nl.tivek.multiversepowers.creature.wizard.field.ArcaneChains;
import nl.tivek.multiversepowers.creature.wizard.field.ArcaneTornado;
import nl.tivek.multiversepowers.creature.wizard.field.Blizzard;
import nl.tivek.multiversepowers.creature.wizard.field.FrostNova;
import nl.tivek.multiversepowers.creature.wizard.field.GlacialSpikes;
import nl.tivek.multiversepowers.creature.wizard.field.GravityWell;
import nl.tivek.multiversepowers.creature.wizard.field.InfernoRing;
import nl.tivek.multiversepowers.creature.wizard.field.MarkedStrike;
import nl.tivek.multiversepowers.creature.wizard.field.RuneMine;
import nl.tivek.multiversepowers.creature.wizard.field.StarRift;
import nl.tivek.multiversepowers.creature.wizard.field.TimeWarp;
import nl.tivek.multiversepowers.creature.wizard.special.GrandSigil;
import nl.tivek.multiversepowers.creature.wizard.special.Omen;
import nl.tivek.multiversepowers.creature.wizard.special.SolarWaves;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcLightning;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBeam;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBolt;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneFamiliar;
import nl.tivek.multiversepowers.creature.wizard.spell.FallingStar;
import nl.tivek.multiversepowers.creature.wizard.spell.GreatMeteor;
import nl.tivek.multiversepowers.creature.wizard.spell.PrismLance;
import nl.tivek.multiversepowers.creature.wizard.spell.RuneSentry;
import nl.tivek.multiversepowers.creature.wizard.spell.WardCrystal;
import nl.tivek.multiversepowers.creature.wizard.spell.WizardFireball;

// Every kind of thing the Wizard brings into the world: himself, his decoys and familiars, and his spells, with the
// harm his magic does.
public final class WizardEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    // Moved every tick while his clones race round (WizardMirrors): told every tick, so none of them jerks along.
    public static final DeferredHolder<EntityType<?>, EntityType<Wizard>> WIZARD = TYPES.register("wizard",
            () -> EntityType.Builder.<Wizard>of(Wizard::new, MobCategory.MONSTER).sized(0.6F, 1.95F)
                    .eyeHeight(1.62F).fireImmune().clientTrackingRange(10).updateInterval(1).build("wizard"));
    public static final DeferredHolder<EntityType<?>, EntityType<WizardDecoy>> DECOY = TYPES.register(
            "wizard_decoy", () -> EntityType.Builder.<WizardDecoy>of(WizardDecoy::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).fireImmune().clientTrackingRange(10).updateInterval(1)
                    .build("wizard_decoy"));
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneFamiliar>> FAMILIAR = TYPES.register(
            "arcane_familiar", () -> EntityType.Builder.<ArcaneFamiliar>of(ArcaneFamiliar::new, MobCategory.MONSTER)
                    .sized(0.5F, 0.5F).fireImmune().clientTrackingRange(8).build("arcane_familiar"));
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBolt>> BOLT = projectile("arcane_bolt",
            ArcaneBolt::new, 0.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBolt>> SPARK = projectile("arcane_spark",
            ArcaneBolt::new, 0.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<WizardFireball>> FIREBALL = projectile(
            "wizard_fireball", WizardFireball::new, 0.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<FallingStar>> STAR = projectile("falling_star",
            FallingStar::new, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<GreatMeteor>> METEOR = projectile("great_meteor",
            GreatMeteor::new, 1.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<StarRift>> RIFT = field("star_rift", StarRift::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Blizzard>> BLIZZARD = field("blizzard",
            Blizzard::new);
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneChains>> CHAINS = field("arcane_chains",
            ArcaneChains::new);
    public static final DeferredHolder<EntityType<?>, EntityType<RuneMine>> RUNE = field("arcane_rune",
            RuneMine::new);
    public static final DeferredHolder<EntityType<?>, EntityType<GravityWell>> WELL = field("gravity_well",
            GravityWell::new);
    public static final DeferredHolder<EntityType<?>, EntityType<TimeWarp>> WARP = field("time_warp", TimeWarp::new);
    public static final DeferredHolder<EntityType<?>, EntityType<ArcLightning>> ARC = field("chain_lightning",
            ArcLightning::new);
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBeam>> BEAM = field("arcane_beam",
            ArcaneBeam::new);
    // His ward's crystals circle him: told every tick, struck by anything.
    public static final DeferredHolder<EntityType<?>, EntityType<WardCrystal>> CRYSTAL = TYPES.register(
            "ward_crystal", () -> EntityType.Builder.<WardCrystal>of(WardCrystal::new, MobCategory.MISC)
                    .sized(0.6F, 0.9F).fireImmune().clientTrackingRange(10).updateInterval(1).build("ward_crystal"));
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBolt>> ORB = projectile("arcane_orb",
            ArcaneBolt::new, 0.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBolt>> SHARD = projectile("star_shard",
            ArcaneBolt::new, 0.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrismLance>> LANCE = projectile("prism_lance",
            PrismLance::new, 0.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<FrostNova>> NOVA = field("frost_nova",
            FrostNova::new);
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneTornado>> TORNADO = field("arcane_tornado",
            ArcaneTornado::new);
    public static final DeferredHolder<EntityType<?>, EntityType<InfernoRing>> RING_OF_FIRE = field("inferno_ring",
            InfernoRing::new);
    // A tendril holds its foe's legs, and its foe may strike it to break free.
    public static final DeferredHolder<EntityType<?>, EntityType<VoidTendrils>> TENDRILS = TYPES.register(
            "void_tendrils", () -> EntityType.Builder.<VoidTendrils>of(VoidTendrils::new, MobCategory.MISC)
                    .sized(0.9F, 1.2F).fireImmune().clientTrackingRange(10).updateInterval(1).build("void_tendrils"));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidOrb>> VOID_ORB = TYPES.register("void_orb",
            () -> EntityType.Builder.<VoidOrb>of(VoidOrb::new, MobCategory.MISC).sized(1.0F, 1.0F).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).build("void_orb"));
    public static final DeferredHolder<EntityType<?>, EntityType<SoulDrain>> DRAIN = field("soul_drain",
            SoulDrain::new);
    public static final DeferredHolder<EntityType<?>, EntityType<Supernova>> SUPERNOVA = TYPES.register("supernova",
            () -> EntityType.Builder.<Supernova>of(Supernova::new, MobCategory.MISC).sized(1.0F, 1.0F).fireImmune()
                    .clientTrackingRange(16).updateInterval(1).build("supernova"));
    public static final DeferredHolder<EntityType<?>, EntityType<GlacialSpikes>> GLACIAL = field("glacial_spikes",
            GlacialSpikes::new);
    public static final DeferredHolder<EntityType<?>, EntityType<MarkedStrike>> STRIKE = field("marked_strike",
            MarkedStrike::new);
    public static final DeferredHolder<EntityType<?>, EntityType<GrandSigil>> SIGIL = field("grand_sigil",
            GrandSigil::new);
    public static final DeferredHolder<EntityType<?>, EntityType<SolarWaves>> WAVES = field("solar_waves",
            SolarWaves::new);
    // A sentry turns to its foe and the omens keep over him: told every tick.
    public static final DeferredHolder<EntityType<?>, EntityType<RuneSentry>> SENTRY = TYPES.register("rune_sentry",
            () -> EntityType.Builder.<RuneSentry>of(RuneSentry::new, MobCategory.MISC).sized(0.8F, 0.8F)
                    .fireImmune().clientTrackingRange(10).updateInterval(1).build("rune_sentry"));
    public static final DeferredHolder<EntityType<?>, EntityType<Omen>> OMEN = TYPES.register("wizard_omen",
            () -> EntityType.Builder.<Omen>of(Omen::new, MobCategory.MISC).sized(1.0F, 1.0F).fireImmune()
                    .clientTrackingRange(16).updateInterval(1).build("wizard_omen"));
    public static final ResourceKey<DamageType> ARCANE = key("arcane");
    public static final ResourceKey<DamageType> FROST = key("frost");
    public static final ResourceKey<DamageType> STARFALL = key("starfall");
    public static final ResourceKey<DamageType> LIGHTNING = key("arcane_lightning");
    public static final ResourceKey<DamageType> VOID = key("void_magic");
    public static final ResourceKey<DamageType> INFERNO = key("inferno");
    public static final ResourceKey<DamageType> NOVA_BURST = key("supernova");

    private WizardEntities() {
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(WizardEntities::onAttributes);
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> projectile(String name,
            EntityType.EntityFactory<T> factory, float size) {
        return TYPES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC).sized(size, size)
                .clientTrackingRange(8).updateInterval(1).build(name));
    }

    // A spell laid on a place, told every tick (a beam's end, who the chains hold).
    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> field(String name,
            EntityType.EntityFactory<T> factory) {
        return TYPES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC).sized(1.0F, 0.2F)
                .clientTrackingRange(10).updateInterval(2).build(name));
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
                name));
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WIZARD.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 700.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.ATTACK_DAMAGE, 10.8).add(Attributes.ARMOR, 3.5)
                .add(Attributes.ARMOR_TOUGHNESS, 2.1).add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.FOLLOW_RANGE, 48.0).build());
        event.put(DECOY.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0).build());
        event.put(FAMILIAR.get(), Vex.createAttributes().add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.ATTACK_DAMAGE, 4.8).build());
    }
}
