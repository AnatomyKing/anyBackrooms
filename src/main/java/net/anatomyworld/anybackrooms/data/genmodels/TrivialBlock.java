package net.anatomyworld.anybackrooms.data.genmodels;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * “Trivial” model helpers: generate models and a single-model (no properties)
 * blockstate via createTrivialBlock. Use when you want a quick model hookup
 * but don’t need full vanilla property logic.
 *
 * Covers:
 * - cube_all
 * - column (vertical/horizontal)
 * - barrel-like (side/top/bottom)
 * - orientable (front/top/side)
 * - cross (plants)
 */
public final class TrivialBlock {
    private TrivialBlock() {}

    /* ------------------------- cube_all ------------------------- */
    public static void cubeAll(BlockModelGenerators gen, Block... blocks) {
        for (Block b : blocks) gen.createTrivialCube(b);
    }

    /* --------------------------- column ------------------------- */
    public static void columnAuto(BlockModelGenerators gen, Block... blocks) {
        TexturedModel.Provider vertical = TexturedModel.createDefault(
                b -> new TextureMapping()
                        .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(b))
                        .put(TextureSlot.END,  TextureMapping.getBlockTexture(b, "_top")),
                ModelTemplates.CUBE_COLUMN
        );
        TexturedModel.Provider horiz = TexturedModel.createDefault(
                b -> new TextureMapping()
                        .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(b))
                        .put(TextureSlot.END,  TextureMapping.getBlockTexture(b, "_top")),
                ModelTemplates.CUBE_COLUMN_HORIZONTAL
        );
        for (Block b : blocks) {
            // Trivial = choose one; vertical here:
            gen.createTrivialBlock(b, vertical);
        }
    }

    public static void columnOverride(BlockModelGenerators gen, Block block,
                                      ResourceLocation side, ResourceLocation end) {
        TexturedModel.Provider provider = TexturedModel.createDefault(
                b -> new TextureMapping().put(TextureSlot.SIDE, side).put(TextureSlot.END, end),
                ModelTemplates.CUBE_COLUMN
        );
        gen.createTrivialBlock(block, provider);
    }

    /* ------------------------ barrel-like ----------------------- */
    public static void barrelLikeAuto(BlockModelGenerators gen, Block... blocks) {
        TexturedModel.Provider provider = TexturedModel.createDefault(
                b -> new TextureMapping()
                        .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(b))
                        .put(TextureSlot.TOP,  TextureMapping.getBlockTexture(b, "_top"))
                        .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(b, "_bottom")),
                ModelTemplates.CUBE_BOTTOM_TOP
        );
        for (Block b : blocks) gen.createTrivialBlock(b, provider);
    }

    public static void barrelLikeOverride(BlockModelGenerators gen, Block block,
                                          ResourceLocation side, ResourceLocation top, ResourceLocation bottom) {
        TexturedModel.Provider provider = TexturedModel.createDefault(
                b -> new TextureMapping()
                        .put(TextureSlot.SIDE, side)
                        .put(TextureSlot.TOP,  top)
                        .put(TextureSlot.BOTTOM, bottom),
                ModelTemplates.CUBE_BOTTOM_TOP
        );
        gen.createTrivialBlock(block, provider);
    }

    /* -------------------------- orientable ---------------------- */
    public static void orientable(BlockModelGenerators gen, Block block,
                                  ResourceLocation side, ResourceLocation front, ResourceLocation top) {
        TexturedModel.Provider provider = TexturedModel.createDefault(
                b -> new TextureMapping()
                        .put(TextureSlot.SIDE, side)
                        .put(TextureSlot.FRONT, front)
                        .put(TextureSlot.TOP,  top),
                ModelTemplates.CUBE_ORIENTABLE
        );
        gen.createTrivialBlock(block, provider);
    }

    /* ---------------------------- cross ------------------------- */
    public static void cross(BlockModelGenerators gen, Block... blocks) {
        for (Block b : blocks) {
            TexturedModel.Provider provider = TexturedModel.createDefault(
                    x -> new TextureMapping().put(TextureSlot.CROSS, texOf(x)),
                    ModelTemplates.CROSS
            );
            gen.createTrivialBlock(b, provider);
        }
    }

    /* ---------------------------- utils ------------------------- */
    public static ResourceLocation texOf(Block b) { return texOf(b, ""); }
    public static ResourceLocation texOf(Block b, String suffix) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath() + suffix);
    }
}
