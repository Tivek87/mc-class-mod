package nl.tivek.welcomescreen.init;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.welcomescreen.WelcomeScreenMod;

public final class ModItems {
        public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WelcomeScreenMod.MODID);

        private ModItems() {
        }

        public static void register(IEventBus modEventBus) {
                ITEMS.register(modEventBus);
        }
}
