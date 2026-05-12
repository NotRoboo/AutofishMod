package com.roboo;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.Locale;

public class AutoParty {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final int DROP_DELAY_TICKS = 4;

    private int dropTicks = 0;

    public void init() {
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
        if (!Autofish.isEnabled()) return;

        String clean = msg.toLowerCase(Locale.ROOT);

        if (clean.contains("to parry") || clean.contains("dodge parry")) {
            dropTicks = DROP_DELAY_TICKS;
        }
    }

    private void onTick() {
        if (!Autofish.isEnabled()) {
            dropTicks = 0;
            return;
        }

        if (dropTicks <= 0) return;

        dropTicks--;

        if (dropTicks == 0) {
            if (mc.player == null || mc.gameMode == null) return;

            mc.execute(() -> {
                if (mc.player == null || mc.gameMode == null) return;
                mc.player.drop(false);
            });
        }
    }
}