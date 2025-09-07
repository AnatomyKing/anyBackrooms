// DrillPistonHeadBlock.java
package net.anatomyworld.anybackrooms.block.custom;

import com.mojang.serialization.MapCodec;
import net.anatomyworld.anybackrooms.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.tags.BlockTags;

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
    @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
    @Override protected boolean isPathfindable(BlockState s, PathComputationType t) { return false; }

    private static boolean isOurBase(BlockState base) {
        return base.is(ModBlocks.DRILL_PISTON.get()) || base.is(ModBlocks.STICKY_DRILL_PISTON.get());
    }

    private boolean isFittingBase(BlockState headState, BlockState baseState) {
        return isOurBase(baseState)
                && baseState.getValue(DrillPistonBaseBlock.EXTENDED)
                && baseState.getValue(FACING) == headState.getValue(FACING);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.preventsBlockDrops()) {
            BlockPos basePos = pos.relative(state.getValue(FACING).getOpposite());
            if (isFittingBase(state, level.getBlockState(basePos))) level.destroyBlock(basePos, false);
        }
        if (level instanceof ServerLevel sl) stopDrill(sl, pos, true);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean isMoving) {
        stopDrill(level, pos, true);
        BlockPos basePos = pos.relative(state.getValue(FACING).getOpposite());
        if (isFittingBase(state, level.getBlockState(basePos))) level.destroyBlock(basePos, true);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction fromDir, BlockPos fromPos, BlockState fromState, RandomSource rand) {
        return fromDir.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, fromDir, fromPos, fromState, rand);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState base = level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
        return isFittingBase(state, base) ||
                (base.is(Blocks.MOVING_PISTON) &&
                        base.getValue(net.minecraft.world.level.block.piston.MovingPistonBlock.FACING) == state.getValue(FACING));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation ori, boolean moving) {
        if (level instanceof ServerLevel sl) ensureDrillRunning(sl, state, pos);
        if (state.canSurvive(level, pos)) {
            level.neighborChanged(pos.relative(state.getValue(FACING).getOpposite()), block,
                    ExperimentalRedstoneUtils.withFront(ori, state.getValue(FACING).getOpposite()));
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean pickBlock) {
        // picking the head gives the (non-sticky) base item (like vanilla)
        return new ItemStack(ModBlocks.DRILL_PISTON.get().asItem());
    }

    /* --------------------- Drill logic --------------------- */

    private static final WeakHashMap<ServerLevel, HashMap<Long, DrillTask>> DRILLS = new WeakHashMap<>();
    private static HashMap<Long, DrillTask> map(ServerLevel lvl) { return DRILLS.computeIfAbsent(lvl, k -> new HashMap<>()); }

    private static final class DrillTask {
        final BlockPos headPos;
        final Direction dir;
        final BlockPos targetPos;
        final int breakerId;
        final int totalTicks;
        int elapsedTicks = 0;
        final boolean stickyHead;
        DrillTask(BlockPos headPos, Direction dir, BlockPos targetPos, int breakerId, int totalTicks, boolean stickyHead) {
            this.headPos = headPos; this.dir = dir; this.targetPos = targetPos;
            this.breakerId = breakerId; this.totalTicks = Math.max(1, totalTicks);
            this.stickyHead = stickyHead;
        }
    }

    private static int breakerIdFor(BlockPos headPos) {
        long v = headPos.asLong();
        return (int)((v ^ (v >>> 32)) & 0x7FFFFFFF);
    }

    private static ItemStack pickToolFor(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_AXE))     return new ItemStack(Items.NETHERITE_AXE);
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL))  return new ItemStack(Items.NETHERITE_SHOVEL);
        if (state.is(BlockTags.MINEABLE_WITH_HOE))     return new ItemStack(Items.NETHERITE_HOE);
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return new ItemStack(Items.NETHERITE_PICKAXE);
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL)) return new ItemStack(Items.SHEARS);
        return new ItemStack(Items.NETHERITE_PICKAXE);
    }

    // netherite 9 × 2 = 18; ticks ~= ceil(30 * hardness / speed)
    private static int computeTicks(double hardness) {
        if (hardness < 0) return Integer.MAX_VALUE;
        return Mth.ceil(30.0 * hardness / 18.0);
    }

    private static void ensureDrillRunning(ServerLevel level, BlockState headState, BlockPos headPos) {
        Direction f = headState.getValue(FACING);
        BlockPos basePos = headPos.relative(f.getOpposite());
        BlockState base = level.getBlockState(basePos);
        if (!(isOurBase(base) && base.getValue(DrillPistonBaseBlock.EXTENDED) && base.getValue(FACING) == f)) {
            stopDrill(level, headPos, true);
            return;
        }

        BlockPos target = headPos.relative(f);
        BlockState targetState = level.getBlockState(target);
        if (targetState.isAir() || targetState.getDestroySpeed(level, target) < 0 || targetState.getPistonPushReaction() == PushReaction.BLOCK) {
            stopDrill(level, headPos, true);
            return;
        }

        long key = headPos.asLong();
        if (!map(level).containsKey(key)) {
            int ticks = computeTicks(targetState.getDestroySpeed(level, target));
            if (ticks == Integer.MAX_VALUE) return;
            boolean stickyHead = headState.getValue(TYPE) == PistonType.STICKY;
            DrillTask t = new DrillTask(headPos, f, target, breakerIdFor(headPos), ticks, stickyHead);
            map(level).put(key, t);
            level.destroyBlockProgress(t.breakerId, target, 0);
            level.scheduleTick(headPos, headState.getBlock(), 1);
        }
    }

    private static void stopDrill(ServerLevel level, BlockPos headPos, boolean clear) {
        DrillTask t = map(level).remove(headPos.asLong());
        if (t != null && clear) level.destroyBlockProgress(t.breakerId, t.targetPos, -1);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level instanceof ServerLevel sl) ensureDrillRunning(sl, state, pos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        DrillTask t = map(level).get(pos.asLong());
        if (t == null) return;

        // still valid?
        if (!level.getBlockState(pos).is(this)) { stopDrill(level, pos, true); return; }
        BlockState base = level.getBlockState(pos.relative(t.dir.getOpposite()));
        if (!(isOurBase(base) && base.getValue(DrillPistonBaseBlock.EXTENDED) && base.getValue(FACING) == t.dir)) {
            stopDrill(level, pos, true); return;
        }

        BlockState targetState = level.getBlockState(t.targetPos);
        if (targetState.isAir() || targetState.getDestroySpeed(level, t.targetPos) < 0) {
            stopDrill(level, pos, true); return;
        }

        // progress & crack anim
        t.elapsedTicks++;
        int stage = Mth.clamp((int)((t.elapsedTicks / (double)t.totalTicks) * 10.0), 0, 9);
        level.destroyBlockProgress(t.breakerId, t.targetPos, stage);

        if (t.elapsedTicks >= t.totalTicks) {
            if (t.stickyHead) {
                // Sticky drill: do NOT destroy. Clear cracks and stop.
                stopDrill(level, pos, true);
            } else {
                // Non-sticky: destroy with drops using a suitable netherite tool
                BlockEntity be = targetState.hasBlockEntity() ? level.getBlockEntity(t.targetPos) : null;
                ItemStack tool = pickToolFor(targetState);
                Block.dropResources(targetState, level, t.targetPos, be, null, tool);
                level.levelEvent(2001, t.targetPos, Block.getId(targetState));
                level.setBlock(t.targetPos, Blocks.AIR.defaultBlockState(), 35);
                stopDrill(level, pos, true);
            }
        } else {
            level.scheduleTick(pos, state.getBlock(), 1);
        }
    }
}
