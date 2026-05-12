package com.roboo;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.Locale;

public class AutoDodge {

    private static final Minecraft mc = Minecraft.getInstance();

    private boolean dodging = false;
    private boolean forcedSneak = false;

    private boolean parryActive = false;
    private int safeCount = 0;
    private int requiredSafes = 1;

    private long dodgeStartTime = 0;
    private static final long MAX_DODGE_TIME = 7000;

    public void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleMessage(message.getString());
        });

        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> {
            handleMessage(message.getString());
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (mc.player == null) return;

        if (!Autofish.isEnabled()) {
            if (dodging) stopDodging();
            return;
        }

        if (!dodging) return;

        long now = System.currentTimeMillis();

        if (now - dodgeStartTime > MAX_DODGE_TIME) {
            stopDodging();
            return;
        }

        var sneakKey = mc.options.keyShift;
        if (!sneakKey.isDown()) {
            sneakKey.setDown(true);
            forcedSneak = true;
        }
    }

    private void handleMessage(String msg) {
        if (msg == null) return;
        if (!Autofish.isEnabled()) return;

        String clean = msg.toLowerCase(Locale.ROOT);

        if (clean.contains("parry") || clean.contains("parried")) {
            parryActive = true;
        }

        if (clean.contains("hold shift to dodge") && !dodging) {
            requiredSafes = parryActive ? 2 : 1;
            safeCount = 0;
            startDodging();
        }

        if (clean.contains("safe")) {
            if (parryActive) {
                parryActive = false;
                return;
            }

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
        dodgeStartTime = System.currentTimeMillis();
        safeCount = 0;
    }

    private void stopDodging() {
        if (mc.player == null) return;

        var sneakKey = mc.options.keyShift;
        if (forcedSneak || sneakKey.isDown()) {
            sneakKey.setDown(false);
        }

        forcedSneak = false;
        dodging = false;
        safeCount = 0;
    }
}