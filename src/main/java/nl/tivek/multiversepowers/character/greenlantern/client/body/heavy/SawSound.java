package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The Heavy Chainsaw's engine as every player near hears it, from what the server told of the saw (ClientHeavy): the
// cord and the catch as it forms, then three loops laid over each other, idling, screaming at full throttle and
// grinding through something, each turned up and down as the saw's move asks, and a blip of the throttle as a slash or
// a thrust begins.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class SawSound extends AbstractTickableSoundInstance {
    private static final SoundEvent START = Sounds.of("saw.start");
    private static final SoundEvent IDLE_LOOP = Sounds.of("saw.idle");
    private static final SoundEvent RUN_LOOP = Sounds.of("saw.run");
    private static final SoundEvent CUT_LOOP = Sounds.of("saw.cut");
    private static final SoundEvent REV_BLIP = Sounds.of("saw.rev");
    // The start's engine settles to its idle this long after the saw begins to form.
    private static final double SETTLED = 30.0;
    private static final float IDLE = 0.55F;
    private static final float RUN = 0.8F;
    private static final float CUT = 0.95F;

    private record Engine(SawSound idle, SawSound run, SawSound cut) {
    }

    private static final Map<Integer, Engine> ENGINES = new HashMap<>();
    private static final Map<Integer, Double> TOLD = new HashMap<>();

    private final Entity owner;
    private float target;

    private SawSound(SoundEvent sound, Entity owner) {
        super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.owner = owner;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001F;
        this.x = owner.getX();
        this.y = owner.getY() + 1.0;
        this.z = owner.getZ();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            clear();
            return;
        }
        for (Map.Entry<Integer, ClientHeavy.Held> entry : ClientHeavy.all().entrySet()) {
            ClientHeavy.Held held = entry.getValue();
            Entity owner = level.getEntity(entry.getKey());
            if (held.weapon != SAW || owner == null) {
                continue;
            }
            double now = ClientHeavy.now(0.0F);
            Double was = TOLD.put(owner.getId(), held.start);
            boolean fresh = was == null || Math.abs(was - held.start) > 0.5;
            if (fresh && held.brokeAt < 0.0) {
                if (held.move == FORM) {
                    once(owner, START, 1.0F);
                } else if (held.move == REV || held.move == REV_BACK || held.move == IMPALE) {
                    once(owner, REV_BLIP, held.move == IMPALE ? 0.95F : 1.0F + 0.08F * (held.move - REV));
                }
            }
            Engine engine = ENGINES.get(owner.getId());
            if (engine == null || engine.idle.isStopped()) {
                engine = new Engine(new SawSound(IDLE_LOOP, owner), new SawSound(RUN_LOOP, owner),
                        new SawSound(CUT_LOOP, owner));
                ENGINES.put(owner.getId(), engine);
                Minecraft.getInstance().getSoundManager().play(engine.idle);
                Minecraft.getInstance().getSoundManager().play(engine.run);
                Minecraft.getInstance().getSoundManager().play(engine.cut);
            }
            set(engine, held, now - held.formedAt);
        }
        ENGINES.entrySet().removeIf(entry -> {
            ClientHeavy.Held held = ClientHeavy.all().get(entry.getKey());
            if (held != null && held.weapon == SAW) {
                return false;
            }
            entry.getValue().idle.target = 0.0F;
            entry.getValue().run.target = 0.0F;
            entry.getValue().cut.target = 0.0F;
            TOLD.remove(entry.getKey());
            return true;
        });
    }

    // How loud each loop should be for what the saw does now.
    private static void set(Engine engine, ClientHeavy.Held held, double formed) {
        double age = held.age(0.0F);
        float idle = formed < SETTLED ? 0.0F : IDLE;
        float run = 0.0F;
        float cut = 0.0F;
        if (held.brokeAt >= 0.0) {
            idle = 0.0F;
        } else {
            switch (held.move) {
                case REV, REV_BACK -> idle *= 0.6F;
                case REND -> {
                    idle = 0.0F;
                    run = age < LOOP_FROM ? RUN : RUN * 0.35F;
                    cut = age < LOOP_FROM ? 0.0F : CUT;
                }
                case GUARD -> {
                    idle = 0.0F;
                    run = RUN;
                }
                case IMPALE -> {
                    idle *= 0.3F;
                    cut = age >= hit(SAW, IMPALE) && age < EJECT ? CUT : 0.0F;
                }
                default -> {
                }
            }
        }
        engine.idle.target = idle;
        engine.run.target = run;
        engine.cut.target = cut;
    }

    private static void once(Entity owner, SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(new EntityBoundSoundInstance(sound, SoundSource.PLAYERS, 1.0F,
                pitch, owner, owner.getRandom().nextLong()));
    }

    public static void clear() {
        for (Engine engine : ENGINES.values()) {
            engine.idle.stop();
            engine.run.stop();
            engine.cut.stop();
        }
        ENGINES.clear();
        TOLD.clear();
    }

    @Override
    public void tick() {
        if (this.owner.isRemoved()) {
            this.stop();
            return;
        }
        this.x = this.owner.getX();
        this.y = this.owner.getY() + 1.0;
        this.z = this.owner.getZ();
        // Up quickly as the throttle opens, down a little slower as it falls back.
        this.volume = Mth.lerp(this.target > this.volume ? 0.45F : 0.25F, this.volume, this.target);
        if (this.target <= 0.0F && this.volume < 0.01F && !this.current()) {
            this.stop();
        }
    }

    // Still one of its owner's engine loops: a saw formed again soon after has new loops, and these may stop.
    private boolean current() {
        Engine engine = ENGINES.get(this.owner.getId());
        return engine != null && (engine.idle == this || engine.run == this || engine.cut == this);
    }
}
