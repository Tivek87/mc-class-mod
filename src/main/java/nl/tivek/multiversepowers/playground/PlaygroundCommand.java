package nl.tivek.multiversepowers.playground;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /playground build  ->  ragdoll stress-test arena.
 * Arena radius: 120 blocks. Large open areas between all feature zones.
 */
@EventBusSubscriber(modid = nl.tivek.multiversepowers.MultiversePowers.MODID)
public final class PlaygroundCommand {

    private PlaygroundCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playground")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("build")
                        .executes(PlaygroundCommand::build)));
    }

    private static int build(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        Vec3 pos = src.getPosition();
        int ox = (int) Math.floor(pos.x);
        int oy = 64;
        int oz = (int) Math.floor(pos.z);
        src.sendSuccess(() -> Component.literal("[Playground] Building arena..."), false);
        buildArena(level, ox, oy, oz);
        if (src.getEntity() instanceof ServerPlayer p)
            p.teleportTo(level, ox + 0.5, oy + 1, oz + 0.5, p.getYRot(), p.getXRot());
        src.sendSuccess(() -> Component.literal("[Playground] Done at " + ox + "," + oy + "," + oz), false);
        return 1;
    }

    private static void buildArena(ServerLevel level, int ox, int oy, int oz) {
        clearVolume(level, ox-122, oy-25, oz-122, ox+122, oy+75, oz+122);
        fillBox(level, ox-121, oy-1, oz-121, ox+121, oy-1, oz+121, Blocks.BEDROCK.defaultBlockState());
        fillBox(level, ox-120, oy,   oz-120, ox+120, oy,   oz+120, Blocks.SMOOTH_STONE.defaultBlockState());
        buildPerimeterWall(level, ox, oy, oz, 120, 14);

        // ---- original zones ----
        buildHillZone        (level, ox-80,  oy, oz-80);
        buildCliffZone       (level, ox+40,  oy, oz-90);
        buildPitZone         (level, ox-90,  oy, oz+40);
        buildStaircaseTower  (level, ox+70,  oy, oz+70);
        buildElevatedBridge  (level, ox-20,  oy, oz-110, 40);
        buildPillarField     (level, ox-15,  oy, oz-15);
        buildRampSlope       (level, ox+40,  oy, oz+10);
        buildFunnelPit       (level, ox-55,  oy, oz+80);
        buildSlimePad        (level, ox+90,  oy, oz+90);
        buildSandDune        (level, ox+80,  oy, oz-45);
        buildObstacleRow     (level, ox-110, oy, oz+0);
        buildZigzagChannel   (level, ox+15,  oy, oz+80);
        buildDropTower       (level, ox-15,  oy, oz+55);
        buildCraterPit       (level, ox+20,  oy, oz-50);
        buildRubbleHeap      (level, ox-40,  oy, oz-40);

        // ---- new zones ----
        buildMultiLevelPlatforms(level, ox-90,  oy, oz-30);
        buildWaterSlide         (level, ox+90,  oy, oz-10);
        buildCobwebTower        (level, ox+55,  oy, oz-70);
        buildNarrowCatwalk      (level, ox-65,  oy, oz+0);
        buildSpiralTower        (level, ox-30,  oy, oz+85);
        buildLavaMoatFort       (level, ox+55,  oy, oz+30);
        buildArenaFightPit      (level, ox+0,   oy, oz+40);
        buildLedgeWall          (level, ox-55,  oy, oz-55);
        buildBouncePyramid      (level, ox-10,  oy, oz-85);
        buildTerraceHillside    (level, ox+90,  oy, oz-90);
        buildTunnelSystem       (level, ox-90,  oy, oz-90);
        buildSwingPlatforms     (level, ox+20,  oy, oz-85);
        buildGiantBowl          (level, ox-55,  oy, oz+10);
        buildStaircaseGauntlet  (level, ox+80,  oy, oz-55);
        buildZiplineNetwork     (level, ox-10,  oy, oz-55);
        buildCagePlatform       (level, ox+30,  oy, oz+70);
        buildMazeWalls          (level, ox-80,  oy, oz+80);
        buildWaterfallCliff     (level, ox-30,  oy, oz-95);
        buildUndergroundCave    (level, ox+80,  oy, oz+20);
        buildAngledRamps        (level, ox-20,  oy, oz+100);
    }

    // =========================================================================
    //  PERIMETER WALL
    // =========================================================================
    private static void buildPerimeterWall(ServerLevel level, int ox, int oy, int oz, int r, int h) {
        BlockState s = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState w = Blocks.STONE_BRICK_WALL.defaultBlockState();
        fillBox(level, ox-r, oy+1, oz-r,   ox+r, oy+h, oz-r+1, s);
        fillBox(level, ox-r, oy+1, oz+r-1, ox+r, oy+h, oz+r,   s);
        fillBox(level, ox-r,   oy+1, oz-r, ox-r+1, oy+h, oz+r, s);
        fillBox(level, ox+r-1, oy+1, oz-r, ox+r,   oy+h, oz+r, s);
        for (int i = -r; i <= r; i+=2) {
            setBlock(level, ox+i, oy+h+1, oz-r, w);
            setBlock(level, ox+i, oy+h+1, oz+r, w);
            setBlock(level, ox-r, oy+h+1, oz+i, w);
            setBlock(level, ox+r, oy+h+1, oz+i, w);
        }
    }

    // =========================================================================
    //  HILL ZONE
    // =========================================================================
    private static void buildHillZone(ServerLevel level, int ox, int oy, int oz) {
        buildHill(level, ox,    oy, oz,    13, 1.7, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState());
        buildHill(level, ox+24, oy, oz+9,   9, 1.9, Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState());
        buildHill(level, ox+9,  oy, oz+28, 11, 1.6, Blocks.COARSE_DIRT.defaultBlockState(), Blocks.GRAVEL.defaultBlockState());
    }
    private static void buildHill(ServerLevel level, int cx, int oy, int cz,
                                  int radius, double steep, BlockState top, BlockState fill) {
        int maxH = (int)Math.ceil(radius * 1.5);
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx*dx + dz*dz);
                if (dist <= radius) {
                    int h = (int)Math.round(Math.pow(1.0-dist/radius, steep) * maxH);
                    for (int dy = 1; dy <= h; dy++)
                        setBlock(level, cx+dx, oy+dy, cz+dz, dy==h ? top : fill);
                }
            }
    }

    // =========================================================================
    //  CLIFF ZONE
    // =========================================================================
    private static void buildCliffZone(ServerLevel level, int ox, int oy, int oz) {
        int ph=22, w=22, d=26;
        fillBox(level, ox, oy+1, oz, ox+w, oy+ph, oz+d, Blocks.STONE.defaultBlockState());
        fillBox(level, ox, oy+ph+1, oz, ox+w, oy+ph+1, oz+d, Blocks.GRASS_BLOCK.defaultBlockState());
        int[] cxs={ox,ox+w}; int[] czs={oz,oz+d};
        for (int cx:cxs) for (int cz2:czs)
            fillBox(level, cx, oy+1, cz2, cx, oy+ph+5, cz2, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        for (int step=0; step<ph; step++)
            setBlock(level, ox-1, oy+step+1, oz+step, Blocks.STONE_STAIRS.defaultBlockState());
    }

    // =========================================================================
    //  PIT ZONE
    // =========================================================================
    private static void buildPitZone(ServerLevel level, int ox, int oy, int oz) {
        fillBox(level, ox,    oy-12, oz,    ox+16, oy-1, oz+16, Blocks.AIR.defaultBlockState());
        fillBox(level, ox+18, oy-22, oz,    ox+22, oy-1, oz+8,  Blocks.AIR.defaultBlockState());
        fillBox(level, ox,    oy-14, oz+18, ox+14, oy-1, oz+32, Blocks.AIR.defaultBlockState());
        fillBox(level, ox,    oy-14, oz+18, ox+14, oy-13,oz+32, Blocks.WATER.defaultBlockState());
        fillBox(level, ox+16, oy-12, oz+18, ox+24, oy-1, oz+32, Blocks.AIR.defaultBlockState());
        fillBox(level, ox+16, oy-12, oz+18, ox+24, oy-11,oz+32, Blocks.LAVA.defaultBlockState());
        for (int dy=0; dy<=12; dy++) {
            for (int i=0; i<=16; i++) {
                setBlock(level, ox+i, oy-dy, oz,    Blocks.STONE_BRICKS.defaultBlockState());
                setBlock(level, ox+i, oy-dy, oz+16, Blocks.STONE_BRICKS.defaultBlockState());
            }
            for (int i=1; i<16; i++) {
                setBlock(level, ox,    oy-dy, oz+i, Blocks.STONE_BRICKS.defaultBlockState());
                setBlock(level, ox+16, oy-dy, oz+i, Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
    }

    // =========================================================================
    //  STAIRCASE TOWER
    // =========================================================================
    private static void buildStaircaseTower(ServerLevel level, int ox, int oy, int oz) {
        int w=10, h=30;
        for (int y=1; y<=h; y++) {
            for (int x=0; x<=w; x++) {
                setBlock(level, ox+x, oy+y, oz,   Blocks.STONE_BRICKS.defaultBlockState());
                setBlock(level, ox+x, oy+y, oz+w, Blocks.STONE_BRICKS.defaultBlockState());
            }
            for (int z=1; z<w; z++) {
                setBlock(level, ox,   oy+y, oz+z, Blocks.STONE_BRICKS.defaultBlockState());
                setBlock(level, ox+w, oy+y, oz+z, Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        for (int step=0; step<h; step++) {
            int x = (step%2==0) ? 1+(step/4)%(w-2) : w-1-(step/4)%(w-2);
            int z = (step%4<2) ? 1 : w-1;
            setBlock(level, ox+x, oy+step+1, oz+z, Blocks.STONE_SLAB.defaultBlockState());
        }
        fillBox(level, ox+1, oy+h+1, oz+1, ox+w-1, oy+h+1, oz+w-1, Blocks.OAK_PLANKS.defaultBlockState());
    }

    // =========================================================================
    //  ELEVATED BRIDGE
    // =========================================================================
    private static void buildElevatedBridge(ServerLevel level, int ox, int oy, int oz, int length) {
        int by=oy+16;
        for (int i=0; i<=length; i+=5)
            fillBox(level, ox+i, oy+1, oz+1, ox+i, by-1, oz+1, Blocks.OAK_LOG.defaultBlockState());
        fillBox(level, ox, by, oz, ox+length, by, oz+2, Blocks.OAK_PLANKS.defaultBlockState());
        for (int i=0; i<=length; i++) {
            setBlock(level, ox+i, by+1, oz,   Blocks.OAK_FENCE.defaultBlockState());
            setBlock(level, ox+i, by+1, oz+2, Blocks.OAK_FENCE.defaultBlockState());
        }
    }

    // =========================================================================
    //  PILLAR FIELD
    // =========================================================================
    private static void buildPillarField(ServerLevel level, int ox, int oy, int oz) {
        int[] hs={8,14,20,10,18,6,22,12,16,24,9,17};
        int idx=0;
        for (int dx=0; dx<=20; dx+=4) for (int dz=0; dz<=20; dz+=4) {
            int h=hs[idx%hs.length];
            fillBox(level, ox+dx, oy+1, oz+dz, ox+dx, oy+h, oz+dz, Blocks.POLISHED_ANDESITE.defaultBlockState());
            setBlock(level, ox+dx, oy+h+1, oz+dz, Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState());
            idx++;
        }
    }

    // =========================================================================
    //  RAMP 45 DEG
    // =========================================================================
    private static void buildRampSlope(ServerLevel level, int ox, int oy, int oz) {
        for (int i=0; i<22; i++) {
            fillBox(level, ox+i, oy+1, oz, ox+i, oy+i, oz+7, Blocks.SMOOTH_STONE.defaultBlockState());
            for (int dz=0; dz<=7; dz++)
                setBlock(level, ox+i, oy+i+1, oz+dz, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        }
        fillBox(level, ox+22, oy+23, oz-2, ox+32, oy+23, oz+9, Blocks.SMOOTH_STONE.defaultBlockState());
    }

    // =========================================================================
    //  FUNNEL PIT
    // =========================================================================
    private static void buildFunnelPit(ServerLevel level, int ox, int oy, int oz) {
        int radius=11, maxD=16;
        for (int dx=-radius; dx<=radius; dx++) for (int dz=-radius; dz<=radius; dz++) {
            double dist=Math.sqrt(dx*dx+dz*dz);
            if (dist<=radius) {
                double t=1.0-dist/radius;
                int depth=(int)Math.round(t*t*maxD);
                for (int dy=0; dy>=-depth; dy--) setBlock(level, ox+dx, oy+dy, oz+dz, Blocks.AIR.defaultBlockState());
                if (dist>radius-1.5) setBlock(level, ox+dx, oy+1, oz+dz, Blocks.STONE.defaultBlockState());
            }
        }
    }

    // =========================================================================
    //  SLIME PAD
    // =========================================================================
    private static void buildSlimePad(ServerLevel level, int ox, int oy, int oz) {
        fillBox(level, ox-6, oy+1, oz-6, ox+6, oy+1, oz+6, Blocks.SLIME_BLOCK.defaultBlockState());
        for (int i=-7; i<=7; i++) for (int row=1; row<=3; row++) {
            setBlock(level, ox+i, oy+row, oz-7, Blocks.LIME_CONCRETE.defaultBlockState());
            setBlock(level, ox+i, oy+row, oz+7, Blocks.LIME_CONCRETE.defaultBlockState());
            setBlock(level, ox-7, oy+row, oz+i, Blocks.LIME_CONCRETE.defaultBlockState());
            setBlock(level, ox+7, oy+row, oz+i, Blocks.LIME_CONCRETE.defaultBlockState());
        }
    }

    // =========================================================================
    //  SAND DUNE
    // =========================================================================
    private static void buildSandDune(ServerLevel level, int ox, int oy, int oz) {
        for (int i=0; i<22; i++) {
            int h=(int)(Math.sin((double)i/22*Math.PI)*15)+1;
            fillBox(level, ox+i, oy+1, oz, ox+i, oy+h, oz+14, Blocks.SAND.defaultBlockState());
            if (h>2) fillBox(level, ox+i, oy+1, oz, ox+i, oy+h-1, oz+14, Blocks.SANDSTONE.defaultBlockState());
        }
    }

    // =========================================================================
    //  OBSTACLE ROW
    // =========================================================================
    private static void buildObstacleRow(ServerLevel level, int ox, int oy, int oz) {
        int[] hs={9,5,14,6,12,4,10,7,11};
        for (int i=0; i<hs.length; i++) {
            int x=ox+i*9;
            if (i%2==0) {
                fillBox(level, x, oy+1, oz-6, x, oy+2,    oz+6, Blocks.BRICKS.defaultBlockState());
                fillBox(level, x, oy+5, oz-6, x, oy+hs[i], oz+6, Blocks.BRICKS.defaultBlockState());
            } else {
                fillBox(level, x, oy+1, oz-6, x, oy+hs[i], oz+6, Blocks.BRICKS.defaultBlockState());
            }
        }
    }

    // =========================================================================
    //  ZIGZAG CHANNEL
    // =========================================================================
    private static void buildZigzagChannel(ServerLevel level, int ox, int oy, int oz) {
        int depth=12;
        BlockState wall=Blocks.COBBLESTONE.defaultBlockState(), floor=Blocks.GRAVEL.defaultBlockState();
        for (int i=0; i<14; i++) {
            for (int dy=0; dy>=-depth; dy--) setBlock(level, ox, oy+dy, oz+i, Blocks.AIR.defaultBlockState());
            setBlock(level, ox, oy-depth-1, oz+i, floor);
            setBlock(level, ox-1, oy+1, oz+i, wall); setBlock(level, ox+1, oy+1, oz+i, wall);
        }
        for (int i=0; i<14; i++) {
            for (int dy=0; dy>=-depth; dy--) setBlock(level, ox+i, oy+dy, oz+14, Blocks.AIR.defaultBlockState());
            setBlock(level, ox+i, oy-depth-1, oz+14, floor);
            setBlock(level, ox+i, oy+1, oz+13, wall); setBlock(level, ox+i, oy+1, oz+15, wall);
        }
        for (int i=0; i<12; i++) {
            for (int dy=0; dy>=-depth; dy--) setBlock(level, ox+14, oy+dy, oz+14+i, Blocks.AIR.defaultBlockState());
            setBlock(level, ox+14, oy-depth-1, oz+14+i, floor);
            setBlock(level, ox+13, oy+1, oz+14+i, wall); setBlock(level, ox+15, oy+1, oz+14+i, wall);
        }
    }

    // =========================================================================
    //  DROP TOWER
    // =========================================================================
    private static void buildDropTower(ServerLevel level, int ox, int oy, int oz) {
        int h=38;
        for (int y=1; y<=h; y++)
            for (int dx=0; dx<=3; dx++) for (int dz=0; dz<=3; dz++)
                if (dx==0||dx==3||dz==0||dz==3)
                    setBlock(level, ox+dx, oy+y, oz+dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        for (int y=1; y<=h; y++)
            setBlock(level, ox+1, oy+y, oz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        fillBox(level, ox-2, oy+h+1, oz-2, ox+5, oy+h+1, oz+5, Blocks.OAK_PLANKS.defaultBlockState());
    }

    // =========================================================================
    //  CRATER PIT
    // =========================================================================
    private static void buildCraterPit(ServerLevel level, int ox, int oy, int oz) {
        int radius=13;
        for (int dx=-radius; dx<=radius; dx++) for (int dz=-radius; dz<=radius; dz++)
            for (int dy=-radius+2; dy<=2; dy++) {
                double dist=Math.sqrt(dx*dx+dy*dy*0.6+dz*dz);
                if (dist<=radius-Math.abs(dy)*0.2) setBlock(level, ox+dx, oy+dy, oz+dz, Blocks.AIR.defaultBlockState());
            }
        for (int dx=-radius; dx<=radius; dx++) for (int dz=-radius; dz<=radius; dz++) {
            double dist=Math.sqrt(dx*dx+dz*dz);
            if (dist>=radius-1&&dist<=radius+1) setBlock(level, ox+dx, oy+1, oz+dz, Blocks.COARSE_DIRT.defaultBlockState());
        }
    }

    // =========================================================================
    //  RUBBLE HEAP
    // =========================================================================
    private static void buildRubbleHeap(ServerLevel level, int ox, int oy, int oz) {
        java.util.Random rng=new java.util.Random(42);
        BlockState[] blocks={Blocks.COBBLESTONE.defaultBlockState(),Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState(),Blocks.STONE.defaultBlockState(),
            Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),Blocks.OAK_LOG.defaultBlockState()};
        for (int i=0; i<400; i++) {
            int dx=rng.nextInt(26)-6, dz=rng.nextInt(26)-6, dy=rng.nextInt(7)+1;
            setBlock(level, ox+dx, oy+dy, oz+dz, blocks[rng.nextInt(blocks.length)]);
        }
    }

    // =========================================================================
    //  MULTI-LEVEL PLATFORMS
    // =========================================================================
    private static void buildMultiLevelPlatforms(ServerLevel level, int ox, int oy, int oz) {
        int[] hs={5,10,16,23,31};
        int[][] off={{0,0},{10,5},{-5,12},{8,-8},{0,20}};
        for (int i=0; i<hs.length; i++) {
            int px=ox+off[i][0], pz=oz+off[i][1], py=oy+hs[i];
            fillBox(level, px-4, py, pz-4, px+4, py, pz+4, Blocks.OAK_PLANKS.defaultBlockState());
            for (int d=-4; d<=4; d++) {
                setBlock(level, px+d, py+1, pz-4, Blocks.OAK_FENCE.defaultBlockState());
                setBlock(level, px-4, py+1, pz+d, Blocks.OAK_FENCE.defaultBlockState());
                setBlock(level, px+4, py+1, pz+d, Blocks.OAK_FENCE.defaultBlockState());
            }
            if (i<hs.length-1) {
                int ladX=px+3, nextPy=oy+hs[i+1];
                fillBox(level, ladX, py+1, pz+3, ladX, nextPy-1, pz+3, Blocks.OAK_LOG.defaultBlockState());
                for (int ly=py+1; ly<nextPy; ly++)
                    setBlock(level, ladX-1, ly, pz+3, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
            }
        }
    }

    // =========================================================================
    //  WATER SLIDE
    // =========================================================================
    private static void buildWaterSlide(ServerLevel level, int ox, int oy, int oz) {
        int tH=25;
        fillBox(level, ox, oy+1, oz, ox+3, oy+tH, oz+3, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox+1, oy+1, oz+1, ox+2, oy+tH, oz+2, Blocks.AIR.defaultBlockState());
        for (int y=1; y<=tH; y++)
            setBlock(level, ox+1, oy+y, oz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        for (int i=0; i<tH; i++) {
            int slideY=oy+tH-i, slideZ=oz+4+i;
            setBlock(level, ox+1, slideY,   slideZ, Blocks.PACKED_ICE.defaultBlockState());
            setBlock(level, ox,   slideY+1, slideZ, Blocks.STONE_BRICKS.defaultBlockState());
            setBlock(level, ox+2, slideY+1, slideZ, Blocks.STONE_BRICKS.defaultBlockState());
            setBlock(level, ox+1, slideY+1, slideZ, Blocks.WATER.defaultBlockState());
        }
        int poolZ=oz+4+tH;
        fillBox(level, ox-3, oy-4, poolZ, ox+6, oy-1, poolZ+8, Blocks.WATER.defaultBlockState());
        fillBox(level, ox-4, oy-5, poolZ-1, ox+7, oy, poolZ-1, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox-4, oy-5, poolZ+9, ox+7, oy, poolZ+9, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox-4, oy-5, poolZ-1, ox-4, oy, poolZ+9, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox+7, oy-5, poolZ-1, ox+7, oy, poolZ+9, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox-4, oy-5, poolZ-1, ox+7, oy-5, poolZ+9, Blocks.STONE_BRICKS.defaultBlockState());
    }

    // =========================================================================
    //  COBWEB TOWER
    // =========================================================================
    private static void buildCobwebTower(ServerLevel level, int ox, int oy, int oz) {
        int h=20, r=5;
        for (int y=1; y<=h; y++)
            for (int dx=-r; dx<=r; dx++) for (int dz=-r; dz<=r; dz++) {
                double dist=Math.sqrt(dx*dx+dz*dz);
                if (dist>=r-0.5&&dist<=r+0.5) setBlock(level, ox+dx, oy+y, oz+dz, Blocks.STONE_BRICKS.defaultBlockState());
                else if (dist<r-0.5&&y%3==0) setBlock(level, ox+dx, oy+y, oz+dz, Blocks.COBWEB.defaultBlockState());
            }
        fillBox(level, ox-r+1, oy+h+1, oz-r+1, ox+r-1, oy+h+1, oz+r-1, Blocks.AIR.defaultBlockState());
        for (int y=1; y<=h; y++)
            setBlock(level, ox, oy+y, oz-r-1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        fillBox(level, ox-1, oy, oz-r-2, ox+1, oy+h+1, oz-r-2, Blocks.STONE_BRICKS.defaultBlockState());
    }

    // =========================================================================
    //  NARROW CATWALK
    // =========================================================================
    private static void buildNarrowCatwalk(ServerLevel level, int ox, int oy, int oz) {
        int catH=18, len=40;
        fillBox(level, ox, oy+1, oz, ox+1, oy+catH-1, oz+1, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox+len, oy+1, oz, ox+len+1, oy+catH-1, oz+1, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox, oy+catH, oz, ox+len, oy+catH, oz, Blocks.IRON_BLOCK.defaultBlockState());
        for (int y=1; y<catH; y++)
            setBlock(level, ox, oy+y, oz-1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        for (int i=10; i<len; i+=10)
            fillBox(level, ox+i-1, oy+catH, oz-1, ox+i+1, oy+catH, oz+1, Blocks.OAK_PLANKS.defaultBlockState());
    }

    // =========================================================================
    //  SPIRAL TOWER
    // =========================================================================
    private static void buildSpiralTower(ServerLevel level, int ox, int oy, int oz) {
        int h=35, coreR=3;
        fillBox(level, ox-coreR, oy+1, oz-coreR, ox+coreR, oy+h, oz+coreR, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox-coreR+1, oy+1, oz-coreR+1, ox+coreR-1, oy+h, oz+coreR-1, Blocks.AIR.defaultBlockState());
        double aStep=Math.PI*2.0/14.0; int sR=coreR+3;
        for (int y=1; y<=h; y++) {
            double angle=y*aStep;
            int sx=ox+(int)Math.round(Math.cos(angle)*sR), sz=oz+(int)Math.round(Math.sin(angle)*sR);
            setBlock(level, sx, oy+y, sz, Blocks.STONE_BRICKS.defaultBlockState());
            setBlock(level, sx, oy+y+1, sz, Blocks.STONE_BRICK_WALL.defaultBlockState());
        }
        fillBox(level, ox-coreR, oy+h+1, oz-coreR, ox+coreR, oy+h+1, oz+coreR, Blocks.OAK_PLANKS.defaultBlockState());
    }

    // =========================================================================
    //  LAVA MOAT FORT
    // =========================================================================
    private static void buildLavaMoatFort(ServerLevel level, int ox, int oy, int oz) {
        int fH=6, mW=4;
        fillBox(level, ox, oy+1, oz, ox+20, oy+fH, oz+20, Blocks.STONE.defaultBlockState());
        fillBox(level, ox+2, oy+fH+1, oz+2, ox+18, oy+fH+1, oz+18, Blocks.SMOOTH_STONE.defaultBlockState());
        for (int y=fH+1; y<=fH+5; y++) {
            for (int i=0; i<=20; i++) {
                setBlock(level, ox+i, oy+y, oz,    Blocks.STONE_BRICKS.defaultBlockState());
                setBlock(level, ox+i, oy+y, oz+20, Blocks.STONE_BRICKS.defaultBlockState());
            }
            for (int i=1; i<20; i++) {
                setBlock(level, ox,    oy+y, oz+i, Blocks.STONE_BRICKS.defaultBlockState());
                setBlock(level, ox+20, oy+y, oz+i, Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        fillBox(level, ox-mW, oy-2, oz-mW, ox+20+mW, oy, oz-1,    Blocks.LAVA.defaultBlockState());
        fillBox(level, ox-mW, oy-2, oz+21,  ox+20+mW, oy, oz+20+mW, Blocks.LAVA.defaultBlockState());
        fillBox(level, ox-mW, oy-2, oz,     ox-1,      oy, oz+20, Blocks.LAVA.defaultBlockState());
        fillBox(level, ox+21, oy-2, oz,     ox+20+mW,  oy, oz+20, Blocks.LAVA.defaultBlockState());
        fillBox(level, ox+8, oy+1, oz-mW, ox+12, oy+1, oz-1, Blocks.OAK_PLANKS.defaultBlockState());
    }

    // =========================================================================
    //  ARENA FIGHT PIT
    // =========================================================================
    private static void buildArenaFightPit(ServerLevel level, int ox, int oy, int oz) {
        int radius=16;
        for (int dx=-radius; dx<=radius; dx++) for (int dz=-radius; dz<=radius; dz++) {
            double dist=Math.sqrt(dx*dx+dz*dz);
            if (dist<=radius) for (int dy=-8; dy<=0; dy++) setBlock(level, ox+dx, oy+dy, oz+dz, Blocks.AIR.defaultBlockState());
        }
        for (int dx=-(radius-1); dx<=radius-1; dx++) for (int dz=-(radius-1); dz<=radius-1; dz++)
            if (Math.sqrt(dx*dx+dz*dz)<=radius-1) setBlock(level, ox+dx, oy-8, oz+dz, Blocks.SAND.defaultBlockState());
        for (int tier=0; tier<4; tier++) {
            int iR=radius+tier*2, tH=oy+tier*2;
            for (int dx=-iR-2; dx<=iR+2; dx++) for (int dz=-iR-2; dz<=iR+2; dz++) {
                double dist=Math.sqrt(dx*dx+dz*dz);
                if (dist>=iR&&dist<=iR+2)
                    setBlock(level, ox+dx, tH, oz+dz, tier%2==0 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
    }

    // =========================================================================
    //  LEDGE WALL
    // =========================================================================
    private static void buildLedgeWall(ServerLevel level, int ox, int oy, int oz) {
        int wallH=30, wallLen=30;
        fillBox(level, ox, oy+1, oz, ox, oy+wallH, oz+wallLen, Blocks.STONE_BRICKS.defaultBlockState());
        for (int y=4; y<=wallH; y+=4) {
            int ledZ=oz+(y/4)*4%wallLen;
            fillBox(level, ox+1, oy+y, ledZ, ox+4, oy+y, ledZ+3, Blocks.STONE_SLAB.defaultBlockState());
            if ((y/4)%2==0)
                fillBox(level, ox-4, oy+y-2, ledZ+5, ox-1, oy+y-2, ledZ+8, Blocks.STONE_SLAB.defaultBlockState());
        }
        for (int y=1; y<=wallH; y++)
            setBlock(level, ox, oy+y, oz-1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
    }

    // =========================================================================
    //  BOUNCE PYRAMID
    // =========================================================================
    private static void buildBouncePyramid(ServerLevel level, int ox, int oy, int oz) {
        int maxR=12;
        for (int layer=0; layer<=maxR; layer++) {
            int r=maxR-layer;
            BlockState mat=layer%2==0 ? Blocks.SLIME_BLOCK.defaultBlockState() : Blocks.HONEY_BLOCK.defaultBlockState();
            for (int dx=-r; dx<=r; dx++) for (int dz=-r; dz<=r; dz++)
                if (Math.sqrt(dx*dx+dz*dz)<=r) setBlock(level, ox+dx, oy+layer+1, oz+dz, mat);
        }
    }

    // =========================================================================
    //  TERRACE HILLSIDE
    // =========================================================================
    private static void buildTerraceHillside(ServerLevel level, int ox, int oy, int oz) {
        int steps=8, stepH=4, stepW=5;
        for (int step=0; step<steps; step++) {
            int yBase=oy+step*stepH, xMin=ox+step*stepW, xMax=ox+steps*stepW;
            fillBox(level, xMin, yBase+1, oz, xMax, yBase+stepH, oz+20, Blocks.STONE.defaultBlockState());
            fillBox(level, xMin, yBase+stepH+1, oz, xMax, yBase+stepH+1, oz+20, Blocks.GRASS_BLOCK.defaultBlockState());
        }
    }

    // =========================================================================
    //  TUNNEL SYSTEM
    // =========================================================================
    private static void buildTunnelSystem(ServerLevel level, int ox, int oy, int oz) {
        int tY=oy-8;
        fillBox(level, ox-1, tY-1, oz-1, ox+41, tY+4, oz+5, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox,   tY,   oz,   ox+40, tY+3, oz+4, Blocks.AIR.defaultBlockState());
        fillBox(level, ox+19, tY-1, oz-21, ox+25, tY+4, oz+5, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox+20, tY,   oz-20, ox+24, tY+3, oz+4, Blocks.AIR.defaultBlockState());
        fillBox(level, ox+9,  tY-1, oz+3, ox+15, tY+4, oz+25, Blocks.STONE_BRICKS.defaultBlockState());
        fillBox(level, ox+10, tY,   oz+4, ox+14, tY+3, oz+24, Blocks.AIR.defaultBlockState());
        fillBox(level, ox,   tY, oz+1, ox+40, tY, oz+3, Blocks.GRAVEL.defaultBlockState());
        // Surface entries
        fillBox(level, ox+2,  tY+4, oz+1, ox+4,  oy, oz+3, Blocks.AIR.defaultBlockState());
        fillBox(level, ox+20, tY+4, oz-18, ox+22, oy, oz-16, Blocks.AIR.defaultBlockState());
        fillBox(level, ox+11, tY+4, oz+6, ox+13, oy, oz+8, Blocks.AIR.defaultBlockState());
    }

    // =========================================================================
    //  SWING PLATFORMS
    // =========================================================================
    private static void buildSwingPlatforms(ServerLevel level, int ox, int oy, int oz) {
        int[][] plats={{0,8},{12,14},{24,9},{36,18},{48,12}};
        for (int i=0; i<plats.length; i++) {
            int px=ox+plats[i][0], ph=oy+plats[i][1];
            fillBox(level, px-2, ph, oz-2, px+2, ph, oz+2, Blocks.CRIMSON_PLANKS.defaultBlockState());
            fillBox(level, px,   oy+1, oz, px, ph-1, oz, Blocks.CRIMSON_STEM.defaultBlockState());
            if (i<plats.length-1) {
                int nx=ox+plats[i+1][0], nh=oy+plats[i+1][1];
                int steps=nx-px; if (steps==0) continue;
                for (int s=0; s<=steps; s++) {
                    int fy=(int)Math.round(ph*(1.0-s/(double)steps)+nh*(s/(double)steps)-Math.sin(s/(double)steps*Math.PI)*3);
                    setBlock(level, px+s, fy, oz, Blocks.OAK_FENCE.defaultBlockState());
                }
            }
        }
    }

    // =========================================================================
    //  GIANT BOWL
    // =========================================================================
    private static void buildGiantBowl(ServerLevel level, int ox, int oy, int oz) {
        int radius=18, rimH=8;
        for (int dx=-radius; dx<=radius; dx++) for (int dz=-radius; dz<=radius; dz++) {
            double dist=Math.sqrt(dx*dx+dz*dz);
            if (dist<=radius) {
                double t=1.0-dist/radius;
                int depth=rimH-(int)Math.round(t*t*rimH);
                setBlock(level, ox+dx, oy-depth, oz+dz, Blocks.SMOOTH_STONE.defaultBlockState());
                fillBox(level, ox+dx, oy-rimH, oz+dz, ox+dx, oy-depth-1, oz+dz, Blocks.STONE.defaultBlockState());
                for (int dy=-depth+1; dy<=0; dy++) setBlock(level, ox+dx, oy+dy, oz+dz, Blocks.AIR.defaultBlockState());
            }
        }
    }

    // =========================================================================
    //  STAIRCASE GAUNTLET
    // =========================================================================
    private static void buildStaircaseGauntlet(ServerLevel level, int ox, int oy, int oz) {
        for (int i=0; i<20; i++) {
            fillBox(level, ox+i, oy+1, oz, ox+i, oy+i, oz+5, Blocks.STONE.defaultBlockState());
            setBlock(level, ox+i, oy+i+1, oz+2, Blocks.STONE_STAIRS.defaultBlockState());
        }
        fillBox(level, ox+20, oy+21, oz-2, ox+30, oy+21, oz+7, Blocks.STONE.defaultBlockState());
        for (int i=0; i<20; i++) {
            int yy=20-i;
            fillBox(level, ox+30+i, oy+1, oz, ox+30+i, oy+yy, oz+5, Blocks.STONE.defaultBlockState());
            setBlock(level, ox+30+i, oy+yy+1, oz+2, Blocks.STONE_STAIRS.defaultBlockState());
        }
    }

    // =========================================================================
    //  ZIPLINE NETWORK
    // =========================================================================
    private static void buildZiplineNetwork(ServerLevel level, int ox, int oy, int oz) {
        int[][] poles={{0,22},{20,18},{-15,24},{10,20},{-5,15}};
        int[] poleHs={22,18,25,20,16};
        for (int i=0; i<poles.length; i++) {
            int px=ox+poles[i][0], pz=oz+poles[i][1], ph=oy+poleHs[i];
            fillBox(level, px, oy+1, pz, px, ph, pz, Blocks.OAK_LOG.defaultBlockState());
            setBlock(level, px, ph+1, pz, Blocks.OAK_PLANKS.defaultBlockState());
        }
        for (int i=0; i<poles.length-1; i++) {
            int x1=ox+poles[i][0], y1=oy+poleHs[i], z1=oz+poles[i][1];
            int x2=ox+poles[i+1][0], y2=oy+poleHs[i+1], z2=oz+poles[i+1][1];
            int steps=Math.max(Math.abs(x2-x1), Math.abs(z2-z1));
            if (steps==0) continue;
            for (int s=0; s<=steps; s++) {
                int fx=x1+(x2-x1)*s/steps, fy=y1+(y2-y1)*s/steps, fz=z1+(z2-z1)*s/steps;
                setBlock(level, fx, fy, fz, Blocks.OAK_FENCE.defaultBlockState());
            }
        }
    }

    // =========================================================================
    //  CAGE PLATFORM
    // =========================================================================
    private static void buildCagePlatform(ServerLevel level, int ox, int oy, int oz) {
        int cH=20, cS=12;
        int[] corners={0, cS};
        for (int cx:corners) for (int cz:corners)
            fillBox(level, ox+cx, oy+1, oz+cz, ox+cx, oy+cH+4, oz+cz, Blocks.IRON_BARS.defaultBlockState());
        for (int i=0; i<=cS; i++) for (int y=cH; y<=cH+4; y++) {
            setBlock(level, ox+i, oy+y, oz,    Blocks.IRON_BARS.defaultBlockState());
            setBlock(level, ox+i, oy+y, oz+cS, Blocks.IRON_BARS.defaultBlockState());
            setBlock(level, ox,   oy+y, oz+i,  Blocks.IRON_BARS.defaultBlockState());
            setBlock(level, ox+cS, oy+y, oz+i, Blocks.IRON_BARS.defaultBlockState());
        }
        for (int dx=1; dx<cS; dx++) for (int dz=1; dz<cS; dz++)
            if ((dx+dz)%2==0) setBlock(level, ox+dx, oy+cH, oz+dz, Blocks.STONE_SLAB.defaultBlockState());
        for (int y=1; y<cH; y++)
            setBlock(level, ox-1, oy+y, oz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
    }

    // =========================================================================
    //  MAZE WALLS
    // =========================================================================
    private static void buildMazeWalls(ServerLevel level, int ox, int oy, int oz) {
        int[][] maze={
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,1,0,0,0,0,0,1,0,0,0,1},
            {1,0,1,0,1,0,1,1,1,0,1,0,1,0,1},
            {1,0,1,0,0,0,0,0,1,0,0,0,1,0,1},
            {1,0,1,1,1,1,1,0,1,1,1,0,1,0,1},
            {1,0,0,0,0,0,1,0,0,0,1,0,0,0,1},
            {1,1,1,0,1,0,1,0,1,0,1,1,1,0,1},
            {1,0,0,0,1,0,0,0,1,0,0,0,0,0,1},
            {1,0,1,1,1,1,1,1,1,0,1,1,1,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,1,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        };
        int scale=3;
        for (int row=0; row<maze.length; row++) for (int col=0; col<maze[row].length; col++)
            if (maze[row][col]==1) {
                int bx=ox+col*scale, bz=oz+row*scale;
                fillBox(level, bx, oy+1, bz, bx+scale-1, oy+5, bz+scale-1, Blocks.STONE_BRICKS.defaultBlockState());
            }
    }

    // =========================================================================
    //  WATERFALL CLIFF
    // =========================================================================
    private static void buildWaterfallCliff(ServerLevel level, int ox, int oy, int oz) {
        int h=30, w=20;
        fillBox(level, ox, oy+1, oz, ox+w, oy+h, oz+5, Blocks.STONE.defaultBlockState());
        fillBox(level, ox, oy+h+1, oz, ox+w, oy+h+1, oz+5, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int wc:new int[]{3,9,16}) for (int y=oy+2; y<=oy+h; y++) setBlock(level, ox+wc, y, oz, Blocks.WATER.defaultBlockState());
        fillBox(level, ox-2, oy-4, oz-6, ox+w+2, oy-1, oz, Blocks.WATER.defaultBlockState());
        fillBox(level, ox-3, oy-5, oz-7, ox+w+3, oy-5, oz+1, Blocks.STONE.defaultBlockState());
        fillBox(level, ox-3, oy-5, oz-7, ox-3,   oy,   oz+1, Blocks.STONE.defaultBlockState());
        fillBox(level, ox+w+3, oy-5, oz-7, ox+w+3, oy, oz+1, Blocks.STONE.defaultBlockState());
        fillBox(level, ox-3, oy-5, oz-7, ox+w+3, oy, oz-7, Blocks.STONE.defaultBlockState());
    }

    // =========================================================================
    //  UNDERGROUND CAVE
    // =========================================================================
    private static void buildUndergroundCave(ServerLevel level, int ox, int oy, int oz) {
        int cH=10, cR=15, caveY=oy-5;
        for (int dx=-cR; dx<=cR; dx++) for (int dz=-cR; dz<=cR; dz++)
            if (Math.sqrt(dx*dx+dz*dz)<=cR)
                fillBox(level, ox+dx, caveY-cH+2, oz+dz, ox+dx, caveY, oz+dz, Blocks.AIR.defaultBlockState());
        fillBox(level, ox-2, caveY+1, oz-2, ox+2, oy, oz+2, Blocks.AIR.defaultBlockState());
        java.util.Random rng=new java.util.Random(77);
        for (int i=0; i<40; i++) {
            int dx=rng.nextInt(cR*2)-cR, dz=rng.nextInt(cR*2)-cR;
            if (Math.sqrt(dx*dx+dz*dz)<=cR-2) {
                int sLen=rng.nextInt(4)+2;
                for (int s=0; s<sLen; s++) setBlock(level, ox+dx, caveY-s, oz+dz, Blocks.POINTED_DRIPSTONE.defaultBlockState());
            }
        }
        fillBox(level, ox-5, caveY-cH+2, oz-5, ox+5, caveY-cH+2, oz+5, Blocks.LAVA.defaultBlockState());
    }

    // =========================================================================
    //  ANGLED RAMPS (3 angles side by side)
    // =========================================================================
    private static void buildAngledRamps(ServerLevel level, int ox, int oy, int oz) {
        for (int i=0; i<30; i++) {
            int h=i/3;
            fillBox(level, ox+i, oy+1, oz, ox+i, oy+h, oz+5, Blocks.SMOOTH_STONE.defaultBlockState());
            for (int dz=0; dz<=5; dz++) setBlock(level, ox+i, oy+h+1, oz+dz, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        }
        for (int i=0; i<24; i++) {
            int h=i/2;
            fillBox(level, ox+i, oy+1, oz+8, ox+i, oy+h, oz+13, Blocks.COBBLESTONE.defaultBlockState());
            for (int dz=0; dz<=5; dz++) setBlock(level, ox+i, oy+h+1, oz+8+dz, Blocks.COBBLESTONE_SLAB.defaultBlockState());
        }
        for (int i=0; i<20; i++) {
            int h=i;
            fillBox(level, ox+i, oy+1, oz+16, ox+i, oy+h, oz+21, Blocks.STONE.defaultBlockState());
            for (int dz=0; dz<=5; dz++) setBlock(level, ox+i, oy+h+1, oz+16+dz, Blocks.STONE_SLAB.defaultBlockState());
        }
        fillBox(level, ox+28, oy+11, oz-1, ox+38, oy+11, oz+22, Blocks.SMOOTH_STONE.defaultBlockState());
    }

    // =========================================================================
    //  UTILITY
    // =========================================================================
    private static void fillBox(ServerLevel level,
                                int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x=Math.min(x1,x2); x<=Math.max(x1,x2); x++)
            for (int y=Math.min(y1,y2); y<=Math.max(y1,y2); y++)
                for (int z=Math.min(z1,z2); z<=Math.max(z1,z2); z++)
                    level.setBlock(new BlockPos(x,y,z), state, 2);
    }
    private static void clearVolume(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        fillBox(level, x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
    }
    private static void setBlock(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x,y,z), state, 2);
    }
}
