package nl.tivek.multiversepowers.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;

public final class ModItems {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MultiversePowers.MODID);

        private ModItems() {
        }

        public static void register(IEventBus modEventBus) {
                ITEMS.register(modEventBus);
        }
}
