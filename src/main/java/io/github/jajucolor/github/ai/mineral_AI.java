package io.github.jajucolor.github.ai;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.util.Formatting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static net.minecraft.component.DataComponentTypes.*;
import static net.minecraft.component.type.NbtComponent.set;

import static net.minecraft.component.DataComponentTypes.WRITTEN_BOOK_CONTENT;

public class mineral_AI implements ModInitializer {

    private static MinecraftServer server;
    // 데이터 저장용 맵 (y좌표에 따른 광물 빈도수)
    private final Map<Integer, Map<Block, Integer>> mineralFrequency = new HashMap<>();
    // 선형 회귀 모델을 위한 데이터
    private final List<DataPoint> dataPoints = new ArrayList<>();
    private static final List<Block> ORES = Arrays.asList(
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE
    );

    @Override
    public void onInitialize() {

        // 서버 객체 초기화
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
        // 서버 시작 시 월드 정보를 가져오는 이벤트 리스너 등록
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerWorld world = server.getWorld(server.getOverworld().getRegistryKey()); // 월드 가져오기

            if (world != null) {
                calculateMineralFrequencies(world);  // 월드가 null이 아니면 광물 빈도 계산
            } else {
                System.err.println("Failed to get world!");
            }
        });

        // 커맨드 등록 메세지
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(
                    CommandManager.literal("sendmessage")  // 커맨드 이름
                            .executes(context -> {
                                mineralFrequency.forEach((y, minerals) -> {
                                    String information = "Y=" + y + " : " + minerals;
                                    sendMessageToAllPlayers(information);

                                });

                                sendMessageToAllPlayers(trainLinearRegression());

                                return 1;
                            })
            );
        });

        // 커맨드 등록 책
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("givebook")
                    .executes(context -> {
                        // 책 생성
                        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

                        ServerCommandSource source = context.getSource();
                        mineralFrequency.forEach((y, minerals) -> {
                            if (source.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity player) {

                                WrittenBookContentComponent infoBook = getWrittenBookContentComponent();
                                book.set(WRITTEN_BOOK_CONTENT, infoBook);

                            }
                        });
                        if (source.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity player) {

                            // 책 주기
                            player.giveItemStack(book);

                        }
                        return 1;

                    })
            );
        });


    }



    private void calculateMineralFrequencies(ServerWorld world) {
//        // 월드의 모든 청크를 순회하며 광물 빈도수를 측정
//        for (int chunkX = 0; chunkX < 10; chunkX++) {  // 실제로는 전체 범위로 변경
//            for (int chunkZ = 0; chunkZ < 10; chunkZ++) {  // 실제로는 전체 범위로 변경
//                Chunk chunk = world.getChunk(chunkX, chunkZ);
//
//                // 청크 내 각 y좌표에서 광물 블록을 확인
//                for (int y = 0; y <= 320; y++) {  // Minecraft 세계의 기본 높이 256
//                    ChunkSection section = chunk.getSection(y >> 4);  // 각 청크의 섹션을 가져옴 (16블록 단위)
//                    if (section != null) {
//                        for (int x = 0; x < 16; x++) {
//                            for (int z = 0; z < 16; z++) {
//                                BlockPos pos = new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z);
//                                Block block = section.getBlockState(x, y & 15, z).getBlock();  // 블록 상태 가져오기
//                                if (isMineral(block)) {  // 광물인지 확인
//                                    mineralFrequency.computeIfAbsent(y, k -> new HashMap<>());
//                                    mineralFrequency.get(y).merge(block, 1, Integer::sum);
//
//                                    // 선형 회귀 모델에 데이터 추가 (x, y, z 좌표, 광물 종류)
//                                    dataPoints.add(new DataPoint(chunkX * 16 + x, y, chunkZ * 16 + z, block));
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        // 월드의 모든 청크를 순회하며 광물 빈도수를 측정
//        for (int chunkX = -10; chunkX <= 10; chunkX++) {  // 실제로는 전체 범위로 변경
//            for (int chunkZ = -10; chunkZ <= 10; chunkZ++) {  // 실제로는 전체 범위로 변경
//                Chunk chunk = world.getChunk(chunkX, chunkZ);
//
        // 청크의 섹션 개수 확인
        int minY = -64;  // 세계의 최소 Y 좌표
        int maxY = 320;     // 세계의 최대 Y 좌표
//
//                for (int y = minY; y <= maxY; y++) {  // 수정된 범위: minY ~ maxY
//                    int sectionIndex = (y - minY) / 16;  // 섹션 인덱스 계산
//                    ChunkSection section = chunk.getSection(sectionIndex);  // 청크 섹션 가져오기
//
//                    if (section != null) {
//                        for (int x = 0; x <= 16; x++) {
//                            for (int z = 0; z <= 16; z++) {
//                                BlockPos pos = new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z);
//                                Block block = section.getBlockState(x, y & 15, z).getBlock();  // 블록 상태 가져오기
//                                if (isMineral(block)) {  // 광물인지 확인
//                                    mineralFrequency.computeIfAbsent(y, k -> new HashMap<>());
//                                    mineralFrequency.get(y).merge(block, 1, Integer::sum);
//
//                                    // 선형 회귀 모델에 데이터 추가 (x, y, z 좌표, 광물 종류)
//                                    dataPoints.add(new DataPoint(chunkX * 16 + x, y, chunkZ * 16 + z, block));
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        모종의 이유로 음수 좌표를 집계 안함 찾기 귀찮아서 다른 방법으로 코드 작성
//        ㄴ 사실 딥슬레이트 집계를 안함 - 당연히 음수 좌표에서는 일반 광물이 안나오지

        // 월드의 전체 영역을 체크
        for (int x = -80; x <= 80; x++) {
            for (int z = -80; z <= 80; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();

                    if (isOre(block)) {  // 광물인지 확인
                        mineralFrequency.computeIfAbsent(y, k -> new HashMap<>());
                        mineralFrequency.get(y).merge(block, 1, Integer::sum);

                        // 선형 회귀 모델에 데이터 추가 (x, y, z 좌표, 광물 종류와 빈도수)
                        int frequency = mineralFrequency.getOrDefault(y, new HashMap<>()).getOrDefault(getOreType(block), 0);
                        dataPoints.add(new DataPoint(x, y, z, getOreType(block), frequency));
                    }
                }
            }
        }

        // 결과 출력 (y좌표별로 어떤 광물이 얼마나 나왔는지)
        mineralFrequency.forEach((y, minerals) -> {

            System.out.println("Y=" + y + " : " + minerals);
        });
        System.out.println(trainLinearRegression());
        saveOreDataToFile(mineralFrequency);

    }

    private boolean isOre(Block block) {
        return ORES.contains(block);
    }

    private Block getOreType(Block block) {
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return Blocks.IRON_ORE;
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) return Blocks.GOLD_ORE;
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return Blocks.DIAMOND_ORE;
        return Blocks.AIR;
    }

    public String trainLinearRegression() {
        // 각 광물에 대해 선형 회귀 모델을 훈련
        Map<Block, io.github.jajucolor.github.ai.LinearRegression> regressionModels = new HashMap<>();

        // 각 광물에 대해 선형 회귀 모델 생성
        for (Block ore : Arrays.asList(Blocks.IRON_ORE, Blocks.GOLD_ORE, Blocks.DIAMOND_ORE)) {
            List<DataPoint> oreData = new ArrayList<>();
            for (DataPoint dp : dataPoints) {
                if (dp.block == ore) {
                    oreData.add(dp);
                }
            }
            if (!oreData.isEmpty()) {
                regressionModels.put(ore, new io.github.jajucolor.github.ai.LinearRegression(oreData));
            }
        }

        // 결과 출력
        StringBuilder result = new StringBuilder();
        for (Block ore : regressionModels.keySet()) {
            io.github.jajucolor.github.ai.LinearRegression model = regressionModels.get(ore);
            result.append("Predicted optimal Y-level for ")
                    .append(ore.getTranslationKey())
                    .append(": ")
                    .append(model.predictOptimalLocation())
                    .append("\n");
        }
        return result.toString();

    }

//    // 선형 회귀 모델 클래스(선형 회귀를 빙자한 평균값 찾는 코드)
//    static class LinearRegression {
//        private final List<DataPoint> dataPoints;
//
//        public LinearRegression(List<DataPoint> dataPoints) {
//            this.dataPoints = dataPoints;
//        }
//
//        // 간단한 예시로, 선형 회귀 모델을 훈련하고 예측하는 메소드
//        public String predictOptimalLocation() {
//            // 실제로는 여기서 선형 회귀 계산을 수행해야 하지만, 간단한 예시로 최소값을 반환
//            int optimalX = Integer.MAX_VALUE;
//            int optimalY = Integer.MAX_VALUE;
//            int optimalZ = Integer.MAX_VALUE;
//
//            for (DataPoint dp : dataPoints) {
//                // 여기에 선형 회귀 계산식 대신 가장 적합한 좌표를 출력
//                if (dp.x + dp.y + dp.z < optimalX + optimalY + optimalZ) {
//                    optimalX = dp.x;
//                    optimalY = dp.y;
//                    optimalZ = dp.z;
//                }
//            }
//
//            return "X: " + optimalX + ", Y: " + optimalY + ", Z: " + optimalZ;
//        }
//    }

    // 데이터 포인트 클래스 (x, y, z 좌표 및 블록 정보와 빈도)
    static class DataPoint {
        int x, y, z;
        Block block;
        int frequency;



        DataPoint(int x, int y, int z, Block block, int frequency) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.frequency = frequency;
        }
    }

    // 서버가 시작될 때 서버 객체 초기화
    private void onServerStart(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }

    // 모든 플레이어에게 메시지 전송하는 메소드
    private void sendMessageToAllPlayers(String message) {
        if (server != null) {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                sendMessageToPlayer(player, message);
            });
        }
    }

    // 특정 플레이어에게 메시지 전송하는 메소드
    private void sendMessageToPlayer(ServerPlayerEntity player, String message) {
        Text text = Text.literal(message).formatted(Formatting.DARK_PURPLE); // 텍스트 생성 및 색상 지정
        player.sendMessage(text, false); // 플레이어에게 메시지 전송
    }

    // 가진 정보를 책에 저장 (업데이트 때문에 제일 어려웠씀)
    private @NotNull WrittenBookContentComponent getWrittenBookContentComponent() {

        List<RawFilteredPair<Text>> coor = new ArrayList<>();

        coor.add(new RawFilteredPair<Text>(Text.literal(trainLinearRegression()), Optional.empty()));

        mineralFrequency.forEach((y, minerals) -> {

            String Information = "Y=" + y + " : " + minerals;
            coor.add(new RawFilteredPair<Text>(Text.literal(Information), Optional.empty()));

        });

        // Data Component 입력
        WrittenBookContentComponent infoBook = new WrittenBookContentComponent(new RawFilteredPair<String>("Information", Optional.of("asdf")), "Jajucolor", 3, coor, true);

        return infoBook;
    }

    // 광물 빈도 데이터를 파일에 저장하는 함수
    private void saveOreDataToFile(Map<Integer, Map<Block, Integer>> mineralFrequency) {
        // 저장할 파일 경로 지정
        Path filePath = new File("ore_frequency_data.txt").toPath();

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            // 데이터를 파일에 기록
            mineralFrequency.forEach((y, minerals) -> {
                try {
                    writer.write(y + ": " + minerals);
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            writer.write(trainLinearRegression());
            // 객기
            System.out.println("Ore frequency data saved to ore_frequency_data.txt");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}