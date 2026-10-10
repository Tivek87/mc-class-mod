package nl.tivek.multiversepowers.character.client;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.client.ClientStandings;
import nl.tivek.multiversepowers.killconfirm.client.KillMarker;

// A character's own crosshair in place of the game's: each draws its own round the middle of the screen, in its own
// colours, moving with everything that happens (Feel): it blooms out as you strike, run or leave the ground and springs
// back, closes in on a creature under it in that creature's colour (red, yellow, green) with a thin arc of its health,
// flashes as a blow of yours lands, jolts red when you are hurt, squashes as you land and stretches as you fall, beats
// with your heart when your health runs low, pops as you switch what you hold, draws in while you crouch or hold a
// button, shakes "no" when a move is refused, wobbles in water and flickers on fire, sways a little behind your look,
// and kicks in a way of its own with each ability (Kind). The kill's red cross flicks out round its own edge. The
// game's comes back with the debug screen, outside first person and without a character.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Crosshairs {
    // Draws a crosshair round (cx, cy) and says how far out it reaches (gui pixels), for the kill's cross to sit round.
    @FunctionalInterface
    public interface Drawer {
        float draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, Feel feel);
    }

    // How an ability kicks the crosshair as it is used: a burst outward, a slam shut, a dash's sideways smear, a
    // lift's upward stretch, a grab closing in, a guard thickening, a spin.
    public enum Kind {
        BURST, SLAM, DASH, LIFT, GRAB, GUARD, SPIN
    }

    // `spread` 0 at rest to about 1 blooming out, `aim` 0 to 1 on a creature of colour `target` with `health` of its
    // health left, `hit` and `kill` 0 to 1 as a blow landed or a kill was made just now; `hurt`, `refuse`, `land` and
    // `swap` 0 to 1 just after you were hurt, a move was refused, you landed or switched what you hold; `fall` how
    // fast you fall, `beat` your heart at low health, `focus` crouched or holding a button, `wet` and `burn` in water
    // or on fire; the last ability used (`used`, its `seed`) kicking `pulse` (1 as it starts, to 0); how far the outer
    // parts sway behind your look; `time` in ticks.
    public record Feel(float spread, float aim, int target, float hit, float kill, float partialTick, float hurt,
            float refuse, float land, float fall, float beat, float swap, float focus, float wet, float burn,
            Kind used, float pulse, int seed, float swayX, float swayY, float health, float time) {
        // How much bigger it is drawn now.
        public float grow() {
            float kick = switch (this.used) {
                case BURST -> 0.2F;
                case GRAB -> -0.35F;
                case GUARD -> 0.08F;
                default -> 0.0F;
            };
            return (1.0F + 0.14F * this.beat + 0.35F * this.swap - 0.15F * this.focus) * (1.0F + kick * this.kick());
        }

        // How much wider and taller.
        public float wide() {
            float kick = this.used == Kind.DASH ? 0.6F : this.used == Kind.SLAM ? 0.25F : 0.0F;
            return (1.0F + kick * this.kick()) * (1.0F + 0.15F * this.land);
        }

        public float tall() {
            float kick = this.used == Kind.LIFT ? 0.55F : this.used == Kind.SLAM ? -0.4F : 0.0F;
            return (1.0F + kick * this.kick()) * (1.0F - 0.3F * this.land) * (1.0F + 0.35F * this.fall);
        }

        // How far it is turned (radians): a spin goes half round back to where it was, water rocks it.
        public float turn() {
            float spin = this.used == Kind.SPIN ? Mth.PI * this.kick() * (this.seed % 2 == 0 ? 1.0F : -1.0F) : 0.0F;
            return spin + 0.1F * this.wet * Mth.sin(this.time * 0.21F);
        }

        // How much thicker its lines are.
        public float thick() {
            return (this.used == Kind.GUARD ? 0.9F * this.kick() : 0.0F) + 0.35F * this.hit;
        }

        // The ability's kick, eased.
        public float kick() {
            float p = Mth.clamp(this.pulse, 0.0F, 1.0F);
            return p * p * (3.0F - 2.0F * p);
        }

        // A point `dx`, `dy` off the middle as the crosshair is grown, squashed, turned and swayed now.
        public float x(float cx, float dx, float dy) {
            float turn = this.turn();
            return cx + this.swayX + this.grow() * this.wide() * (dx * Mth.cos(turn) - dy * Mth.sin(turn));
        }

        public float y(float cy, float dx, float dy) {
            float turn = this.turn();
            return cy + this.swayY + this.grow() * this.tall() * (dx * Mth.sin(turn) + dy * Mth.cos(turn));
        }

        // A radius as grown and squashed now (rings stay round).
        public float radius(float radius) {
            return radius * this.grow() * 0.5F * (this.wide() + this.tall());
        }
    }

    private static final Map<GameCharacter, Drawer> DRAWERS = new EnumMap<>(GameCharacter.class);
    // How far a click blooms it out, how fast that springs back (kept a tick), and how fast it follows.
    private static final float KICK = 0.55F;
    private static final float KICK_KEPT = 0.78F;
    private static final float FOLLOW = 0.45F;
    // Below this share of health the heart beats in it.
    private static final float LOW_HEALTH = 0.35F;
    // Overlay messages from the server that say a move was refused.
    private static final String[] REFUSALS = { "no_power", "busy", "not_ready", "cannot", "cant_", "refus", "blocked",
            "too_", "need" };
    private static float spread;
    private static float spreadBefore;
    private static float aim;
    private static float aimBefore;
    private static float kick;
    private static float hitSeen;
    private static int target = Standing.NEUTRAL.rgb();
    private static float health = 1.0F;
    private static boolean attackWas;
    private static boolean useWas;
    private static int buttonHeld;
    // Each signal now and a tick before, eased between them as it is drawn.
    private static final float[] NOW = new float[11];
    private static final float[] BEFORE = new float[11];
    private static final int HURT = 0;
    private static final int REFUSE = 1;
    private static final int LAND = 2;
    private static final int FALL = 3;
    private static final int SWAP = 4;
    private static final int FOCUS = 5;
    private static final int WET = 6;
    private static final int BURN = 7;
    private static final int PULSE = 8;
    private static final int SWAY_X = 9;
    private static final int SWAY_Y = 10;
    private static Kind used = Kind.BURST;
    private static int usedSeed;
    private static int hurtWas;
    private static boolean groundWas = true;
    private static float fallMost;
    private static int slotWas = -1;
    private static ItemStack heldWas = ItemStack.EMPTY;
    private static float yawWas;
    private static float pitchWas;
    private static float danger;

    private Crosshairs() {
    }

    // An ability of the own player's character was just used.
    public static void used(CharacterAbility ability) {
        used = kindOf(ability.id());
        usedSeed = ability.id().hashCode() & 0xFFFF;
        NOW[PULSE] = 1.0F;
    }

    // A move of the own player's was just refused.
    public static void refused() {
        NOW[REFUSE] = 1.0F;
    }

    private static Kind kindOf(String id) {
        return switch (id) {
            case "shockwave", "ground_slam", "ground_strike", "thunderclap", "air_shockwave", "giant_hands",
                    "air_strike", "rampage", "hammer_uppercut", "lightning_bomb" -> Kind.SLAM;
            case "dash", "air_blink", "grab_dash", "lightning_flight", "emerald_express" -> Kind.DASH;
            case "flight", "super_jump", "hammer_leap", "storm" -> Kind.LIFT;
            case "grab", "light_bubble", "multi_tentacle", "grab_dash_dive", "hammer_call", "hammer_follow",
                    "beam_lock" -> Kind.GRAB;
            case "block", "stance", "charged", "recharge", "mjolnir" -> Kind.GUARD;
            case "construct_wheel", "light_fists", "combo", "storm_throw", "hammer_throw", "portal", "mech",
                    "ring_scan" -> Kind.SPIN;
            default -> Kind.BURST;
        };
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            spread = spreadBefore = aim = aimBefore = kick = 0.0F;
            Arrays.fill(NOW, 0.0F);
            Arrays.fill(BEFORE, 0.0F);
            slotWas = -1;
            return;
        }
        spreadBefore = spread;
        aimBefore = aim;
        System.arraycopy(NOW, 0, BEFORE, 0, NOW.length);
        boolean attack = minecraft.options.keyAttack.isDown();
        boolean use = minecraft.options.keyUse.isDown();
        if (attack && !attackWas || use && !useWas) {
            kick = Math.min(1.0F, kick + KICK);
        }
        attackWas = attack;
        useWas = use;
        buttonHeld = attack || use ? buttonHeld + 1 : 0;
        float hit = KillMarker.hitFlash();
        if (hit > hitSeen + 0.5F) {
            kick = Math.min(1.0F, kick + 0.25F);
        }
        hitSeen = hit;
        kick *= KICK_KEPT;
        Vec3 motion = player.getDeltaMovement();
        float moving = Mth.clamp((float) Math.sqrt(motion.x * motion.x + motion.z * motion.z) / 0.28F, 0.0F, 1.0F);
        float goal = 0.3F * moving * (player.isSprinting() ? 1.5F : 1.0F) + (player.onGround() ? 0.0F : 0.3F) + kick;
        spread += (Math.min(1.4F, goal) - spread) * FOLLOW;
        Entity picked = minecraft.crosshairPickEntity;
        boolean on = picked instanceof LivingEntity living && living.isAlive() && !living.isInvisibleTo(player);
        if (on) {
            LivingEntity living = (LivingEntity) picked;
            target = ClientStandings.of(picked).rgb();
            health = Mth.clamp(living.getHealth() / Math.max(1.0F, living.getMaxHealth()), 0.0F, 1.0F);
        }
        aim += ((on ? 1.0F : 0.0F) - aim) * (on ? 0.5F : 0.25F);
        feelings(player);
    }

    // What the player's own body and hands are doing.
    private static void feelings(LocalPlayer player) {
        NOW[HURT] *= 0.8F;
        NOW[REFUSE] *= 0.8F;
        NOW[LAND] *= 0.72F;
        NOW[SWAP] *= 0.7F;
        NOW[PULSE] *= 0.8F;
        if (player.hurtTime > hurtWas) {
            NOW[HURT] = 1.0F;
        }
        hurtWas = player.hurtTime;
        Vec3 motion = player.getDeltaMovement();
        boolean ground = player.onGround();
        if (!ground) {
            fallMost = Math.max(fallMost, (float) -motion.y);
        } else if (!groundWas && fallMost > 0.3F) {
            NOW[LAND] = Mth.clamp(fallMost, 0.3F, 1.0F);
        }
        if (ground) {
            fallMost = 0.0F;
        }
        groundWas = ground;
        float falling = ground || player.getAbilities().flying ? 0.0F
                : Mth.clamp(((float) -motion.y - 0.5F) / 1.5F, 0.0F, 1.0F);
        NOW[FALL] += (falling - NOW[FALL]) * 0.4F;
        int slot = player.getInventory().selected;
        ItemStack held = player.getMainHandItem();
        if (slotWas >= 0 && (slot != slotWas || !ItemStack.isSameItem(held, heldWas))) {
            NOW[SWAP] = 1.0F;
        }
        slotWas = slot;
        heldWas = held.copy();
        float focus = player.isCrouching() || buttonHeld > 6 ? 1.0F : 0.0F;
        NOW[FOCUS] += (focus - NOW[FOCUS]) * 0.3F;
        NOW[WET] += ((player.isInWater() ? 1.0F : 0.0F) - NOW[WET]) * 0.2F;
        NOW[BURN] += ((player.isOnFire() ? 1.0F : 0.0F) - NOW[BURN]) * 0.3F;
        float yaw = Mth.wrapDegrees(player.getYRot() - yawWas);
        float pitch = player.getXRot() - pitchWas;
        yawWas = player.getYRot();
        pitchWas = player.getXRot();
        NOW[SWAY_X] += (Mth.clamp(-yaw * 0.08F, -2.5F, 2.5F) - NOW[SWAY_X]) * 0.5F;
        NOW[SWAY_Y] += (Mth.clamp(-pitch * 0.08F, -2.0F, 2.0F) - NOW[SWAY_Y]) * 0.5F;
        float share = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        danger = player.isAlive() && share < LOW_HEALTH ? 1.0F - share / LOW_HEALTH : 0.0F;
    }

    // An overlay line from the server that says no (the ring's "no power", "your hands are busy").
    @SubscribeEvent
    public static void onSystemMessage(ClientChatReceivedEvent.System event) {
        if (!event.isOverlay() || !(event.getMessage().getContents() instanceof TranslatableContents words)
                || !words.getKey().contains(MultiversePowers.MODID)) {
            return;
        }
        for (String refusal : REFUSALS) {
            if (words.getKey().contains(refusal)) {
                refused();
                return;
            }
        }
    }

    public static void add(GameCharacter character, Drawer drawer) {
        DRAWERS.put(character, drawer);
    }

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR) || event.isCanceled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        GameCharacter character = ClientCharacter.active();
        Drawer drawer = character == null ? null : DRAWERS.get(character);
        if (drawer == null || player == null || player.isSpectator() || minecraft.options.hideGui
                || !minecraft.options.getCameraType().isFirstPerson()
                || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics graphics = event.getGuiGraphics();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = player.tickCount + partialTick;
        float[] now = new float[NOW.length];
        for (int i = 0; i < now.length; i++) {
            now[i] = Mth.lerp(partialTick, BEFORE[i], NOW[i]);
        }
        // The heart: two quick beats, then a rest, faster and harder the lower the health.
        float rate = 0.35F + 0.25F * danger;
        float phase = (time * rate) % Mth.TWO_PI;
        float beat = danger * (float) (Math.pow(Math.max(0.0F, Mth.sin(phase * 2.0F)), 6.0)
                * (phase < Mth.PI ? 1.0F : 0.0F));
        Feel feel = new Feel(Mth.lerp(partialTick, spreadBefore, spread), Mth.lerp(partialTick, aimBefore, aim),
                target, KillMarker.hitFlash(), KillMarker.killed(), partialTick, now[HURT], now[REFUSE], now[LAND],
                now[FALL], beat, now[SWAP], now[FOCUS], now[WET], now[BURN], used, now[PULSE], usedSeed,
                now[SWAY_X], now[SWAY_Y], health, time);
        // Hurt, it jolts every way; refused, it shakes side to side.
        float cx = graphics.guiWidth() / 2.0F + 2.2F * now[HURT] * Mth.sin(time * 3.7F)
                + 2.4F * now[REFUSE] * Mth.sin(time * 2.9F);
        float cy = graphics.guiHeight() / 2.0F + 1.6F * now[HURT] * Mth.cos(time * 4.3F);
        float reach = drawer.draw(graphics, player, cx, cy, feel);
        kick(graphics, cx, cy, reach, feel);
        KillMarker.claim();
        KillMarker.cross(graphics, cx, cy, reach + 1.0F, reach + 4.0F);
        GuiShapes.flush(graphics);
    }

    // What the last ability used leaves round the crosshair as it kicks: a ring bursting out, lines slamming shut from
    // above and below, streaks left and right or up and down, corners closing in, a steady ring round it, arcs going
    // round. Each ability turns and spaces its own a little differently.
    private static void kick(GuiGraphics graphics, float cx, float cy, float reach, Feel feel) {
        float p = feel.pulse();
        if (p < 0.03F) {
            return;
        }
        float k = feel.kick();
        float out = 1.0F - p;
        int color = tint(0xFFFFFF, feel, 0.0F);
        float spin = (feel.seed() % 90) * Mth.DEG_TO_RAD * 0.5F;
        int count = 2 + feel.seed() % 3;
        switch (feel.used()) {
            case BURST -> ring(graphics, cx, cy, reach + 1.0F + 9.0F * out, 0.7F, color, 0.7F * k);
            case SLAM -> {
                float gap = reach + 7.0F * (1.0F - out);
                float half = 3.0F + 3.0F * k;
                stroke(graphics, cx - half, cy - gap, cx + half, cy - gap, 0.8F, color, 0.75F * k);
                stroke(graphics, cx - half, cy + gap, cx + half, cy + gap, 0.8F, color, 0.75F * k);
            }
            case DASH, LIFT -> {
                boolean side = feel.used() == Kind.DASH;
                for (int i = 0; i < count; i++) {
                    float lane = (i - (count - 1) * 0.5F) * 2.2F;
                    float from = reach + 1.0F + 6.0F * out;
                    float to = from + 4.0F * k;
                    for (int end = -1; end <= 1; end += 2) {
                        if (side) {
                            stroke(graphics, cx + end * from, cy + lane, cx + end * to, cy + lane, 0.6F, color,
                                    0.6F * k);
                        } else {
                            stroke(graphics, cx + lane, cy + end * from, cx + lane, cy + end * to, 0.6F, color,
                                    0.6F * k);
                        }
                    }
                }
            }
            case GRAB -> {
                float at = reach + 2.0F + 8.0F * p;
                for (int corner = 0; corner < 4; corner++) {
                    float a = (corner + 0.5F) * Mth.HALF_PI + spin;
                    float x = cx + Mth.sin(a) * at;
                    float y = cy - Mth.cos(a) * at;
                    float bx = Mth.sin(a + Mth.HALF_PI) * 2.0F;
                    float by = -Mth.cos(a + Mth.HALF_PI) * 2.0F;
                    stroke(graphics, x - bx, y - by, x + bx, y + by, 0.8F, color, 0.8F * k);
                }
            }
            case GUARD -> ring(graphics, cx, cy, reach + 2.0F, 0.6F + 1.2F * k, color, 0.55F * k);
            case SPIN -> {
                float r = reach + 2.5F;
                float turn = 360.0F * out * (feel.seed() % 2 == 0 ? 1.0F : -1.0F) + spin * Mth.RAD_TO_DEG;
                for (int i = 0; i < count; i++) {
                    float from = turn + i * 360.0F / count;
                    GuiShapes.arc(graphics, cx, cy, r - 0.45F, r + 0.45F, from, from + 40.0F,
                            GuiShapes.fade(color, 0.7F * k));
                }
            }
        }
    }

    // A thin arc round a creature under the crosshair, as long as the share of its health it has left.
    public static void health(GuiGraphics graphics, float cx, float cy, float radius, Feel feel) {
        if (feel.aim() < 0.05F || feel.health() >= 0.999F) {
            return;
        }
        float half = 180.0F * feel.health();
        GuiShapes.arc(graphics, cx, cy, radius - 0.9F, radius + 0.9F, 180.0F - half, 180.0F + half,
                GuiShapes.fade(0x000000, 0.35F * feel.aim()));
        GuiShapes.arc(graphics, cx, cy, radius - 0.4F, radius + 0.4F, 180.0F - half, 180.0F + half,
                GuiShapes.fade(feel.target(), 0.85F * feel.aim()));
    }

    // The colour a crosshair part takes: its own, toward the creature's under it as it closes in, white as a blow
    // lands, red as a kill is made or you are hurt, beating red at low health, grey as a move is refused, flickering
    // orange on fire.
    public static int tint(int own, Feel feel, float aimed) {
        int color = GuiShapes.mix(own, feel.target(), feel.aim() * aimed);
        color = GuiShapes.mix(color, 0xFFFFFF, feel.hit() * 0.8F);
        color = GuiShapes.mix(color, 0xFFA040, feel.burn() * (0.25F + 0.2F * Mth.sin(feel.time() * 1.7F)));
        color = GuiShapes.mix(color, 0x8A8A8A, feel.refuse() * 0.7F);
        color = GuiShapes.mix(color, KillMarker.RED, Math.max(feel.hurt() * 0.65F, feel.beat() * 0.45F));
        return GuiShapes.mix(color, KillMarker.RED, feel.kill() * 0.85F);
    }

    // A stroke with a dark edge under it, so it reads on snow and sky alike.
    public static void stroke(GuiGraphics graphics, float x0, float y0, float x1, float y1, float width, int rgb,
            float alpha) {
        GuiShapes.stroke(graphics, x0, y0, x1, y1, width + 1.0F, GuiShapes.fade(0x000000, 0.4F * alpha));
        GuiShapes.stroke(graphics, x0, y0, x1, y1, width, GuiShapes.fade(rgb, alpha));
    }

    public static void ring(GuiGraphics graphics, float cx, float cy, float radius, float width, int rgb,
            float alpha) {
        GuiShapes.ring(graphics, cx, cy, radius, width + 1.0F, GuiShapes.fade(0x000000, 0.4F * alpha));
        GuiShapes.ring(graphics, cx, cy, radius, width, GuiShapes.fade(rgb, alpha));
    }

    public static void dot(GuiGraphics graphics, float cx, float cy, float radius, int rgb, float alpha) {
        GuiShapes.disc(graphics, cx, cy, radius + 0.5F, GuiShapes.fade(0x000000, 0.4F * alpha));
        GuiShapes.disc(graphics, cx, cy, radius, GuiShapes.fade(rgb, alpha));
    }
}
