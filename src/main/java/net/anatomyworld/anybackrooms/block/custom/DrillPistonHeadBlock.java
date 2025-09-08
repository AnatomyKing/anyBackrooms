package net.anatomyworld.anybackrooms.block.custom;

import com.mojang.serialization.MapCodec;
import net.anatomyworld.anybackrooms.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class DrillPistonHeadBlock extends DirectionalBlock {

    public static final MapCodec<DrillPistonHeadBlock> CODEC = simpleCodec(DrillPistonHeadBlock::new);
    @Override public MapCodec<DrillPistonHeadBlock> codec() { return CODEC; }

    public static final EnumProperty<PistonType> TYPE = BlockStateProperties.PISTON_TYPE;
    public static final BooleanProperty SHORT = BlockStateProperties.SHORT;

    public static final int PLATFORM_THICKNESS = 4;
    private static final VoxelShape SHAPE_PLATFORM = Block.boxZ(16.0, 0.0, 4.0);
    private static final Map<Direction, VoxelShape> SHAPES_SHORT =
            Shapes.rotateAll(Shapes.or(SHAPE_PLATFORM, Block.boxZ(4.0, 4.0, 16.0)));
    private static final Map<Direction, VoxelShape> SHAPES =
            Shapes.rotateAll(Shapes.or(SHAPE_PLATFORM, Block.boxZ(4.0, 4.0, 20.0)));

    public DrillPistonHeadBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TYPE, PistonType.DEFAULT)
                .setValue(SHORT, Boolean.FALSE));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, TYPE, SHORT); }
    @Override protected boolean useShapeForLightOcclusion(BlockState s) { return true; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return (s.getValue(SHORT) ? SHAPES_SHORT : SHAPES).get(s.getValue(FACING)); }

    /* ---------- helpers ---------- */

    private static boolean fitsBase(BlockState headState, BlockState baseState) {
        if (!(baseState.getBlock() instanceof DrillPistonBaseBlock)) return false;
        return baseState.getValue(DrillPistonBaseBlock.EXTENDED)
                && baseState.getValue(FACING) == headState.getValue(FACING);
    }

    private static boolean fitsBaseOrMoving(BlockGetter level, BlockState headState, BlockPos headPos) {
        Direction f = headState.getValue(FACING);
        BlockPos behind = headPos.relative(f.getOpposite());
        BlockState base = level.getBlockState(behind);

        if (fitsBase(headState, base)) return true;

        if (base.is(Blocks.MOVING_PISTON)) {
            BlockEntity be = level.getBlockEntity(behind);
            if (be instanceof PistonMovingBlockEntity mpe) {
                return !mpe.isExtending() && mpe.getDirection() == f;
            }
        }
        return false;
    }

    /* ---------- vanilla tie-ins & drill runtime hooks ---------- */

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel sl) DrillRuntime.stop(sl, pos, true);
        if (!level.isClientSide && player.preventsBlockDrops()) {
            BlockPos basePos = pos.relative(state.getValue(FACING).getOpposite());
            if (fitsBase(state, level.getBlockState(basePos))) {
                level.destroyBlock(basePos, false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        DrillRuntime.stop(level, pos, true);
        BlockPos basePos = pos.relative(state.getValue(FACING).getOpposite());
        if (fitsBase(state, level.getBlockState(basePos))) level.destroyBlock(basePos, true);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction fromDir, BlockPos fromPos, BlockState fromState, RandomSource rand) {
        return fromDir.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, fromDir, fromPos, fromState, rand);
    }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        BlockState base = level.getBlockState(pos.relative(s.getValue(FACING).getOpposite()));
        return fitsBase(s, base) ||
                (base.is(Blocks.MOVING_PISTON) &&
                        base.getValue(MovingPistonBlock.FACING) == s.getValue(FACING));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation ori, boolean moving) {
        if (level instanceof ServerLevel sl) DrillRuntime.ensure(sl, state, pos);
        if (state.canSurvive(level, pos)) {
            level.neighborChanged(pos.relative(state.getValue(FACING).getOpposite()), block,
                    ExperimentalRedstoneUtils.withFront(ori, state.getValue(FACING).getOpposite()));
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader l, BlockPos p, BlockState s, boolean pick) {
        return new ItemStack(s.getValue(TYPE) == PistonType.STICKY
                ? ModBlocks.STICKY_DRILL_PISTON.get().asItem()
                : ModBlocks.DRILL_PISTON.get().asItem());
    }

    @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
    @Override protected boolean isPathfindable(BlockState s, PathComputationType t) { return false; }

    /* ======================= SMART DRILL RUNTIME ======================= */
    public static final class DrillRuntime {
        private static final WeakHashMap<ServerLevel, HashMap<Long, Task>> TASKS = new WeakHashMap<>();
        private static HashMap<Long, Task> map(ServerLevel l) { return TASKS.computeIfAbsent(l, k -> new HashMap<>()); }

        private static final class Task {
            final BlockPos headPos;
            final Direction dir;
            final boolean sticky;
            final int breakerId;
            final BlockState initialTargetState;
            BlockPos targetPos;
            int totalTicks;
            int elapsedTicks = 0;
            boolean pausedForRetract = false;
            BlockPos retargetPos = null;
            Task(BlockPos headPos, Direction dir, boolean sticky, int breakerId, BlockPos targetPos, BlockState initialTargetState, int totalTicks) {
                this.headPos = headPos; this.dir = dir; this.sticky = sticky; this.breakerId = breakerId;
                this.targetPos = targetPos; this.initialTargetState = initialTargetState; this.totalTicks = Math.max(1, totalTicks);
            }
        }

        private static int breakerIdFor(BlockPos headPos) {
            long v = headPos.asLong();
            return (int)((v ^ (v >>> 32)) & 0x7FFFFFFF);
        }
        private static ItemStack bestTool(BlockState s) {
            if (s.is(BlockTags.MINEABLE_WITH_AXE))     return new ItemStack(Items.NETHERITE_AXE);
            if (s.is(BlockTags.MINEABLE_WITH_SHOVEL))  return new ItemStack(Items.NETHERITE_SHOVEL);
            if (s.is(BlockTags.MINEABLE_WITH_HOE))     return new ItemStack(Items.NETHERITE_HOE);
            if (s.is(BlockTags.MINEABLE_WITH_PICKAXE)) return new ItemStack(Items.NETHERITE_PICKAXE);
            if (s.is(BlockTags.LEAVES) || s.is(BlockTags.WOOL)) return new ItemStack(Items.SHEARS);
            return new ItemStack(Items.NETHERITE_PICKAXE);
        }
        private static int breakTicks(double hardness) {
            if (hardness < 0) return Integer.MAX_VALUE;
            return Mth.ceil(30.0 * hardness / 18.0);
        }

        /** Ensure a task exists while extended & touching a valid block. */
        public static void ensure(ServerLevel level, BlockState headState, BlockPos headPos) {
            Direction f = headState.getValue(FACING);
            BlockPos basePos = headPos.relative(f.getOpposite());
            BlockState base = level.getBlockState(basePos);
            boolean isExtended = (base.getBlock() instanceof DrillPistonBaseBlock)
                    && base.getValue(DrillPistonBaseBlock.EXTENDED)
                    && base.getValue(DrillPistonBaseBlock.FACING) == f;
            if (!isExtended) { stop(level, headPos, true); return; }

            BlockPos target = headPos.relative(f);
            BlockState targetState = level.getBlockState(target);
            if (targetState.isAir() || targetState.getDestroySpeed(level, target) < 0 || targetState.getPistonPushReaction() == PushReaction.BLOCK) {
                stop(level, headPos, true); return;
            }

            long key = headPos.asLong();
            if (!map(level).containsKey(key)) {
                boolean sticky = base.getBlock() == ModBlocks.STICKY_DRILL_PISTON.get();
                int ticks = breakTicks(targetState.getDestroySpeed(level, target));
                if (ticks == Integer.MAX_VALUE) return;
                Task t = new Task(headPos, f, sticky, breakerIdFor(headPos), target, targetState, ticks);
                map(level).put(key, t);
                level.destroyBlockProgress(t.breakerId, target, 0);
                level.scheduleTick(headPos, headState.getBlock(), 1);
            }
        }

        /** Called when a sticky retract begins to pull the target into the head. */
        public static void onStickyRetractBegin(ServerLevel level, BlockPos headPos, Direction dir) {
            Task t = map(level).get(headPos.asLong());
            if (t == null || !t.sticky) return;

            t.pausedForRetract = true;
            t.retargetPos = headPos; // target will be pulled into the head
            int stage = Mth.clamp((int)((t.elapsedTicks / (double)t.totalTicks) * 10.0), 0, 9);
            level.destroyBlockProgress(t.breakerId, t.retargetPos, stage);
        }

        public static void stop(ServerLevel level, BlockPos headPos, boolean clearCracks) {
            Task t = map(level).remove(headPos.asLong());
            if (t != null && clearCracks) level.destroyBlockProgress(t.breakerId, t.targetPos, -1);
        }

        /** Tick from the head. Keeps cracks alive and safe across sticky moves. */
        public static void tick(ServerLevel level, BlockState headState, BlockPos headPos, RandomSource rnd) {
            Task t = map(level).get(headPos.asLong());
            if (t == null) return;

            // If head/base invalid, stop.
            if (!(level.getBlockState(headPos).getBlock() instanceof DrillPistonHeadBlock)) { stop(level, headPos, true); return; }
            BlockState base = level.getBlockState(headPos.relative(t.dir.getOpposite()));
            boolean validBase = (base.getBlock() instanceof DrillPistonBaseBlock)
                    && base.getValue(DrillPistonBaseBlock.EXTENDED)
                    && base.getValue(DrillPistonBaseBlock.FACING) == t.dir;
            if (!validBase) { stop(level, headPos, true); return; }

            // Handle sticky retract: wait until the pulled block lands at retarget pos
            if (t.pausedForRetract) {
                BlockState at = level.getBlockState(t.retargetPos);
                if (!at.isAir() && !at.is(Blocks.MOVING_PISTON)) {
                    if (at.is(t.initialTargetState.getBlock())) {
                        level.destroyBlockProgress(t.breakerId, t.targetPos, -1);
                        t.targetPos = t.retargetPos;
                        t.pausedForRetract = false;
                    }
                }
                int stage = Mth.clamp((int)((t.elapsedTicks / (double)t.totalTicks) * 10.0), 0, 9);
                level.destroyBlockProgress(t.breakerId, t.retargetPos, stage);
                level.scheduleTick(headPos, headState.getBlock(), 1);
                return;
            }

            BlockState targetState = level.getBlockState(t.targetPos);
            if (targetState.isAir() || targetState.getDestroySpeed(level, t.targetPos) < 0) {
                stop(level, headPos, true); return;
            }

            t.elapsedTicks++;
            int stage = Mth.clamp((int)((t.elapsedTicks / (double)t.totalTicks) * 10.0), 0, 9);
            level.destroyBlockProgress(t.breakerId, t.targetPos, stage);

            if (t.elapsedTicks >= t.totalTicks) {
                BlockEntity be = targetState.hasBlockEntity() ? level.getBlockEntity(t.targetPos) : null;
                ItemStack tool = bestTool(targetState);
                Block.dropResources(targetState, level, t.targetPos, be, null, tool);
                level.levelEvent(2001, t.targetPos, Block.getId(targetState));
                level.setBlock(t.targetPos, Blocks.AIR.defaultBlockState(), 35);
                stop(level, headPos, true);
            } else {
                level.scheduleTick(headPos, headState.getBlock(), 1);
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel sl) DrillRuntime.ensure(sl, state, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        DrillRuntime.tick(level, state, pos, random);
    }
}
