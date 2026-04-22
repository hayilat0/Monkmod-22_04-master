package net.gabriel.monk.client.render;

import net.gabriel.monk.Monkmod;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public final class SpiritualSphereModel {

    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of(Monkmod.MOD_ID, "spiritual_sphere"), "main");

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartBuilder b = ModelPartBuilder.create();

        // "Bola voxel": cubos 1x1x1 dentro de um raio
        int r = 3;                // raio em "voxels" (3 -> diâmetro ~7)
        float cube = 1.0f;        // tamanho do voxel
        float center = 0.5f;      // pra centralizar melhor

        // === A SUA TEXTURA ===
        final int TEX_W = 16;
        final int TEX_H = 16;

        // Para um cubo 1x1x1, o layout UV do cubo ocupa 4x2 pixels na textura.
        // Então o (u,v) precisa caber dentro disso pra não “sair da textura”.
        final int UV_BOX_W = 4;
        final int UV_BOX_H = 2;

        final int MAX_U = Math.max(0, TEX_W - UV_BOX_W); // 16-4 = 12
        final int MAX_V = Math.max(0, TEX_H - UV_BOX_H); // 16-2 = 14

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    double d2 = x * x + y * y + z * z;
                    if (d2 <= r * r) {
                        if ((Math.abs(x) == r && y == 0 && z == 0) ||
                                (Math.abs(y) == r && x == 0 && z == 0) ||
                                (Math.abs(z) == r && x == 0 && y == 0)) {
                            continue;
                        }
                        int uSeed = (x + 7) * 31 + (y + 11) * 17 + (z + 13) * 47;
                        int vSeed = (x + 3) * 19 + (y + 5) * 29 + (z + 9) * 23;

                        int u = (MAX_U == 0) ? 0 : Math.floorMod(uSeed, MAX_U + 1);
                        int v = (MAX_V == 0) ? 0 : Math.floorMod(vSeed, MAX_V + 1);

                        b.uv(u, v).cuboid(
                                (x - center) * cube,
                                (y - center) * cube,
                                (z - center) * cube,
                                cube, cube, cube,
                                new Dilation(0.0f)
                        );
                    }
                }
            }
        }

        root.addChild("sphere", b, ModelTransform.NONE);

        return TexturedModelData.of(modelData, TEX_W, TEX_H);
    }

    private SpiritualSphereModel() {}
}
