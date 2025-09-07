// DrillPistonBaseBlock.java
package net.anatomyworld.anybackrooms.block.custom;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.anatomyworld.anybackrooms.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class DrillPistonBaseBlock extends DirectionalBlock {

    public static final MapCodec<DrillPistonBaseBlock> CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(
                    Codec.BOOL.fieldOf("sticky").forGetter(b -> b.isSticky),
                    propertiesCodec()
            ).apply(inst, DrillPistonBaseBlock::new)
    );
    @Override public MapCodec<DrillPistonBaseBlock> codec() { return CODEC; }

    public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Block.boxZ(16.0, 4.0, 16.0));

    private final boolean isSticky;

    public DrillPistonBaseBlock(boolean sticky, BlockBehaviour.Properties properties) {
        super(properties);
        this.isSticky = sticky;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(EXTENDED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, EXTENDED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(EXTENDED) ? SHAPES.get(state.getValue(FACING)) : Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getNearestLookingDirection().getOpposite())
                .setValue(EXTENDED, Boolean.FALSE);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) checkIfExtend(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation ori, boolean moving) {
        if (!level.isClientSide) checkIfExtend(level, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) == null) {
            checkIfExtend(level, pos, state);
        }
    }

    @Override protected boolean useShapeForLightOcclusion(BlockState state) { return state.getValue(EXTENDED); }
    @Override protected boolean isPathfindable(BlockState state, PathComputationType type) { return false; }
    @Override protected BlockState rotate(BlockState state, Rotation rot) { return state.setValue(FACING, rot.rotate(state.getValue(FACING))); }
    @Override public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation dir) { return state.getValue(EXTENDED) ? state : super.rotate(state, world, pos, dir); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

    /* -------------------- piston logic -------------------- */

    private void checkIfExtend(Level level, BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FACING);
        boolean powered = getNeighborSignal(level, pos, dir);

        if (powered && !state.getValue(EXTENDED)) {
            if ((new PistonStructureResolver(level, pos, dir, true)).resolve()) {
                level.blockEvent(pos, this, 0, dir.get3DDataValue());
            }
        } else if (!powered && state.getValue(EXTENDED)) {
            BlockPos headPos = pos.relative(dir, 2);
            BlockState at = level.getBlockState(headPos);
            int trigger = 1;

            if (at.is(Blocks.MOVING_PISTON) && at.getValue(MovingPistonBlock.FACING) == dir) {
                BlockEntity be = level.getBlockEntity(headPos);
                if (be instanceof PistonMovingBlockEntity moving) {
                    if (moving.isExtending() &&
                            (moving.getProgress(0.0F) < 0.5F || level.getGameTime() == moving.getLastTicked() ||
                                    (level instanceof ServerLevel))) {
                        trigger = 2;
                    }
                }
            }
            level.blockEvent(pos, this, trigger, dir.get3DDataValue());
        }
    }

    private boolean getNeighborSignal(SignalGetter signals, BlockPos pos, Direction front) {
        for (Direction d : Direction.values()) {
            if (d != front && signals.hasSignal(pos.relative(d), d)) return true;
        }
        if (signals.hasSignal(pos, Direction.DOWN)) return true;

        BlockPos above = pos.above();
        for (Direction d : Direction.values()) {
            if (d != Direction.DOWN && signals.hasSignal(above.relative(d), d)) return true;
        }
        return false;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        Direction dir = state.getValue(FACING);
        BlockState extended = state.setValue(EXTENDED, true);

        if (!level.isClientSide) {
            boolean powered = getNeighborSignal(level, pos, dir);
            if (powered && (id == 1 || id == 2)) {
                level.setBlock(pos, extended, 2);
                return false;
            }
            if (!powered && id == 0) return false;
        }

        if (id == 0) { // extend
            if (EventHooks.onPistonMovePre(level, pos, dir, true)) return false;
            if (!moveBlocks(level, pos, dir, true)) return false;

            level.setBlock(pos, extended, 67);
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.25F + 0.6F);
            level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, Context.of(extended));
        } else if (id == 1 || id == 2) { // retract
            if (EventHooks.onPistonMovePre(level, pos, dir, false)) return false;

            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof PistonMovingBlockEntity moving) moving.finalTick();

            BlockState movingBase = Blocks.MOVING_PISTON.defaultBlockState()
                    .setValue(MovingPistonBlock.FACING, dir);
            level.setBlock(pos, movingBase, 276);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(
                    pos, movingBase,
                    this.defaultBlockState().setValue(FACING, Direction.from3DDataValue(param & 7)),
                    dir, false, true
            ));
            level.updateNeighborsAt(pos, movingBase.getBlock());
            movingBase.updateNeighbourShapes(level, pos, 2);

            if (this.isSticky) {
                BlockPos ahead2 = pos.offset(dir.getStepX() * 2, dir.getStepY() * 2, dir.getStepZ() * 2);
                BlockState bs = level.getBlockState(ahead2);
                boolean cancelled = false;

                if (bs.is(Blocks.MOVING_PISTON)) {
                    BlockEntity be2 = level.getBlockEntity(ahead2);
                    if (be2 instanceof PistonMovingBlockEntity m2) {
                        if (m2.getDirection() == dir && m2.isExtending()) {
                            m2.finalTick();
                            cancelled = true;
                        }
                    }
                }

                if (!cancelled) {
                    if (id == 1 && !bs.isAir() &&
                            isPushable(bs, level, ahead2, dir.getOpposite(), false, dir) &&
                            (bs.getPistonPushReaction() == PushReaction.NORMAL || bs.is(Blocks.PISTON) || bs.is(Blocks.STICKY_PISTON))) {
                        this.moveBlocks(level, pos, dir, false);
                    } else {
                        level.removeBlock(pos.relative(dir), false);
                    }
                }
            } else {
                level.removeBlock(pos.relative(dir), false);
            }

            level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.15F + 0.6F);
            level.gameEvent(GameEvent.BLOCK_DEACTIVATE, pos, Context.of(movingBase));
        }

        EventHooks.onPistonMovePost(level, pos, dir, id == 0);
        return true;
    }

    public static boolean isPushable(BlockState state, Level level, BlockPos pos, Direction moveDir, boolean allowDestroy, Direction pistonFacing) {
        if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY() || !level.getWorldBorder().isWithinBounds(pos)) return false;
        if (state.isAir()) return true;
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.RESPAWN_ANCHOR) || state.is(Blocks.REINFORCED_DEEPSLATE))
            return false;

        if (moveDir == Direction.DOWN && pos.getY() == level.getMinY()) return false;
        if (moveDir == Direction.UP && pos.getY() == level.getMaxY()) return false;

        if (!state.is(Blocks.PISTON) && !state.is(Blocks.STICKY_PISTON)) {
            if (state.getDestroySpeed(level, pos) == -1.0F) return false;
            switch (state.getPistonPushReaction()) {
                case BLOCK -> { return false; }
                case DESTROY -> { return allowDestroy; }
                case PUSH_ONLY -> { return moveDir == pistonFacing; }
            }
        } else if (state.getValue(EXTENDED)) {
            return false;
        }
        return !state.hasBlockEntity();
    }

    private boolean moveBlocks(Level level, BlockPos pos, Direction dir, boolean extending) {
        BlockPos headPos = pos.relative(dir);
        if (!extending && level.getBlockState(headPos).is(Blocks.PISTON_HEAD)) {
            level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 276);
        }

        PistonStructureResolver resolver = new PistonStructureResolver(level, pos, dir, extending);
        if (!resolver.resolve()) return false;

        Map<BlockPos, BlockState> snapshot = Maps.newHashMap();
        var toPush = resolver.getToPush();
        var pushedStates = Lists.<BlockState>newArrayList();

        for (BlockPos bp : toPush) {
            BlockState s = level.getBlockState(bp);
            pushedStates.add(s);
            snapshot.put(bp, s);
        }

        var toDestroy = resolver.getToDestroy();
        BlockState[] destroyed = new BlockState[toPush.size() + toDestroy.size()];
        Direction move = extending ? dir : dir.getOpposite();
        int i = 0;

        for (int k = toDestroy.size() - 1; k >= 0; --k) {
            BlockPos dp = toDestroy.get(k);
            BlockState ds = level.getBlockState(dp);
            BlockEntity be = ds.hasBlockEntity() ? level.getBlockEntity(dp) : null;
            dropResources(ds, level, dp, be);
            if (!ds.is(BlockTags.FIRE) && level.isClientSide()) level.levelEvent(2001, dp, getId(ds));
            ds.onDestroyedByPushReaction(level, dp, move, level.getFluidState(dp));
            destroyed[i++] = ds;
        }

        for (int k = toPush.size() - 1; k >= 0; --k) {
            BlockPos bp = toPush.get(k);
            BlockState bs = level.getBlockState(bp);
            BlockPos dest = bp.relative(move);
            snapshot.remove(dest);

            BlockState moving = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, dir);
            level.setBlock(dest, moving, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(dest, moving, pushedStates.get(k), dir, extending, false));
            destroyed[i++] = bs;
        }

        if (extending) {
            BlockState head = ModBlocks.DRILL_PISTON_HEAD.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.piston.PistonHeadBlock.FACING, dir)
                    .setValue(net.minecraft.world.level.block.piston.PistonHeadBlock.TYPE,
                            this.isSticky ? net.minecraft.world.level.block.state.properties.PistonType.STICKY
                                    : net.minecraft.world.level.block.state.properties.PistonType.DEFAULT);

            BlockState moving = Blocks.MOVING_PISTON.defaultBlockState()
                    .setValue(MovingPistonBlock.FACING, dir)
                    .setValue(MovingPistonBlock.TYPE, this.isSticky
                            ? net.minecraft.world.level.block.state.properties.PistonType.STICKY
                            : net.minecraft.world.level.block.state.properties.PistonType.DEFAULT);

            snapshot.remove(headPos);
            level.setBlock(headPos, moving, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(headPos, moving, head, dir, true, true));
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos p : snapshot.keySet()) level.setBlock(p, air, 82);

        for (Map.Entry<BlockPos, BlockState> e : snapshot.entrySet()) {
            BlockPos bp = e.getKey();
            BlockState bs = e.getValue();
            bs.updateIndirectNeighbourShapes(level, bp, 2);
            air.updateNeighbourShapes(level, bp, 2);
            air.updateIndirectNeighbourShapes(level, bp, 2);
        }

        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, resolver.getPushDirection(), null);
        i = 0;

        for (int i1 = toDestroy.size() - 1; i1 >= 0; --i1) {
            BlockState bs = destroyed[i++];
            BlockPos bp = toDestroy.get(i1);
            if (level instanceof ServerLevel sl) bs.affectNeighborsAfterRemoval(sl, bp, false);
            bs.updateIndirectNeighbourShapes(level, bp, 2);
            level.updateNeighborsAt(bp, bs.getBlock(), orientation);
        }

        for (int i1 = toPush.size() - 1; i1 >= 0; --i1) {
            level.updateNeighborsAt(toPush.get(i1), destroyed[i++].getBlock(), orientation);
        }

        if (extending) level.updateNeighborsAt(headPos, ModBlocks.DRILL_PISTON_HEAD.get(), orientation);
        return true;
    }
}
