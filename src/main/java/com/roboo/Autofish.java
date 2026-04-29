package com.roboo;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.FishingRodItem;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class Autofish implements ClientModInitializer {

	private static final Minecraft mc = Minecraft.getInstance();
	private static KeyMapping toggleKey;
	private static boolean enabled = false;

	private long lastCastTime = 0;
	private long nextActionTime = 0;
	private boolean waitingForHologram = false;
	private boolean biteDetected = false;
	private long biteTime = 0;

	private final Random random = new Random();

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath("autofish", "main")
		);

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"Autofish",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_LEFT_ALT,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(Minecraft client) {
		if (toggleKey.consumeClick()) {
			enabled = !enabled;
			String msg = enabled ? "§aAutoFish Enabled" : "§cAutoFish Disabled";
			client.gui.getChat().addMessage(Component.literal(msg));
		}

		if (!enabled || mc.player == null || mc.level == null) return;

		boolean holdingRod = mc.player.getMainHandItem().getItem() instanceof FishingRodItem ||
				mc.player.getOffhandItem().getItem() instanceof FishingRodItem;

		if (!holdingRod) {
			resetState();
			return;
		}

		long now = System.currentTimeMillis();

		if (now < nextActionTime) return;

		// Auto cast when not fishing
		if (!isFishing() && !waitingForHologram && !biteDetected) {
			rightClick(client);
			lastCastTime = now;
			waitingForHologram = true;
			return;
		}

		// Check for timer hologram (4.5, 3.7, etc.)
		if (waitingForHologram && now - lastCastTime > 700 && now - lastCastTime < 950) {
			if (!hasTimerHologramAnywhere()) {
				rightClick(client);
				nextActionTime = now + random.nextInt(120, 200);
				lastCastTime = now;
				waitingForHologram = true;
			} else {
				waitingForHologram = false;
			}
		}

		// Detect bite "!!!"
		if (!biteDetected && hasBiteHologramAnywhere()) {
			biteDetected = true;
			biteTime = now + random.nextInt(140, 260);
		}

		// Reel in after bite delay
		if (biteDetected && now >= biteTime) {
			rightClick(client);
			nextActionTime = now + random.nextInt(180, 330);
			lastCastTime = now;
			waitingForHologram = true;
			biteDetected = false;
		}
	}

	private void resetState() {
		waitingForHologram = false;
		biteDetected = false;
		nextActionTime = 0;
	}

	private boolean isFishing() {
		return mc.player != null && mc.player.fishing != null;
	}

	private void rightClick(Minecraft client) {
		if (mc.gameMode == null || mc.player == null) return;
		mc.player.swing(InteractionHand.MAIN_HAND);
		mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
	}

	private boolean hasTimerHologramAnywhere() {
		if (mc.level == null || mc.player == null) return false;

		return !mc.level.getEntitiesOfClass(ArmorStand.class,
				mc.player.getBoundingBox().inflate(20),
				stand -> {
					if (!stand.isInvisible()) return false;
					String clean = getCleanName(stand);
					return clean.matches(".*\\d+\\.\\d+.*") || clean.contains("#.");
				}).isEmpty();
	}

	private boolean hasBiteHologramAnywhere() {
		if (mc.level == null || mc.player == null) return false;

		return !mc.level.getEntitiesOfClass(ArmorStand.class,
				mc.player.getBoundingBox().inflate(80),
				stand -> {
					if (!stand.isInvisible()) return false;
					String clean = getCleanName(stand);
					return clean.contains("!!!");
				}).isEmpty();
	}

	private String getCleanName(ArmorStand stand) {
		if (stand == null || !stand.hasCustomName()) return "";

		var customName = stand.getCustomName();
		if (customName == null) return "";

		String raw = customName.getString();
		return raw.replaceAll("§[0-9a-fk-or]", "")
				.replaceAll("&[0-9a-fk-or]", "")
				.trim();
	}
}