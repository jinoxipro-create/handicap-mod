package fr.jinoxi.handicaps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * Un handicap = une contrainte de gameplay fictive.
 * Toutes les methodes sont optionnelles : chaque handicap ne redefinit que ce dont il a besoin.
 */
public abstract class Handicap {
	public final String id;
	public final String name;
	public final String description;

	protected Handicap(String id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	/** Appele quand le handicap devient actif (reinitialiser l'etat ici). */
	public void onStart(Minecraft mc, long tick) {
	}

	/** Appele quand le handicap se termine (annuler ce qui doit l'etre ici). */
	public void onStop(Minecraft mc) {
	}

	/** Debut de chaque tick client (avant que le joueur ne bouge). */
	public void onStartTick(Minecraft mc, LocalPlayer player, long tick) {
	}

	/** Fin de chaque tick client (apres le mouvement du joueur). */
	public void onEndTick(Minecraft mc, LocalPlayer player, long tick) {
	}

	/** Dessine par-dessus le monde mais SOUS le HUD (barre d'objets, coeurs...). */
	public void renderOverlay(GuiGraphics g, int width, int height, long tick) {
	}

	/** Dessine par-dessus tout le HUD. */
	public void renderTop(GuiGraphics g, Font font, int width, int height, long tick) {
	}

	/** Multiplicateur du volume general (1 = normal, 0 = muet). */
	public float soundMultiplier() {
		return 1.0f;
	}

	/** Retourner false pour bloquer l'envoi d'un message de chat. */
	public boolean allowChat(Minecraft mc, String message, long tick) {
		return true;
	}

	/** Retourner false pour bloquer une interaction (poser un bloc, utiliser un objet...). */
	public boolean allowInteraction(InteractionHand hand, long tick) {
		return true;
	}

	// ---------------------------------------------------------------- utilitaires

	/** Vrai si on peut modifier le deplacement (pas en vol creatif, pas en elytres, pas en monture). */
	protected static boolean canAlterMovement(LocalPlayer p) {
		return !p.isPassenger() && !p.isFallFlying() && !p.getAbilities().flying;
	}

	/** Multiplie la vitesse horizontale du joueur. */
	protected static void scaleHorizontalSpeed(LocalPlayer p, double factor) {
		Vec3 v = p.getDeltaMovement();
		p.setDeltaMovement(v.x * factor, v.y, v.z * factor);
	}
}
