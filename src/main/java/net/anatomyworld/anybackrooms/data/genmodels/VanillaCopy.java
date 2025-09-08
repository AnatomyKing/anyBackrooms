package net.anatomyworld.anybackrooms.data.genmodels;

import com.mojang.math.Quadrant;
import net.anatomyworld.anybackrooms.AnyBackroomsCore;
import net.minecraft.client.data.models.BlockModelGenerators;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;

import javax.annotation.Nullable;
import java.util.Optional;


/**
 * Vanilla-like generators for NeoForge 1.21.x client datagen.
 * Uses renderer Variant + VariantMutator + BlockModelGenerators helpers.
 */
public final class VanillaCopy {
    private VanillaCopy() {}

    /* ------------------------- cube_all ------------------------- */
    public static void vanillaCube(BlockModelGenerators gen, Block... blocks) {
        for (Block b : blocks) gen.createTrivialCube(b);
    }

    /* --------------------- pillar + horizontal ------------------ */
    public static void pillarAuto(BlockModelGenerators gen, Block... blocks) {
        for (Block b : blocks) {
            gen.createRotatedPillarWithHorizontalVariant(
                    b,
                    TexturedModel.COLUMN_ALT,
                    TexturedModel.COLUMN_HORIZONTAL_ALT
            );
        }
    }

    /* ------------------------- orientable ----------------------- */
    /** Furnace-like: side/front/top (bottom uses side). */
    public static void orientable(BlockModelGenerators gen, Block block,
                                  ResourceLocation side, ResourceLocation front, ResourceLocation top) {
        TexturedModel.Provider provider = TexturedModel.ORIENTABLE_ONLY_TOP.updateTexture(mapping -> {
            mapping.put(TextureSlot.SIDE,  side);
            mapping.put(TextureSlot.FRONT, front);
            mapping.put(TextureSlot.TOP,   top);
        });
        gen.createHorizontallyRotatedBlock(block, provider);
    }

    /* --------------------------- slab --------------------------- */
    /** Use the full block’s textures and reference its cube_all model (don’t recreate it). */
    public static void slabAuto(BlockModelGenerators gen, Block slab, Block fullBlockForDouble) {
        TextureMapping map = new TextureMapping()
                .put(TextureSlot.SIDE,   texOf(fullBlockForDouble))
                .put(TextureSlot.TOP,    texOf(fullBlockForDouble, "_top"))
                .put(TextureSlot.BOTTOM, texOf(fullBlockForDouble, "_bottom"));

        ResourceLocation bottomModel = ModelTemplates.SLAB_BOTTOM.create(slab, map, gen.modelOutput);
        ResourceLocation topModel    = ModelTemplates.SLAB_TOP.create(slab, map, gen.modelOutput);

        ResourceLocation fullModelLocation = rl(
                BuiltInRegistries.BLOCK.getKey(fullBlockForDouble).getNamespace(),
                "block/" + BuiltInRegistries.BLOCK.getKey(fullBlockForDouble).getPath()
        );

        gen.blockStateOutput.accept(
                BlockModelGenerators.createSlab(
                        slab,
                        mv(bottomModel),
                        mv(topModel),
                        mv(fullModelLocation)
                )
        );
    }

    /* -------------------------- stairs -------------------------- */
    public static void stairsAuto(BlockModelGenerators gen, Block stairs) {
        TextureMapping map = new TextureMapping()
                .put(TextureSlot.SIDE,   texOf(stairs))
                .put(TextureSlot.TOP,    texOf(stairs, "_top"))
                .put(TextureSlot.BOTTOM, texOf(stairs, "_bottom"));

        ResourceLocation straight = ModelTemplates.STAIRS_STRAIGHT.create(stairs, map, gen.modelOutput);
        ResourceLocation inner    = ModelTemplates.STAIRS_INNER.create(  stairs, map, gen.modelOutput);
        ResourceLocation outer    = ModelTemplates.STAIRS_OUTER.create(  stairs, map, gen.modelOutput);

        gen.blockStateOutput.accept(
                BlockModelGenerators.createStairs(
                        stairs,
                        mv(inner),
                        mv(straight),
                        mv(outer)
                )
        );
    }

    /* --------------------------- wall --------------------------- */
    public static void wallAuto(BlockModelGenerators gen, Block wall) {
        TextureMapping map = new TextureMapping()
                .put(TextureSlot.SIDE,   texOf(wall))
                .put(TextureSlot.TOP,    texOf(wall, "_top"))
                .put(TextureSlot.BOTTOM, texOf(wall, "_bottom"));

        ResourceLocation post     = ModelTemplates.WALL_POST.create(wall, map, gen.modelOutput);
        ResourceLocation sideLow  = ModelTemplates.WALL_LOW_SIDE.create(wall, map, gen.modelOutput);
        ResourceLocation sideTall = ModelTemplates.WALL_TALL_SIDE.create(wall, map, gen.modelOutput);

        gen.blockStateOutput.accept(
                BlockModelGenerators.createWall(
                        wall,
                        mv(post),
                        mv(sideLow),
                        mv(sideTall)
                )
        );
    }

    /* --------------------------- fence -------------------------- */
    public static void fenceAuto(BlockModelGenerators gen, Block fence) {
        TextureMapping map = new TextureMapping().put(TextureSlot.TEXTURE, texOf(fence));

        ResourceLocation post = ModelTemplates.FENCE_POST.create(fence, map, gen.modelOutput);
        ResourceLocation side = ModelTemplates.FENCE_SIDE.create(fence, map, gen.modelOutput);

        gen.blockStateOutput.accept(
                BlockModelGenerators.createFence(
                        fence,
                        mv(post),
                        mv(side)
                )
        );
    }

    /* ------------------------ cross (plants) -------------------- */
    public static void crossAuto(BlockModelGenerators gen, Block... blocks) {
        for (Block block : blocks) {
            TextureMapping map = new TextureMapping().put(TextureSlot.CROSS, texOf(block));
            ResourceLocation model = ModelTemplates.CROSS.create(block, map, gen.modelOutput);
            gen.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, mv(model)));
        }
    }

    /* ----------------------- glass full blocks ------------------ */
    /** Basic glass: vanilla parent, translucent, AO off. Texture = block/<id>.png */
    public static void glassAuto(BlockModelGenerators gen, Block glassBlock) {
        var glassTemplate = ModelTemplates.CUBE_ALL.extend()
                .parent(mcBlock("glass"))
                .ambientOcclusion(false)
                .renderType("minecraft:translucent")
                .build();

        TextureMapping map = new TextureMapping().put(TextureSlot.ALL, texOf(glassBlock));
        ResourceLocation model = glassTemplate.create(glassBlock, map, gen.modelOutput);
        gen.blockStateOutput.accept(MultiVariantGenerator.dispatch(glassBlock, mv(model)));
    }

    /* ------------------------- glass panes ---------------------- */
    public static void paneAuto(BlockModelGenerators gen, Block pane) {
        ResourceLocation paneTexture = texOf(pane);
        ResourceLocation edgeTexture = texOf(pane, "_top");

        TextureSlot PANE = TextureSlot.create("pane", TextureSlot.ALL);
        TextureSlot EDGE = TextureSlot.create("edge", TextureSlot.ALL);

        var post     = ModelTemplates.CUBE.extend()
                .parent(mcBlock("template_glass_pane_post"))
                .suffix("_post")
                .requiredTextureSlot(PANE)
                .requiredTextureSlot(EDGE)
                .renderType("minecraft:translucent")
                .build();

        var side     = ModelTemplates.CUBE.extend()
                .parent(mcBlock("template_glass_pane_side"))
                .suffix("_side")
                .requiredTextureSlot(PANE)
                .requiredTextureSlot(EDGE)
                .renderType("minecraft:translucent")
                .build();

        var sideAlt  = ModelTemplates.CUBE.extend()
                .parent(mcBlock("template_glass_pane_side_alt"))
                .suffix("_side_alt")
                .requiredTextureSlot(PANE)
                .requiredTextureSlot(EDGE)
                .renderType("minecraft:translucent")
                .build();

        var noSide   = ModelTemplates.CUBE.extend()
                .parent(mcBlock("template_glass_pane_noside"))
                .suffix("_noside")
                .requiredTextureSlot(PANE)
                .requiredTextureSlot(EDGE)
                .renderType("minecraft:translucent")
                .build();

        var noSideAlt= ModelTemplates.CUBE.extend()
                .parent(mcBlock("template_glass_pane_noside_alt"))
                .suffix("_noside_alt")
                .requiredTextureSlot(PANE)
                .requiredTextureSlot(EDGE)
                .renderType("minecraft:translucent")
                .build();

        TextureMapping tex = new TextureMapping()
                .put(PANE, paneTexture)
                .put(EDGE, edgeTexture);

        ResourceLocation postModel      = post.create(pane, tex, gen.modelOutput);
        ResourceLocation sideModel      = side.create(pane, tex, gen.modelOutput);
        ResourceLocation sideAltModel   = sideAlt.create(pane, tex, gen.modelOutput);
        ResourceLocation noSideModel    = noSide.create(pane, tex, gen.modelOutput);
        ResourceLocation noSideAltModel = noSideAlt.create(pane, tex, gen.modelOutput);

        var mp = MultiPartGenerator.multiPart(pane)
                .with(BlockModelGenerators.variant(plain(postModel)))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), BlockModelGenerators.variant(plain(sideModel)))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.EAST,  true), BlockModelGenerators.variant(plain(sideModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R90))))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), BlockModelGenerators.variant(plain(sideAltModel)))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.WEST,  true), BlockModelGenerators.variant(plain(sideAltModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R90))))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false), BlockModelGenerators.variant(plain(noSideModel)))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.EAST,  false), BlockModelGenerators.variant(plain(noSideAltModel)))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, false), BlockModelGenerators.variant(plain(noSideAltModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R90))))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.WEST,  false), BlockModelGenerators.variant(plain(noSideModel).with(VariantMutator.Y_ROT.withValue(Quadrant.R270))));
        gen.blockStateOutput.accept(mp);
    }

    /* ------------------------- portal axis ---------------------- */
    public static void portalVanillaCopy(BlockModelGenerators gen, Block portalLike) {
        ResourceLocation xModel = mcBlock("nether_portal_ns");
        ResourceLocation zModel = mcBlock("nether_portal_ew");

        gen.blockStateOutput.accept(
                MultiVariantGenerator
                        .dispatch(portalLike, mv(xModel))
                        .with(
                                PropertyDispatch.modify(BlockStateProperties.AXIS)
                                        .select(Direction.Axis.X, VariantMutator.MODEL.withValue(xModel))
                                        .select(Direction.Axis.Z, VariantMutator.MODEL.withValue(zModel))
                        )
        );
    }

    /* --------------------------- lit/unlit ---------------------- */
    /** Expects textures: block/<id>.png and block/<id>_on.png */
    public static void lampAuto(BlockModelGenerators gen, Block lamp) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(lamp);

        ResourceLocation unlitTex = rl(id.getNamespace(), "block/" + id.getPath());
        ResourceLocation litTex   = rl(id.getNamespace(), "block/" + id.getPath() + "_on");

        ResourceLocation unlitModel = ModelTemplates.CUBE_ALL.create(
                lamp, new TextureMapping().put(TextureSlot.ALL, unlitTex), gen.modelOutput);

        // IMPORTANT: write lit model under models/block/
        ResourceLocation litModel = ModelTemplates.CUBE_ALL.create(
                rl(id.getNamespace(), "block/" + id.getPath() + "_on"),
                new TextureMapping().put(TextureSlot.ALL, litTex), gen.modelOutput);

        var mp = MultiPartGenerator.multiPart(lamp)
                .with(BlockModelGenerators.condition().term(BlockStateProperties.LIT, false), BlockModelGenerators.variant(plain(unlitModel)))
                .with(BlockModelGenerators.condition().term(BlockStateProperties.LIT, true),  BlockModelGenerators.variant(plain(litModel)));

        gen.blockStateOutput.accept(mp);
    }

    public static void drillPistonBaseStatesOnly(BlockModelGenerators gen, Block base, @Nullable String extendedModelOverride) {
        ResourceLocation baseId = BuiltInRegistries.BLOCK.getKey(base);
        String ns   = baseId.getNamespace();
        String name = baseId.getPath();

        ResourceLocation retractedModel = rl(ns, "block/" + name);
        ResourceLocation extendedModel  = rl(ns, "block/" + (extendedModelOverride != null ? extendedModelOverride : (name + "_base")));

        var baseGen = MultiVariantGenerator
                .dispatch(base, mv(retractedModel))
                .with(PropertyDispatch.modify(BlockStateProperties.FACING)
                        .select(Direction.NORTH, VariantMutator.X_ROT.withValue(Quadrant.R0))
                        .select(Direction.SOUTH, VariantMutator.Y_ROT.withValue(Quadrant.R180))
                        .select(Direction.EAST,  VariantMutator.Y_ROT.withValue(Quadrant.R90))
                        .select(Direction.WEST,  VariantMutator.Y_ROT.withValue(Quadrant.R270))
                        .select(Direction.UP,    VariantMutator.X_ROT.withValue(Quadrant.R270))
                        .select(Direction.DOWN,  VariantMutator.X_ROT.withValue(Quadrant.R90)))
                .with(PropertyDispatch.modify(BlockStateProperties.EXTENDED)
                        .select(false, VariantMutator.MODEL.withValue(retractedModel))
                        .select(true,  VariantMutator.MODEL.withValue(extendedModel)));

        gen.blockStateOutput.accept(baseGen);
    }

    /* ======================= HEAD (states only) ======================= */
    /** Assumes you provide these models:
     *  block/drill_piston_head.json
     *  block/drill_piston_head_short.json
     *  block/drill_piston_head_sticky.json
     *  block/drill_piston_head_sticky_short.json
     */
    public static void drillPistonHeadStatesOnly(BlockModelGenerators gen, Block head) {
        ResourceLocation headId = BuiltInRegistries.BLOCK.getKey(head);
        String ns   = headId.getNamespace();
        String name = headId.getPath();

        ResourceLocation LONG_DEFAULT   = rl(ns, "block/" + name);
        ResourceLocation SHORT_DEFAULT  = rl(ns, "block/" + name + "_short");
        ResourceLocation LONG_STICKY    = rl(ns, "block/" + name + "_sticky");
        ResourceLocation SHORT_STICKY   = rl(ns, "block/" + name + "_sticky_short");

        var headGen = MultiVariantGenerator
                .dispatch(head, mv(LONG_DEFAULT))
                .with(PropertyDispatch.modify(BlockStateProperties.FACING)
                        .select(Direction.NORTH, VariantMutator.X_ROT.withValue(Quadrant.R0))
                        .select(Direction.SOUTH, VariantMutator.Y_ROT.withValue(Quadrant.R180))
                        .select(Direction.EAST,  VariantMutator.Y_ROT.withValue(Quadrant.R90))
                        .select(Direction.WEST,  VariantMutator.Y_ROT.withValue(Quadrant.R270))
                        .select(Direction.UP,    VariantMutator.X_ROT.withValue(Quadrant.R270))
                        .select(Direction.DOWN,  VariantMutator.X_ROT.withValue(Quadrant.R90)))
                // Cross-dispatch SHORT x TYPE so each combo picks the right model.
                .with(PropertyDispatch.modify(BlockStateProperties.SHORT, BlockStateProperties.PISTON_TYPE)
                        .select(false, PistonType.DEFAULT, VariantMutator.MODEL.withValue(LONG_DEFAULT))
                        .select(true,  PistonType.DEFAULT, VariantMutator.MODEL.withValue(SHORT_DEFAULT))
                        .select(false, PistonType.STICKY,  VariantMutator.MODEL.withValue(LONG_STICKY))
                        .select(true,  PistonType.STICKY,  VariantMutator.MODEL.withValue(SHORT_STICKY))
                );

        gen.blockStateOutput.accept(headGen);
    }


    /* --------------------------- utils -------------------------- */
    public static ResourceLocation texOf(Block b) { return texOf(b, ""); }
    public static ResourceLocation texOf(Block b, String suffix) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
        return rl(id.getNamespace(), "block/" + id.getPath() + suffix);
    }

    /** Just return the cube_all model id for a full block, without creating it again. */
    private static ResourceLocation cubeAllModelLocation(Block fullBlock) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(fullBlock);
        return rl(id.getNamespace(), "block/" + id.getPath());
    }

    private static MultiVariant mv(ResourceLocation model) {
        return BlockModelGenerators.variant(plain(model));
    }

    private static Variant plain(ResourceLocation model) {
        return new Variant(model);
    }

    private static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    public static ResourceLocation mcBlock(String path) {
        return rl("minecraft", "block/" + path);
    }

    public static ResourceLocation modBlock(String path) {
        return rl(AnyBackroomsCore.MOD_ID, "block/" + path);
    }
}
