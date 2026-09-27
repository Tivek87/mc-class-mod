package nl.tivek.multiversepowers.engine.client.ragdoll;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.slf4j.Logger;

// How limp each kind of creature goes, from resource packs: assets/<namespace>/ragdolls/<name>.json tunes the entity
// type <namespace>:<name>. Loaded again with the other resources (F3+T); a broken file is logged and left out.
public final class RagdollProfiles extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    // One part, by the name its model gives it: how heavy (times the usual), and how far it swings and twists on its
    // joint (degrees).
    record Tuning(Optional<Double> mass, Optional<Double> swing, Optional<Double> twist) {
        static final Codec<Tuning> CODEC = RecordCodecBuilder.create(tuning -> tuning.group(
                Codec.doubleRange(0.01, 100.0).optionalFieldOf("mass").forGetter(Tuning::mass),
                Codec.doubleRange(0.0, 180.0).optionalFieldOf("swing").forGetter(Tuning::swing),
                Codec.doubleRange(0.0, 180.0).optionalFieldOf("twist").forGetter(Tuning::twist))
                .apply(tuning, Tuning::new));
        static final Tuning NONE = new Tuning(Optional.empty(), Optional.empty(), Optional.empty());
    }

    // A whole kind of creature: never limp, or falling stiff as one piece, and its parts tuned one by one.
    record Profile(boolean never, boolean stiff, Map<String, Tuning> parts) {
        static final Codec<Profile> CODEC = RecordCodecBuilder.create(profile -> profile.group(
                Codec.BOOL.optionalFieldOf("never", false).forGetter(Profile::never),
                Codec.BOOL.optionalFieldOf("stiff", false).forGetter(Profile::stiff),
                Codec.unboundedMap(Codec.STRING, Tuning.CODEC).optionalFieldOf("parts", Map.of())
                        .forGetter(Profile::parts))
                .apply(profile, Profile::new));
        static final Profile NONE = new Profile(false, false, Map.of());

        Tuning part(String name) {
            return this.parts.getOrDefault(name, Tuning.NONE);
        }
    }

    private static volatile Map<ResourceLocation, Profile> profiles = Map.of();

    private RagdollProfiles() {
        super(new Gson(), "ragdolls");
    }

    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new RagdollProfiles());
    }

    static Profile of(EntityType<?> type) {
        return profiles.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(type), Profile.NONE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Profile> read = new HashMap<>();
        files.forEach((id, json) -> Profile.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LOGGER.warn("Ragdoll profile {} left out: {}", id, error))
                .ifPresent(profile -> read.put(id, profile)));
        profiles = Map.copyOf(read);
    }
}
