package net.anatomyworld.anybackrooms.client.render;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client-only baked model wrapper to force render layers.
 * Left empty by default — add blocks to CUTOUT/TRANSLUCENT if you add any later.
 */
public final class AnybackroomsRenderLayers {

    private AnybackroomsRenderLayers() {}

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        if (models.isEmpty()) return;

        // no special layers yet (lobby_wool is fully opaque). Example of how to wrap:
        // if (CUTOUT_BLOCKS.contains(state.getBlock())) e.setValue(new ForceLayerStateModel(original, ChunkSectionLayer.CUTOUT));
    }

    /** Wrap a BlockStateModel and swap its parts with layer-forcing proxies. */
    private static final class ForceLayerStateModel extends DelegateBlockStateModel {
        private final ChunkSectionLayer layer;

        ForceLayerStateModel(BlockStateModel delegate, ChunkSectionLayer layer) {
            super(delegate);
            this.layer = layer;
        }

        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                 RandomSource random, List<BlockModelPart> out) {
            List<BlockModelPart> original = new ArrayList<>();
            this.delegate.collectParts(level, pos, state, random, original);
            TextureAtlasSprite particle = this.particleIcon(level, pos, state);
            for (BlockModelPart part : original) {
                out.add(new ForceLayerPart(part, particle, layer));
            }
        }
    }

    private record ForceLayerPart(BlockModelPart base,
                                  TextureAtlasSprite particle,
                                  ChunkSectionLayer forcedLayer) implements BlockModelPart {
        @Override public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(Direction face) { return base.getQuads(face); }
        @Override public boolean useAmbientOcclusion() { return base.useAmbientOcclusion(); }
        @Override public TextureAtlasSprite particleIcon() { return (particle != null) ? particle : base.particleIcon(); }
        @Override public TriState ambientOcclusion() { return TriState.DEFAULT; }
        @Override public ChunkSectionLayer getRenderType(BlockState state) { return forcedLayer; }
    }
}
