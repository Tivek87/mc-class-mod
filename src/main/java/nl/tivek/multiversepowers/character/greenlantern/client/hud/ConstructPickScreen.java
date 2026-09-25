package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPickPayload;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public class ConstructPickScreen extends DirtBackgroundScreen {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".construct_pick.";
    static final String[] NAMES = { "fist", "hands", "fists", "hammer", "emblem", "anvil", "cymbals", "uppercut",
            "spikes", "boot", "weight", "sword", "rockets", "swatter", "lantern", "safe", "anchor", "mace", "barbell",
            "bell", "meteor", "palm", "gavel", "pickaxe", "trap", "book", "drum", "pillar", "tnt", "piano", "brick",
            "stamp" };
    private static final String[] GROUP_NAMES = { "drop", "clap", "ground", "swing", "other" };
    private static final int[][] GROUPS = {
            { ConstructPayload.SLAM_FIST, ConstructPayload.SLAM_HAMMER, ConstructPayload.SLAM_ANVIL,
                    ConstructPayload.SLAM_BOOT, ConstructPayload.SLAM_WEIGHT, ConstructPayload.SLAM_SWORD,
                    ConstructPayload.SLAM_LANTERN, ConstructPayload.SLAM_SAFE, ConstructPayload.SLAM_ANCHOR,
                    ConstructPayload.SLAM_MACE, ConstructPayload.SLAM_BARBELL, ConstructPayload.SLAM_BELL,
                    ConstructPayload.SLAM_METEOR, ConstructPayload.SLAM_PALM, ConstructPayload.SLAM_TNT,
                    ConstructPayload.SLAM_PIANO, ConstructPayload.SLAM_BRICK, ConstructPayload.SLAM_STAMP },
            { ConstructPayload.SLAM_HANDS, ConstructPayload.SLAM_FISTS, ConstructPayload.SLAM_CYMBALS,
                    ConstructPayload.SLAM_TRAP, ConstructPayload.SLAM_BOOK },
            { ConstructPayload.SLAM_UPPERCUT, ConstructPayload.SLAM_SPIKES, ConstructPayload.SLAM_PILLAR },
            { ConstructPayload.SLAM_SWATTER, ConstructPayload.SLAM_PICKAXE, ConstructPayload.SLAM_GAVEL,
                    ConstructPayload.SLAM_DRUM },
            { ConstructPayload.SLAM_EMBLEM, ConstructPayload.SLAM_ROCKETS } };
    private static final int MAX_WIDTH = 540;
    private static final int TOP = 44;
    private static final int FOOTER = 34;
    private static final int ROW = 22;
    private static final int HEADER = 14;
    private static final int GAP = 4;
    private static final int GREEN = 0xFF6CFF8E;

    private record Entry(Button button, int y) {
    }

    private record Header(Component title, int y) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private final List<Header> headers = new ArrayList<>();
    private int panelLeft;
    private int panelWidth;
    private int contentHeight;
    private int scroll;

    public ConstructPickScreen() {
        super(Component.translatable(PREFIX + "title"));
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("constructshockwave").executes(context -> {
            Minecraft minecraft = Minecraft.getInstance();
            // The chat screen is still open when the command runs; defer to swap it
            minecraft.tell(() -> minecraft.setScreen(new ConstructPickScreen()));
            return 1;
        }));
    }

    static Component name(int variant) {
        return Component.translatable("construct." + MultiversePowers.MODID + ".slam." + NAMES[variant]);
    }

    @Override
    protected void init() {
        this.entries.clear();
        this.headers.clear();
        this.panelWidth = Math.min(this.width - 16, MAX_WIDTH);
        this.panelLeft = (this.width - this.panelWidth) / 2;
        int inner = this.panelWidth - 16;
        int columns = Mth.clamp((inner + GAP) / (100 + GAP), 2, 6);
        int buttonWidth = (inner - (columns - 1) * GAP) / columns;
        int y = 0;
        for (int g = 0; g < GROUPS.length; g++) {
            this.headers.add(new Header(Component.translatable(PREFIX + "group." + GROUP_NAMES[g]), y));
            y += HEADER;
            int[] group = GROUPS[g];
            for (int i = 0; i < group.length; i++) {
                int variant = group[i];
                int x = this.panelLeft + 8 + (i % columns) * (buttonWidth + GAP);
                Button button = Button.builder(name(variant), pressed -> this.pick(variant))
                        .bounds(x, 0, buttonWidth, 20)
                        .tooltip(Tooltip.create(Component.translatable("construct." + MultiversePowers.MODID
                                + ".slam." + NAMES[variant] + ".desc")))
                        .build();
                this.entries.add(new Entry(this.addRenderableWidget(button), y + (i / columns) * ROW));
            }
            y += ((group.length + columns - 1) / columns) * ROW + 6;
        }
        this.contentHeight = y;
        int center = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "random"),
                pressed -> this.pick(this.minecraft.level == null ? 0
                        : this.minecraft.level.random.nextInt(ConstructPayload.SLAM_KINDS)))
                .bounds(center - 104, this.height - 26, 100, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, pressed -> this.onClose())
                .bounds(center + 4, this.height - 26, 100, 20).build());
        this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
        this.layout();
    }

    private int viewHeight() {
        return this.height - FOOTER - TOP;
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - this.viewHeight());
    }

    private void layout() {
        int bottom = this.height - FOOTER;
        for (Entry entry : this.entries) {
            int y = TOP + entry.y() - this.scroll;
            entry.button().setY(y);
            entry.button().visible = y >= TOP && y + 20 <= bottom;
        }
    }

    private void pick(int variant) {
        PacketDistributor.sendToServer(new ConstructPickPayload(variant));
        if (this.minecraft != null) {
            this.minecraft.gui.setOverlayMessage(Component.translatable(PREFIX + "coming", name(variant)), false);
        }
        this.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scroll = Mth.clamp(this.scroll - (int) Math.round(scrollY * ROW), 0, this.maxScroll());
        this.layout();
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.renderTransparentBackground(graphics);
        } else {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
        drawPanel(graphics, this.panelLeft, 4, this.panelWidth, this.height - 8, PANEL_BORDER);
        int center = this.width / 2;
        this.drawBigCenteredString(graphics, this.title, center, 10, 1.2F, GREEN);
        graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "help"), center, 24, MUTED_COLOR);
        int bottom = this.height - FOOTER;
        for (Header header : this.headers) {
            int y = TOP + header.y() - this.scroll;
            if (y >= TOP && y + HEADER <= bottom) {
                graphics.drawString(this.font, header.title(), this.panelLeft + 8, y + 3, GREEN);
                int end = this.panelLeft + 12 + this.font.width(header.title());
                graphics.fill(end, y + 7, this.panelLeft + this.panelWidth - 8, y + 8, DIVIDER_COLOR);
            }
        }
        if (this.maxScroll() > 0) {
            int track = this.viewHeight();
            int bar = Math.max(12, track * track / this.contentHeight);
            int barY = TOP + (track - bar) * this.scroll / this.maxScroll();
            int x = this.panelLeft + this.panelWidth - 5;
            graphics.fill(x, TOP, x + 2, TOP + track, 0x40FFFFFF);
            graphics.fill(x, barY, x + 2, barY + bar, 0xC0FFFFFF);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
