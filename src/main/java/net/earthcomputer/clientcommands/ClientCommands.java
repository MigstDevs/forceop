package net.earthcomputer.clientcommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import dev.xpple.betterconfig.api.ModConfigBuilder;
import io.netty.buffer.Unpooled;
import net.earthcomputer.clientcommands.command.*;
import net.earthcomputer.clientcommands.render.RenderQueue;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientCommands implements ClientModInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Path configDir;
    private static final Set<String> clientcommandsCommands = new HashSet<>();
    public static final Identifier COMMAND_EXECUTION_PACKET_ID = new Identifier("clientcommands", "command_execution");
    private static final Set<String> COMMANDS_TO_NOT_SEND_TO_SERVER = Set.of("cwe", "cnote"); // could contain private information

    public static final boolean SCRAMBLE_WINDOW_TITLE = Util.make(() -> {
        String playerUUID = MinecraftClient.getInstance().getSession().getProfile().getId().toString();

        Set<String> victims = Set.of(
                "fa68270b-1071-46c6-ac5c-6c4a0b777a96", // Earthcomputer
                "d4557649-e553-413e-a019-56d14548df96", // Azteched
                "8dc3d945-cf90-47c1-a122-a576319d05a7", // samnrad
                "c5d72740-cabc-42d1-b789-27859041d553", // allocator
                "e4093360-a200-4f99-aa13-be420b8d9a79", // Rybot666
                "083fb87e-c9e4-4489-8fb7-a45b06bfca90", // Kerbaras
                "973e8f6e-2f51-4307-97dc-56fdc71d194f" // KatieTheQt
        );

        return victims.contains(playerUUID);
    });

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommands::registerCommands);

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            context.matrixStack().push();

            Vec3d cameraPos = context.camera().getPos();
            context.matrixStack().translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            RenderQueue.render(RenderQueue.Layer.ON_TOP, Objects.requireNonNull(context.consumers()).getBuffer(RenderQueue.NO_DEPTH_LAYER), context.matrixStack(), context.tickDelta());

            context.matrixStack().pop();
        });

        configDir = FabricLoader.getInstance().getConfigDir().resolve("clientcommands");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config dir", e);
        }

        new ModConfigBuilder("clientcommands", Configs.class).build();

        ItemGroupCommand.registerItemGroups();
    }

    private static Set<String> getCommands(CommandDispatcher<?> dispatcher) {
        return dispatcher.getRoot().getChildren().stream().flatMap(node -> node instanceof LiteralCommandNode<?> literal ? Stream.of(literal.getLiteral()) : Stream.empty()).collect(Collectors.toSet());
    }

    public static void sendCommandExecutionToServer(String command) {
        StringReader reader = new StringReader(command);
        reader.skipWhitespace();
        String theCommand = reader.readUnquotedString();
        if (clientcommandsCommands.contains(theCommand) && !COMMANDS_TO_NOT_SEND_TO_SERVER.contains(theCommand)) {
            if (ClientPlayNetworking.canSend(COMMAND_EXECUTION_PACKET_ID)) {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeString(command);
                ClientPlayNetworking.send(COMMAND_EXECUTION_PACKET_ID, buf);
            }
        }
    }

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        Set<String> existingCommands = getCommands(dispatcher);

        SelfOpCommand.register(dispatcher);

        clientcommandsCommands.clear();
        for (String command : getCommands(dispatcher)) {
            if (!existingCommands.contains(command)) {
                clientcommandsCommands.add(command);
            }
        }
    }
}
