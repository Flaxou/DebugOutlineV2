package com.example.debugoutline;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public class DebugOutlineClient implements ClientModInitializer {

    /** Rayon de scan des chunks, en blocs. Limité par la distance de rendu réelle du client. */
    private static final int SCAN_RADIUS = 256;

    /** Au-delà de cette distance, les conteneurs ne sont pas dessinés. */
    private static final int MAX_RENDER_DISTANCE = 256;

    /** Rescan toutes les 20 ticks (1 s). Le résultat reste en cache entre deux scans. */
    private static final int SCAN_INTERVAL_TICKS = 20;

    private static boolean enabled = false;
    private static boolean tracers = true;
    private static int tickCounter = 0;

    /** Nombre de conteneurs réellement dessinés à la dernière frame (diagnostic). */
    private static int lastDrawn = 0;

    private static final EnumSet<ContainerType> ENABLED_TYPES = EnumSet.allOf(ContainerType.class);
    private static final Map<ContainerType, KeyBinding> FILTER_KEYS = new EnumMap<>(ContainerType.class);

    private static KeyBinding masterKey;
    private static KeyBinding tracerKey;
    private static KeyBinding rescanKey;

    @Override
    public void onInitializeClient() {
        masterKey = register("key.debugoutline.toggle", GLFW.GLFW_KEY_H);
        tracerKey = register("key.debugoutline.tracers", GLFW.GLFW_KEY_T);
        rescanKey = register("key.debugoutline.rescan", GLFW.GLFW_KEY_R);
        FILTER_KEYS.put(ContainerType.CHEST, register("key.debugoutline.chest", GLFW.GLFW_KEY_J));
        FILTER_KEYS.put(ContainerType.TRAPPED_CHEST, register("key.debugoutline.trapped", GLFW.GLFW_KEY_K));
        FILTER_KEYS.put(ContainerType.BARREL, register("key.debugoutline.barrel", GLFW.GLFW_KEY_L));
        FILTER_KEYS.put(ContainerType.SHULKER, register("key.debugoutline.shulker", GLFW.GLFW_KEY_U));

        ClientTickEvents.END_CLIENT_TICK.register(DebugOutlineClient::onClientTick);

        // Seul moment où un conteneur est retiré du cache : son chunk est déchargé.
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> ContainerCache.unload(chunk.getPos()));

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (enabled) {
                lastDrawn = OutlineRenderer.render(ctx, ENABLED_TYPES, tracers, MAX_RENDER_DISTANCE);
            }
        });
    }

    private static KeyBinding register(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                id, InputUtil.Type.KEYSYM, key, "category.debugoutline"));
    }

    private static void onClientTick(MinecraftClient client) {
        while (masterKey.wasPressed()) {
            enabled = !enabled;
            announce("Debug outline", enabled);
        }
        while (tracerKey.wasPressed()) {
            tracers = !tracers;
            announce("Tracers", tracers);
        }
        for (Map.Entry<ContainerType, KeyBinding> entry : FILTER_KEYS.entrySet()) {
            while (entry.getValue().wasPressed()) {
                ContainerType type = entry.getKey();
                if (!ENABLED_TYPES.remove(type)) {
                    ENABLED_TYPES.add(type);
                }
                announce(type.label, ENABLED_TYPES.contains(type));
            }
        }

        if (client.world == null || client.player == null) {
            ContainerCache.clear();
            return;
        }
        if (!enabled) {
            return;
        }

        boolean forced = false;
        while (rescanKey.wasPressed()) {
            forced = true;
        }
        if (forced || tickCounter++ % SCAN_INTERVAL_TICKS == 0) {
            ContainerCache.rescan(client.world, client.player.getBlockPos(), SCAN_RADIUS);
            if (forced) {
                announce("Rescan", true);
            }
        }

        // Diagnostic : cache = conteneurs connus ; dessinés = conteneurs envoyés au rendu.
        // Si « cache » reste à N mais que rien ne s'affiche, le problème vient du rendu.
        // Si « cache » tombe à 0 alors que tu vois des conteneurs, le problème vient du scan.
        if (tickCounter % SCAN_INTERVAL_TICKS == 1) {
            client.player.sendMessage(
                    Text.literal("Cache : " + ContainerCache.size() + " | Dessinés : " + lastDrawn)
                            .formatted(Formatting.GRAY),
                    true
            );
        }
    }

    private static void announce(String name, boolean on) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal(name + " : " + (on ? "ON" : "OFF"))
                            .formatted(on ? Formatting.GREEN : Formatting.RED),
                    true
            );
        }
    }
}
