package fr.jinoxi.handicaps;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

public class HandicapMod implements ClientModInitializer {
	public static final String MOD_ID = "handicapchallenge";

	@Override
	public void onInitializeClient() {
		HandicapManager manager = HandicapManager.INSTANCE;

		// Nouvelle partie / deconnexion / fermeture du jeu
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> manager.reset(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> manager.shutdown(client));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> manager.shutdown(client));

		// Chrono et effets
		ClientTickEvents.START_CLIENT_TICK.register(manager::onStartTick);
		ClientTickEvents.END_CLIENT_TICK.register(manager::onEndTick);

		// Affichage : filtres visuels sous le HUD, liste + jauges au-dessus de tout
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
				Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
				(graphics, deltaTracker) -> manager.renderOverlay(graphics));
		HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES,
				Identifier.fromNamespaceAndPath(MOD_ID, "info"),
				(graphics, deltaTracker) -> manager.renderTop(graphics));

		// Chat
		ClientSendMessageEvents.ALLOW_CHAT.register(manager::allowChat);

		// Interactions (poser un bloc, utiliser un objet)
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
				manager.allowInteraction(level, hand) ? InteractionResult.PASS : InteractionResult.FAIL);
		UseItemCallback.EVENT.register((player, level, hand) ->
				manager.allowInteraction(level, hand) ? InteractionResult.PASS : InteractionResult.FAIL);

		// Commandes de test
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> HandicapCommand.register(dispatcher));
	}
}
