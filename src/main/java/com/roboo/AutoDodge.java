package com.roboo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.Random;

public class AutoDodge implements ClientModInitializer {

    private static final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    private volatile boolean dodging = false;
    private volatile boolean forcedSneak = false;

    private volatile boolean parryActive = false;
    private volatile int safeCount = 0;
    private volatile int requiredSafes = 1;

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleMessage(message.getString());
        });

        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> {
            handleMessage(message.getString());
        });
    }

    private void handleMessage(String msg) {
        if (msg == null) return;

        String clean = msg.toLowerCase(Locale.ROOT);

        // PARRY
        if (clean.contains("parry") || clean.contains("parried")) {
            parryActive = true;
        }

        // DODGE START
        if (clean.contains("hold shift to dodge") && !dodging) {
            requiredSafes = parryActive ? 2 : 1;
            safeCount = 0;
            startDodging();
        }

        // SAFE
        if (clean.contains("safe")) {

            // Consume SAFE for parry first
            if (parryActive) {
                parryActive = false;
                return;
            }

            // Then apply to dodge
            if (dodging) {
                safeCount++;

                if (safeCount >= requiredSafes) {
                    stopDodging();
                }
            }
        }
    }

    private void startDodging() {
        dodging = true;

        new Thread(() -> {
            try {
                // slight delay before starting (200–300 ms)
                Thread.sleep(200 + random.nextInt(100));

                if (mc.player == null) return;

                KeyMapping sneakKey = mc.options.keyShift;

                if (!sneakKey.isDown()) {
                    sneakKey.setDown(true);
                    forcedSneak = true;
                }

            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    private void stopDodging() {
        if (mc.player == null) return;

        KeyMapping sneakKey = mc.options.keyShift;

        if (forcedSneak) {
            sneakKey.setDown(false);
            forcedSneak = false;
        }

        dodging = false;
    }
}