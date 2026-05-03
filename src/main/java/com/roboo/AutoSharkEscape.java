package com.roboo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.Locale;

public class AutoSharkEscape implements ClientModInitializer {

    private static final Minecraft mc = Minecraft.getInstance();

    private volatile boolean active = false;
    private volatile boolean holdingBack = false;
    private volatile boolean rotating = false;

    private long startTime = 0;
    private long holdStartTime = 0;

    private static final long DELAY = 3000;

    private static final double TARGET_X = -55;
    private static final double TARGET_Y = 103;
    private static final double TARGET_Z = 87;
    private static final double RANGE = 5.0;

    private static final long MAX_HOLD_TIME = 3000; // 3 seconds max

    @Override
    public void onInitializeClient() {

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleMessage(message.getString());
        });

        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> {
            handleMessage(message.getString());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
    }

    private void handleMessage(String msg) {
        if (msg == null) return;

        String clean = msg.toLowerCase(Locale.ROOT);

        if (clean.contains("a shark launches you")) {

            if (mc.player == null) return;

            // distance check BEFORE activating
            double dx = mc.player.getX() - TARGET_X;
            double dy = mc.player.getY() - TARGET_Y;
            double dz = mc.player.getZ() - TARGET_Z;

            if (dx * dx + dy * dy + dz * dz > RANGE * RANGE) return;

            startTime = System.currentTimeMillis();
            active = true;
            holdingBack = false;
            rotating = true;
        }
    }

    private void onTick() {
        if (!active || mc.player == null) return;

        long now = System.currentTimeMillis();
        KeyMapping backKey = mc.options.keyDown;

        // wait delay before action
        if (now - startTime < DELAY) return;

        // --- rotation phase ---
        if (rotating) {
            float currentYaw = mc.player.getYRot();
            float targetYaw = 0f;

            float diff = wrapDegrees(targetYaw - currentYaw);

            float step = Math.max(-5f, Math.min(5f, diff));
            mc.player.setYRot(currentYaw + step);

            if (Math.abs(diff) < 2f) {
                mc.player.setYRot(targetYaw);
                rotating = false;
            }

            return;
        }

        // --- start holding S ---
        if (!holdingBack) {
            backKey.setDown(true);
            holdingBack = true;
            holdStartTime = now;
        }

        // --- STOP CONDITIONS ---

        // 1. reached target position
        if (mc.player.getX() <= TARGET_X && mc.player.getZ() <= TARGET_Z) {
            stop(backKey);
            return;
        }

        // 2. max hold time safety
        if (now - holdStartTime >= MAX_HOLD_TIME) {
            stop(backKey);
        }
    }

    private void stop(KeyMapping backKey) {
        backKey.setDown(false);
        holdingBack = false;
        active = false;
        rotating = false;
    }

    private float wrapDegrees(float degrees) {
        degrees = degrees % 360.0F;
        if (degrees >= 180.0F) degrees -= 360.0F;
        if (degrees < -180.0F) degrees += 360.0F;
        return degrees;
    }
}