package com.roboo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.Random;

public class AutoParty implements ClientModInitializer {

    private static final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

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

        if (clean.contains("to parry") || clean.contains("dodge parry")) {
            triggerDropWithDelay();
        }
    }

    private void triggerDropWithDelay() {
        new Thread(() -> {
            try {
                // 200–300ms delay
                Thread.sleep(200 + random.nextInt(100));

                if (mc.player == null || mc.gameMode == null) return;

                mc.execute(() -> {
                    if (mc.player == null || mc.gameMode == null) return;
                    mc.player.drop(false);
                });

            } catch (InterruptedException ignored) {
            }
        }).start();
    }
}