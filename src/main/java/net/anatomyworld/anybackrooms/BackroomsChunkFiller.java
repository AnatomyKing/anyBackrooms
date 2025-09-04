package net.anatomyworld.anybackrooms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backrooms chunk filler
 *
 * What this does:
 * 1. Plans a maze-like region from the world seed.
 * 2. Draws corridors + rooms + cozy rings + special reward rooms.
 * 3. Places lights nicely.
 * 4. Lays carpet, walls, and a simple ceiling.
 * We store only EAST and SOUTH openings per cell,
 * and derive WEST and NORTH from neighbors when we need them.
 */
public final class BackroomsChunkFiller {

    private BackroomsChunkFiller() {} // no instances

    // World height & materials
    private static final int FLOOR_Y          = 80;      // where the floor lives
    private static final int HEADROOM         = 5;       // airspace above the floor
    private static final int CEILING_PAD      = 2;       // extra offset for the ceiling slab
    private static final int VISIBLE_WALL_UP  = HEADROOM + 4; // how high the wall we build is


    // Blocks we use (simple palette)
    private static final BlockState WALL              = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
    private static final BlockState WALL_TRIM         = Blocks.SMOOTH_SANDSTONE.defaultBlockState(); // baseboard at y+2
    private static final BlockState UNDER_CEILING     = Blocks.STONE.defaultBlockState();
    private static final BlockState CARPET            = Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
    private static final BlockState BEDROCK           = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState LAMP_ON           = Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true);
    private static final BlockState REDSTONE_BLOCK    = Blocks.REDSTONE_BLOCK.defaultBlockState();
    private static final BlockState CEILING_SLAB_TOP  = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);

    // Grid shape (cells inside a “tile”, plus a thin wall line)
    private static final int CELL_SIZE     = 6;                  // walk area inside each tile
    private static final int TILE_SIZE     = CELL_SIZE + 1;      // +1 line where walls live
    private static final int CELL_CENTER   = CELL_SIZE >> 1;     // middle of the walking area

    // Regions help us plan bigger areas at once (fast + deterministic)
    private static final int REGION_CELLS  = 32;                  // cells per region (both X and Z)
    private static final int REGION_BLOCKS = REGION_CELLS * TILE_SIZE;

    // Generation “knobs” (tweak feel)
    private static final double EXTRA_BREAKS_CHANCE = 0.08; // add a few loops to the maze

    // Mega “open” rectangles (big plazas)
    private static final double MEGA_CHANCE  = 0.40;
    private static final int    MEGA_COUNT_MIN = 0, MEGA_COUNT_MAX = 2;
    private static final int    MEGA_W_MIN = 8, MEGA_W_MAX = 14, MEGA_H_MIN = 8, MEGA_H_MAX = 14;

    // Normal rooms
    private static final double ROOM_CHANCE  = 0.80;
    private static final int    ROOM_COUNT_MIN = 1, ROOM_COUNT_MAX = 3;
    private static final int    ROOM_W_MIN = 3, ROOM_W_MAX = 7, ROOM_H_MIN = 3, ROOM_H_MAX = 7;

    // “Cozy” rooms (sealed ring with a couple doors)
    private static final int    COZY_COUNT_MIN = 1, COZY_COUNT_MAX = 3;
    private static final int    COZY_W_MIN = 3, COZY_W_MAX = 6, COZY_H_MIN = 3, COZY_H_MAX = 5;
    private static final int    COZY_DOORS_MIN = 1, COZY_DOORS_MAX = 2;

    // Reward rooms at dead-ends (bigger special spaces)
    private static final int    REWARD_W_MIN = 6, REWARD_W_MAX = 10, REWARD_H_MIN = 5, REWARD_H_MAX = 9, REWARD_MAX_PER_REGION = 4;


    // Lighting rules
    private static final int LAMP_STRIP_HALF      = 1;  // corridor lights form a 2-wide strip across center
    private static final int LAMP_SPACING         = 6;  // how far apart along the corridor
    private static final int JUNCTION_HALF        = 0;  // at a junction, use the exact center point
    private static final int ROOM_LAMP_SPACING    = 8;  // room grid spacing
    private static final int ROOM_LAMP_MARGIN     = 2;  // keep lamps a bit away from walls

    // draw everything inside this chunk
    public static void fill(ServerLevel level, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();

        // Which region are we in?
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int regionX = Math.floorDiv(minX, REGION_BLOCKS);
        int regionZ = Math.floorDiv(minZ, REGION_BLOCKS);

        // Get (or build) the plan for this region
        Region plan = RegionCache.get(level, regionX, regionZ);

        // Ceiling Y for this layout
        int ceilingY = FLOOR_Y + HEADROOM + CEILING_PAD;

        // “Roof marker” trick: if we already set a bedrock marker in this chunk, skip rework
        if (level.getBlockState(new BlockPos(minX, ceilingY + 2, minZ)).is(BEDROCK.getBlock())) {
            return;
        }

        // World min/max for safety
        int worldMinY = level.dimensionType().minY();
        int worldMaxY = worldMinY + level.dimensionType().height();

        // Reusable position object
        MutableBlockPos pos = new MutableBlockPos();

        // Some precomputed bases to align lamps nicely
        int regionOriginX = regionX * REGION_BLOCKS;
        int regionOriginZ = regionZ * REGION_BLOCKS;

        int corridorBaseX = regionOriginX + CELL_CENTER;
        int corridorBaseZ = regionOriginZ + CELL_CENTER;

        int roomBaseX = regionOriginX + ROOM_LAMP_MARGIN;
        int roomBaseZ = regionOriginZ + ROOM_LAMP_MARGIN;

        // Walk every block in the 16×16 chunk and place blocks
        for (int dz = 0; dz < 16; dz++) {
            int worldZ = minZ + dz;
            int inRegionZ = Math.floorMod(worldZ, REGION_BLOCKS);
            int cellZ     = inRegionZ / TILE_SIZE;   // which cell (Z) in the region?
            int localZ    = inRegionZ % TILE_SIZE;   // where inside the tile?

            for (int dx = 0; dx < 16; dx++) {
                int worldX = minX + dx;
                int inRegionX = Math.floorMod(worldX, REGION_BLOCKS);
                int cellX     = inRegionX / TILE_SIZE; // which cell (X) in the region?
                int localX    = inRegionX % TILE_SIZE; // where inside the tile?

                // Bedrock markers: one at the base, one above the ceiling (used as “already done” flag)
                set(level, pos, worldX, FLOOR_Y,      worldZ, BEDROCK, worldMinY, worldMaxY);
                set(level, pos, worldX, ceilingY + 2, worldZ, BEDROCK, worldMinY, worldMaxY);

                boolean inWalkX = localX < CELL_SIZE;
                boolean inWalkZ = localZ < CELL_SIZE;

                if (inWalkX && inWalkZ) {
                    // We’re standing on the walkable area inside a cell.
                    set(level, pos, worldX, FLOOR_Y + 1, worldZ, CARPET, worldMinY, worldMaxY);

                    // Are we in a room-ish place (room/mega/cozy), or a corridor?
                    boolean inRoomish = plan.isRoom(cellX, cellZ) || plan.isMega(cellX, cellZ) || plan.isCozy(cellX, cellZ);

                    boolean placeLamp = false;

                    if (!inRoomish) {
                        // Corridor lighting:
                        // Decide if the corridor runs east-west or north-south by looking at openings.
                        int ew = (plan.openWest(cellX, cellZ)  ? 1 : 0) + (plan.openEast(cellX, cellZ)  ? 1 : 0);
                        int ns = (plan.openNorth(cellX, cellZ) ? 1 : 0) + (plan.openSouth(cellX, cellZ) ? 1 : 0);

                        if (ew > ns) {
                            // East-West corridor: place a strip across Z at the center
                            if (Math.abs(localZ - CELL_CENTER) <= LAMP_STRIP_HALF) {
                                placeLamp = (Math.floorMod(worldX - corridorBaseX, LAMP_SPACING) == 0);
                            }
                        } else if (ns > ew) {
                            // North-South corridor: place a strip across X at the center
                            if (Math.abs(localX - CELL_CENTER) <= LAMP_STRIP_HALF) {
                                placeLamp = (Math.floorMod(worldZ - corridorBaseZ, LAMP_SPACING) == 0);
                            }
                        } else {
                            // Junction / corner / dead-end: allow lamp on exact center if either axis aligns
                            boolean atCenter = Math.abs(localX - CELL_CENTER) <= JUNCTION_HALF
                                    && Math.abs(localZ - CELL_CENTER) <= JUNCTION_HALF;
                            if (atCenter) {
                                boolean alignX = (Math.floorMod(worldX - corridorBaseX, LAMP_SPACING) == 0);
                                boolean alignZ = (Math.floorMod(worldZ - corridorBaseZ, LAMP_SPACING) == 0);
                                placeLamp = alignX || alignZ;
                            }
                        }
                    } else {
                        // Room lighting:
                        // Keep lamps away from walls, and place them on a simple grid.
                        boolean insideMargin =
                                localX >= ROOM_LAMP_MARGIN && localX <= (CELL_SIZE - 1 - ROOM_LAMP_MARGIN) &&
                                        localZ >= ROOM_LAMP_MARGIN && localZ <= (CELL_SIZE - 1 - ROOM_LAMP_MARGIN);

                        if (insideMargin) {
                            boolean gx = (Math.floorMod(worldX - roomBaseX, ROOM_LAMP_SPACING) == 0);
                            boolean gz = (Math.floorMod(worldZ - roomBaseZ, ROOM_LAMP_SPACING) == 0);
                            placeLamp = gx && gz;
                        }
                    }

                    // Place lamp (with hidden redstone power), or a ceiling slab + stone above it
                    if (placeLamp) {
                        set(level, pos, worldX, ceilingY,     worldZ, LAMP_ON,        worldMinY, worldMaxY);
                        set(level, pos, worldX, ceilingY + 1, worldZ, REDSTONE_BLOCK,  worldMinY, worldMaxY);
                    } else {
                        set(level, pos, worldX, ceilingY,     worldZ, CEILING_SLAB_TOP, worldMinY, worldMaxY);
                        set(level, pos, worldX, ceilingY + 1, worldZ, UNDER_CEILING,    worldMinY, worldMaxY);
                    }
                } else {
                    // We’re on the wall line between cells. This is either a doorway (open) or a wall (solid).
                    boolean doorway =
                            (!inWalkX && inWalkZ) ? plan.openEast(cellX, cellZ) :
                                    ( inWalkX && !inWalkZ) ? plan.openSouth(cellX, cellZ) :
                                            false; // exact corner posts are always solid

                    if (!doorway) {
                        // Solid wall column, with a special trim ring at y+2 for a “baseboard” look.
                        for (int y = 1; y < VISIBLE_WALL_UP; y++) {
                            BlockState mat = (y == 2) ? WALL_TRIM : WALL;
                            set(level, pos, worldX, FLOOR_Y + y, worldZ, mat, worldMinY, worldMaxY);
                        }
                    } else {
                        // Doorway gets the same treatment as walkable: carpet and ceiling.
                        set(level, pos, worldX, FLOOR_Y + 1, worldZ, CARPET,           worldMinY, worldMaxY);
                        set(level, pos, worldX, ceilingY,     worldZ, CEILING_SLAB_TOP, worldMinY, worldMaxY);
                        set(level, pos, worldX, ceilingY + 1, worldZ, UNDER_CEILING,    worldMinY, worldMaxY);
                    }
                }
            }
        }

        // Tiny carpet pad at (0,0) so new players have a nice spawn
        if (chunkPos.x == 0 && chunkPos.z == 0) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    set(level, pos, x, FLOOR_Y + 1, z, CARPET, worldMinY, worldMaxY);
                }
            }
        }
    }

    // Region for corridors/rooms in a 32×32 cell area
    private static final class Region {

        // Openings we store:
        // eOpen[x][z] tells if the wall between (x,z) and (x+1,z) is open (EAST)
        // sOpen[x][z] tells if the wall between (x,z) and (x,z+1) is open (SOUTH)
        private final boolean[][] eOpen = new boolean[REGION_CELLS - 1][REGION_CELLS];
        private final boolean[][] sOpen = new boolean[REGION_CELLS][REGION_CELLS - 1];

        // Border continuity into neighboring regions
        private final boolean[] borderEast  = new boolean[REGION_CELLS];
        private final boolean[] borderSouth = new boolean[REGION_CELLS];

        // Cell tags
        private static final byte TAG_ROOM = 1, TAG_MEGA = 2, TAG_COZY = 4;
        private final byte[][] tag = new byte[REGION_CELLS][REGION_CELLS];

        // RNG & coordinate base
        private final RandomSource rnd;
        private final long baseSeed;
        private final int cellBaseX, cellBaseZ;

        Region(ServerLevel level, int regionX, int regionZ) {
            this.baseSeed = level.getSeed() ^ 0x6D09_4BA7_1234_5678L;
            this.rnd      = RandomSource.create(mix(baseSeed, regionX, regionZ));
            this.cellBaseX = regionX * REGION_CELLS;
            this.cellBaseZ = regionZ * REGION_CELLS;

            // Build the plan
            computeBorders();
            carveMazeDFS();
            sprinkleExtraBreaks();

            carveRects(MEGA_CHANCE, MEGA_COUNT_MIN, MEGA_COUNT_MAX, MEGA_W_MIN, MEGA_W_MAX, MEGA_H_MIN, MEGA_H_MAX, TAG_MEGA, false, 0, 0);
            carveRects(ROOM_CHANCE, ROOM_COUNT_MIN, ROOM_COUNT_MAX, ROOM_W_MIN, ROOM_W_MAX, ROOM_H_MIN, ROOM_H_MAX, TAG_ROOM, false, 0, 0);
            carveRects(1.0,         COZY_COUNT_MIN, COZY_COUNT_MAX, COZY_W_MIN, COZY_W_MAX, COZY_H_MIN, COZY_H_MAX, TAG_COZY, true,  COZY_DOORS_MIN, COZY_DOORS_MAX);

            carveRewardRooms();
        }

        // easy query helpers
        boolean isRoom(int x, int z) { return inBounds(x, z) && (tag[x][z] & TAG_ROOM) != 0; }
        boolean isMega(int x, int z) { return inBounds(x, z) && (tag[x][z] & TAG_MEGA) != 0; }
        boolean isCozy(int x, int z) { return inBounds(x, z) && (tag[x][z] & TAG_COZY) != 0; }

        boolean openEast (int x, int z) { if (!inBoundsCell(z)) return false; if (x == REGION_CELLS - 1) return borderEast[z];  if (x < 0) return false; return eOpen[x][z]; }
        boolean openSouth(int x, int z) { if (!inBoundsCell(x)) return false; if (z == REGION_CELLS - 1) return borderSouth[x]; if (z < 0) return false; return sOpen[x][z]; }
        boolean openWest (int x, int z) { if (!inBoundsCell(z)) return false; if (x > 0) return eOpen[x - 1][z]; return randomEdge(true,  cellBaseX - 1, cellBaseZ + z); }
        boolean openNorth(int x, int z) { if (!inBoundsCell(x)) return false; if (z > 0) return sOpen[x][z - 1]; return randomEdge(false, cellBaseX + x, cellBaseZ - 1); }

        // match borders between neighboring regions
        private void computeBorders() {
            int eastX = cellBaseX + (REGION_CELLS - 1);
            for (int z = 0; z < REGION_CELLS; z++) {
                borderEast[z] = randomEdge(true, eastX, cellBaseZ + z);
            }
            int southZ = cellBaseZ + (REGION_CELLS - 1);
            for (int x = 0; x < REGION_CELLS; x++) {
                borderSouth[x] = randomEdge(false, cellBaseX + x, southZ);
            }
        }

        // carve a simple DFS maze
        private void carveMazeDFS() {
            boolean[][] visited = new boolean[REGION_CELLS][REGION_CELLS];
            ArrayDeque<int[]> stack = new ArrayDeque<>();

            int startX = rnd.nextInt(REGION_CELLS);
            int startZ = rnd.nextInt(REGION_CELLS);
            stack.push(new int[]{ startX, startZ });
            visited[startX][startZ] = true;

            final int[][] DIRS = { {1,0}, {-1,0}, {0,1}, {0,-1} };

            while (!stack.isEmpty()) {
                int[] cur = stack.peek();
                int cx = cur[0], cz = cur[1];

                int[] order = shuffled4(rnd);
                boolean moved = false;

                for (int i = 0; i < 4; i++) {
                    int dx = DIRS[order[i]][0];
                    int dz = DIRS[order[i]][1];
                    int nx = cx + dx, nz = cz + dz;

                    if (!inBounds(nx, nz) || visited[nx][nz]) continue;

                    // open the wall between current and next
                    if      (dx ==  1) eOpen[cx][cz] = true;
                    else if (dx == -1) eOpen[nx][nz] = true;
                    else if (dz ==  1) sOpen[cx][cz] = true;
                    else               sOpen[nx][nz] = true;

                    visited[nx][nz] = true;
                    stack.push(new int[]{ nx, nz });
                    moved = true;
                    break;
                }
                if (!moved) stack.pop();
            }

            // make sure our planned corridor matches region borders
            if (REGION_CELLS >= 2) {
                for (int z = 0; z < REGION_CELLS; z++) if (borderEast[z])  eOpen[REGION_CELLS - 2][z] = true;
                for (int x = 0; x < REGION_CELLS; x++) if (borderSouth[x]) sOpen[x][REGION_CELLS - 2]  = true;
            }
        }

        // add a few extra connections
        private void sprinkleExtraBreaks() {
            for (int z = 0; z < REGION_CELLS; z++)
                for (int x = 0; x < REGION_CELLS - 1; x++)
                    if (!eOpen[x][z] && rnd.nextDouble() < EXTRA_BREAKS_CHANCE) eOpen[x][z] = true;

            for (int z = 0; z < REGION_CELLS - 1; z++)
                for (int x = 0; x < REGION_CELLS; x++)
                    if (!sOpen[x][z] && rnd.nextDouble() < EXTRA_BREAKS_CHANCE) sOpen[x][z] = true;
        }

        // carve rectangles
        private void carveRects(
                double chance, int countMin, int countMax,
                int wMin, int wMax, int hMin, int hMax,
                byte tagFlag, boolean sealIntoRing, int doorsMin, int doorsMax
        ) {
            if (rnd.nextDouble() > chance) return;

            int count = clamp(countMin, countMax);
            for (int i = 0; i < count; i++) {
                int w = clamp(wMin, wMax);
                int h = clamp(hMin, hMax);
                int x0 = rnd.nextInt(Math.max(1, REGION_CELLS - w));
                int z0 = rnd.nextInt(Math.max(1, REGION_CELLS - h));

                // Tag all cells inside the rectangle
                for (int z = z0; z < z0 + h; z++)
                    for (int x = x0; x < x0 + w; x++)
                        tag[x][z] |= tagFlag;

                // Open internal walls horizontally
                for (int z = z0; z < z0 + h; z++)
                    for (int x = x0; x < x0 + w - 1; x++)
                        eOpen[x][z] = true;

                // Open internal walls vertically
                for (int z = z0; z < z0 + h - 1; z++)
                    for (int x = x0; x < x0 + w; x++)
                        sOpen[x][z] = true;

                if (sealIntoRing) {
                    // Close outer ring
                    for (int x = x0; x < x0 + w - 1; x++) { eOpen[x][z0] = false; eOpen[x][z0 + h - 1] = false; }
                    for (int z = z0; z < z0 + h - 1; z++) { sOpen[x0][z] = false; sOpen[x0 + w - 1][z] = false; }

                    // Punch 1–2 doors randomly on the ring
                    int doors = clamp(doorsMin, doorsMax);
                    for (int d = 0; d < doors; d++) {
                        switch (rnd.nextInt(4)) {
                            case 0 -> eOpen[x0 + rnd.nextInt(Math.max(1, w - 1))][z0]           = true;
                            case 1 -> eOpen[x0 + rnd.nextInt(Math.max(1, w - 1))][z0 + h - 1]   = true;
                            case 2 -> sOpen[x0][          z0 + rnd.nextInt(Math.max(1, h - 1))]  = true;
                            default-> sOpen[x0 + w - 1][  z0 + rnd.nextInt(Math.max(1, h - 1))]  = true;
                        }
                    }
                }
            }
        }

        // bigger sealed rooms at true dead-ends (one door back)
        private void carveRewardRooms() {
            int placed = 0;

            outer:
            for (int cz = 0; cz < REGION_CELLS; cz++) {
                for (int cx = 0; cx < REGION_CELLS; cx++) {

                    if (placed >= REWARD_MAX_PER_REGION) break outer;

                    // skip if already roomish
                    if ((tag[cx][cz] & (TAG_ROOM | TAG_MEGA | TAG_COZY)) != 0) continue;

                    // dead-end = degree 1
                    if (degree(cx, cz) != 1) continue;

                    // find the direction the corridor points
                    int dx = 0, dz = 0;
                    if      (openEast (cx, cz)) dx =  1;
                    else if (openWest (cx, cz)) dx = -1;
                    else if (openSouth(cx, cz)) dz =  1;
                    else if (openNorth(cx, cz)) dz = -1;
                    else continue;

                    int w = clamp(REWARD_W_MIN, REWARD_W_MAX);
                    int h = clamp(REWARD_H_MIN, REWARD_H_MAX);

                    // place the rectangle just beyond the dead-end cell
                    int x0, z0;
                    if (dx ==  1) { x0 = cx + 1; z0 = Mth.clamp(cz - (h / 2), 0, Math.max(0, REGION_CELLS - h)); }
                    else if (dx == -1){ x0 = cx - w; z0 = Mth.clamp(cz - (h / 2), 0, Math.max(0, REGION_CELLS - h)); }
                    else if (dz ==  1){ z0 = cz + 1; x0 = Mth.clamp(cx - (w / 2), 0, Math.max(0, REGION_CELLS - w)); }
                    else               { z0 = cz - h; x0 = Mth.clamp(cx - (w / 2), 0, Math.max(0, REGION_CELLS - w)); }

                    if (!inBounds(x0, z0) || !inBounds(x0 + w - 1, z0 + h - 1)) continue;

                    // must not overlap other tagged areas
                    boolean clear = true;
                    for (int z = z0; z < z0 + h && clear; z++)
                        for (int x = x0; x < x0 + w; x++)
                            if ((tag[x][z] & (TAG_ROOM | TAG_MEGA | TAG_COZY)) != 0) { clear = false; break; }
                    if (!clear) continue;

                    // tag/open inside
                    for (int z = z0; z < z0 + h; z++)
                        for (int x = x0; x < x0 + w; x++)
                            tag[x][z] |= TAG_ROOM;
                    for (int z = z0; z < z0 + h; z++)
                        for (int x = x0; x < x0 + w - 1; x++)
                            eOpen[x][z] = true;
                    for (int z = z0; z < z0 + h - 1; z++)
                        for (int x = x0; x < x0 + w; x++)
                            sOpen[x][z] = true;

                    // seal the outer ring
                    for (int x = x0; x < x0 + w - 1; x++) { eOpen[x][z0] = false; eOpen[x][z0 + h - 1] = false; }
                    for (int z = z0; z < z0 + h - 1; z++) { sOpen[x0][z] = false; sOpen[x0 + w - 1][z] = false; }

                    // single doorway back to the dead-end corridor
                    if      (dx ==  1) eOpen[cx][cz]     = true;
                    else if (dx == -1) eOpen[cx - 1][cz] = true;
                    else if (dz ==  1) sOpen[cx][cz]     = true;
                    else               sOpen[cx][cz - 1] = true;

                    placed++;
                }
            }
        }

        // tiny helpers for Region
        private int degree(int x, int z) {
            int d = 0;
            if (openEast (x, z)) d++;
            if (openWest (x, z)) d++;
            if (openNorth(x, z)) d++;
            if (openSouth(x, z)) d++;
            return d;
        }
        private static boolean inBounds(int x, int z) { return x >= 0 && z >= 0 && x < REGION_CELLS && z < REGION_CELLS; }
        private static boolean inBoundsCell(int v)    { return v >= 0 && v < REGION_CELLS; }
        private int clamp(int a, int b)               { return (b <= a) ? a : a + rnd.nextInt(b - a + 1); }

        private static int[] shuffled4(RandomSource rnd) {
            int[] a = {0,1,2,3};
            for (int i = 3; i > 0; i--) {
                int j = rnd.nextInt(i + 1);
                int t = a[i]; a[i] = a[j]; a[j] = t;
            }
            return a;
        }

        // “Random” but stable opening on outer edges so regions match their neighbors
        private boolean randomEdge(boolean horizontal, int ex, int ez) {
            long salt = horizontal ? 0x55AA55AA55AA55AAL : 0xAA55AA55AA55AA55L;
            long mixed = mix(baseSeed ^ salt, ex, ez);
            return (mixed & 7L) < 3L; // ~3/8 openings looks nice
        }
    }


    // Tiny LRU cache so we don’t recompute the same region plan
    private static final class RegionCache {
        private static final int MAX = 128;

        private static final Map<Long, Region> LRU = new LinkedHashMap<>(MAX, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Region> eldest) {
                return this.size() > MAX;
            }
        };

        static Region get(ServerLevel level, int regionX, int regionZ) {
            long key = (((long) regionX) << 32) ^ (regionZ & 0xFFFFFFFFL) ^ (level.getSeed() * 0x9E3779B97F4A7C15L);
            synchronized (LRU) {
                Region r = LRU.get(key);
                if (r == null) { r = new Region(level, regionX, regionZ); LRU.put(key, r); }
                return r;
            }
        }
    }


    // Low-level helper: set a block if inside world height
    private static void set(ServerLevel level, MutableBlockPos pos,
                            int x, int y, int z, BlockState state,
                            int minY, int maxY) {
        if (y < minY || y >= maxY) return;
        pos.set(x, y, z);
        if (level.getBlockState(pos).equals(state)) return; // skip same block (saves work)
        level.setBlock(pos, state, 2); // 2 = send to clients
    }

    // Stable “mixer” to derive per-region randomness from world seed
    private static long mix(long k, long x, long z) {
        k ^= x * 0x9E3779B97F4A7C15L;
        k ^= z * 0xC2B2AE3D27D4EB4FL;
        k  = (k ^ (k >>> 30)) * 0xBF58476D1CE4E5B9L;
        k  = (k ^ (k >>> 27)) * 0x94D049BB133111EBL;
        return k ^ (k >>> 31);
    }
}
