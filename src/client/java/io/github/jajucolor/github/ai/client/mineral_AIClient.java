package io.github.jajucolor.github.ai.client;

import net.fabricmc.api.ClientModInitializer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import java.io.FileWriter;
import java.io.IOException;



public class mineral_AIClient implements ClientModInitializer {


    // 모드 초기화
    @Override
    public void onInitializeClient() {
        // 초기화 시 필요한 설정들
        System.out.println("Mineral Data Mod Initialized!");

        startTracking(MinecraftClient.getInstance());

    }

    // 광물 데이터 추적
    public static void trackMinerals(MinecraftClient client) {
        System.out.println("a");
        if (client.world == null) return;  // 월드가 없으면 리턴
        System.out.println("b");
        Chunk chunk = client.world.getChunk(client.player.getBlockPos());

        for (int y = 0; y < client.world.getHeight(); y++) {
            for (int x = chunk.getPos().x * 16; x < (chunk.getPos().x + 1) * 16; x++) {
                for (int z = chunk.getPos().z * 16; z < (chunk.getPos().z + 1) * 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.world.getBlockState(pos);

                    // 광물 확인 (예: 다이아몬드, 금, 철)
                    if (state.getBlock() == Blocks.DIAMOND_ORE || state.getBlock() == Blocks.IRON_ORE || state.getBlock() == Blocks.GOLD_ORE) {
                        // 광물 데이터를 파일로 저장
                        saveMineralDataToFile("Found mineral at Y=" + y + ": " + state.getBlock());
                    }
                }
            }
        }
    }

    // 광물 데이터를 파일에 저장
    public static void saveMineralDataToFile(String data) {
        try (FileWriter writer = new FileWriter("mineral_data.txt", true)) {
            writer.write(data + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 클라이언트 실행 시 광물 데이터 추적 시작
    public static void startTracking(MinecraftClient client) {
        System.out.println("c");
        trackMinerals(client);
    }

}