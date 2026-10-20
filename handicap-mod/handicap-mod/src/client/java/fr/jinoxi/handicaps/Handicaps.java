package fr.jinoxi.handicaps;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * Liste de tous les handicaps du mod (l'ordre de la liste est l'ordre de priorite).
 * Ce sont des CONTRAINTES DE JEU FICTIVES, pas des descriptions fideles de ces handicaps.
 */
public final class Handicaps {
	private static final Random RNG = new Random();

	private Handicaps() {
	}

	// ------------------------------------------------------------------ VUE

	private static final Handicap CECITE = new Handicap("cecite", "Cécité",
			"Écran presque entièrement noir") {
		@Override
		public void renderOverlay(GuiGraphics g, int w, int h, long tick) {
			g.fill(0, 0, w, h, 0xEC000000);
		}
	};

	private static final Handicap VISION_TUNNEL = new Handicap("tunnel", "Vision en tunnel",
			"Seul le centre de l'écran reste visible") {
		@Override
		public void renderOverlay(GuiGraphics g, int w, int h, long tick) {
			final int layers = 22;
			int maxX = (int) (w * 0.32f);
			int maxY = (int) (h * 0.32f);
			for (int i = 0; i < layers; i++) {
				int x0 = maxX * i / layers;
				int x1 = maxX * (i + 1) / layers;
				int y0 = maxY * i / layers;
				int y1 = maxY * (i + 1) / layers;
				float t = i / (float) layers;
				int a = Math.min(255, (int) (255 * 1.5f * (1f - t)));
				int color = a << 24;
				g.fill(x0, y0, w - x0, y1, color);
				g.fill(x0, h - y1, w - x0, h - y0, color);
				g.fill(x0, y1, x1, h - y1, color);
				g.fill(w - x1, y1, w - x0, h - y1, color);
			}
		}
	};

	private static final Handicap HEMIANOPSIE = new Handicap("hemianopsie", "Hemianopsie",
			"Une moitié de l'écran est masquée") {
		private boolean left;

		@Override
		public void onStart(Minecraft mc, long tick) {
			left = RNG.nextBoolean();
		}

		@Override
		public void renderOverlay(GuiGraphics g, int w, int h, long tick) {
			if (left) {
				g.fill(0, 0, w / 2, h, 0xFA000000);
			} else {
				g.fill(w / 2, 0, w, h, 0xFA000000);
			}
		}
	};

	private static final Handicap SCOTOME = new Handicap("scotome", "Scotome",
			"Une zone de l'écran devient invisible par moments") {
		private boolean shown;
		private long phaseEnd;
		private float fx;
		private float fy;

		@Override
		public void onStart(Minecraft mc, long tick) {
			shown = false;
			phaseEnd = tick + 100 + RNG.nextInt(100);
		}

		@Override
		public void onEndTick(Minecraft mc, LocalPlayer player, long tick) {
			if (tick < phaseEnd) {
				return;
			}
			if (shown) {
				shown = false;
				phaseEnd = tick + 120 + RNG.nextInt(160);
			} else {
				shown = true;
				fx = 0.12f + RNG.nextFloat() * 0.55f;
				fy = 0.12f + RNG.nextFloat() * 0.50f;
				phaseEnd = tick + 70 + RNG.nextInt(50);
			}
		}

		@Override
		public void renderOverlay(GuiGraphics g, int w, int h, long tick) {
			if (!shown) {
				return;
			}
			int x = (int) (fx * w);
			int y = (int) (fy * h);
			g.fill(x, y, x + (int) (w * 0.22f), y + (int) (h * 0.30f), 0xFF000000);
		}
	};

	private static final Handicap ACHROMATOPSIE = new Handicap("achromatopsie", "Achromatopsie",
			"Monde presque en noir et blanc (approximation)") {
		@Override
		public void renderOverlay(GuiGraphics g, int w, int h, long tick) {
			g.fill(0, 0, w, h, 0x99808080);
		}
	};

	private static final Handicap SENSIBILITE_LUMIERE = new Handicap("lumiere", "Sensibilité à la lumière",
			"Éblouissements doux et espacés (jamais de flash rapide)") {
		private long flashStart = -1000L;
		private long nextFlash;

		@Override
		public void onStart(Minecraft mc, long tick) {
			flashStart = -1000L;
			nextFlash = tick + 200 + RNG.nextInt(300);
		}

		@Override
		public void onEndTick(Minecraft mc, LocalPlayer player, long tick) {
			if (tick >= nextFlash) {
				flashStart = tick;
				nextFlash = tick + 500 + RNG.nextInt(400);
			}
		}

		@Override
		public void renderOverlay(GuiGraphics g, int w, int h, long tick) {
			long age = tick - flashStart;
			if (age < 0 || age > 200) {
				return;
			}
			float a = age < 5 ? 0.8f * age / 5f : 0.8f * (float) Math.exp(-(age - 5) / 28.0);
			int alpha = (int) (a * 255);
			if (alpha <= 3) {
				return;
			}
			g.fill(0, 0, w, h, (alpha << 24) | 0x00FFFFFF);
		}
	};

	// ------------------------------------------------------- OUIE ET COMMUNICATION

	private static final Handicap SURDITE = new Handicap("surdite", "Surdité",
			"Tous les sons du jeu sont coupés") {
		@Override
		public float soundMultiplier() {
			return 0.0f;
		}
	};

	private static final Handicap PERTE_AUDITIVE = new Handicap("audition", "Perte auditive partielle",
			"Le volume général est fortement réduit") {
		@Override
		public float soundMultiplier() {
			return 0.3f;
		}
	};

	private static final Handicap TROUBLE_PAROLE = new Handicap("parole", "Trouble de la parole",
			"30 s d'attente entre deux messages de chat") {
		private static final int DELAY_TICKS = 600;
		private long lastChat = -100000L;

		@Override
		public void onStart(Minecraft mc, long tick) {
			lastChat = -100000L;
		}

		@Override
		public boolean allowChat(Minecraft mc, String message, long tick) {
			long wait = DELAY_TICKS - (tick - lastChat);
			if (wait > 0) {
				if (mc.player != null) {
					long seconds = (wait + 19) / 20;
					mc.player.displayClientMessage(
							Component.literal("\u00A7cTrouble de la parole : attends encore " + seconds + " s."), true);
				}
				return false;
			}
			lastChat = tick;
			return true;
		}
	};

	private static final Handicap APHASIE = new Handicap("aphasie", "Aphasie",
			"Le chat n'envoie que des phrases prédéfinies") {
		private final String[] phrases = {
				"Oui", "Non", "Aide !", "Je ne sais pas", "Attends...", "Viens ici",
				"Merci", "Par là !", "Danger !", "D'accord"
		};

		@Override
		public boolean allowChat(Minecraft mc, String message, long tick) {
			HandicapManager.sendChatBypassingHandicaps(mc, phrases[RNG.nextInt(phrases.length)]);
			return false;
		}
	};

	// ----------------------------------------------------- MOTRICITE ET CONTROLE

	private static final Handicap TREMBLEMENTS = new Handicap("tremblements", "Tremblements essentiels",
			"Le viseur oscille en permanence") {
		private float appliedYaw;
		private float appliedPitch;

		private float targetYaw(long t) {
			return 0.9f * (float) (Math.sin(t * 0.83) + 0.6 * Math.sin(t * 2.1 + 1.0));
		}

		private float targetPitch(long t) {
			return 0.7f * (float) (Math.sin(t * 0.97 + 2.0) + 0.6 * Math.sin(t * 1.9 + 0.5));
		}

		@Override
		public void onStart(Minecraft mc, long tick) {
			appliedYaw = 0f;
			appliedPitch = 0f;
		}

		@Override
		public void onStop(Minecraft mc) {
			LocalPlayer p = mc.player;
			if (p != null) {
				p.turn(-appliedYaw / 0.15, -appliedPitch / 0.15);
			}
			appliedYaw = 0f;
			appliedPitch = 0f;
		}

		@Override
		public void onEndTick(Minecraft mc, LocalPlayer player, long tick) {
			if (mc.screen != null) {
				return;
			}
			float ty = targetYaw(tick);
			float tp = targetPitch(tick);
			player.turn((ty - appliedYaw) / 0.15, (tp - appliedPitch) / 0.15);
			appliedYaw = ty;
			appliedPitch = tp;
		}
	};

	private static final Handicap TICS = new Handicap("tics", "Tics moteurs",
			"Le viseur fait un petit saut de temps en temps") {
		private long nextTic;

		@Override
		public void onStart(Minecraft mc, long tick) {
			nextTic = tick + 300 + RNG.nextInt(300);
		}

		@Override
		public void onEndTick(Minecraft mc, LocalPlayer player, long tick) {
			if (tick < nextTic || mc.screen != null) {
				return;
			}
			float yaw = (RNG.nextBoolean() ? 1f : -1f) * (6f + RNG.nextFloat() * 8f);
			float pitch = (RNG.nextBoolean() ? 1f : -1f) * (2f + RNG.nextFloat() * 6f);
			player.turn(yaw / 0.15, pitch / 0.15);
			nextTic = tick + 300 + RNG.nextInt(500);
		}
	};

	private static final Handicap MOBILITE_REDUITE = new Handicap("mobilite", "Mobilité réduite",
			"Sprint désactivé et marche plus lente") {
		@Override
		public void onStartTick(Minecraft mc, LocalPlayer player, long tick) {
			mc.options.keySprint.setDown(false);
		}

		@Override
		public void onEndTick(Minecraft mc, LocalPlayer player, long tick) {
			if (!canAlterMovement(player)) {
				return;
			}
			if (player.isSprinting()) {
				player.setSprinting(false);
			}
			scaleHorizontalSpeed(player, 0.8);
		}
	};

	private static final Handicap JAMBE_LIMITEE = new Handicap("jambe", "Amputation d'une jambe",
			"Vitesse réduite et saut plus court") {
		@Override
		public void onEndTick(Minecraft mc, LocalPlayer player, long tick) {
			if (!canAlterMovement(player)) {
				return;
			}
			scaleHorizontalSpeed(player, 0.9);
			Vec3 v = player.getDeltaMovement();
			if (v.y > 0 && !player.isInWater() && !player.isInLava() && !player.onClimbable()) {
				player.setDeltaMovement(v.x, v.y * 0.92, v.z);
			}
		}
	};

	private static final Handicap MAIN_SECONDAIRE = new Handicap("main", "Main secondaire indisponible",
			"Impossible d'utiliser la main secondaire") {
		@Override
		public boolean allowInteraction(InteractionHand hand, long tick) {
			return hand != InteractionHand.OFF_HAND;
		}
	};

	private static final Handicap TEMPS_REACTION = new Handicap("reaction", "Temps de réaction allongé",
			"Délai de 0,75 s entre deux interactions") {
		private static final int COOLDOWN_TICKS = 15;
		private long lastAllowed = -1000L;

		@Override
		public void onStart(Minecraft mc, long tick) {
			lastAllowed = -1000L;
		}

		@Override
		public boolean allowInteraction(InteractionHand hand, long tick) {
			if (tick == lastAllowed) {
				return true;
			}
			if (tick - lastAllowed >= COOLDOWN_TICKS) {
				lastAllowed = tick;
				return true;
			}
			return false;
		}
	};

	private static final Handicap FAIBLESSE_MUSCULAIRE = new Handicap("endurance", "Faiblesse musculaire",
			"Jauge d'endurance : le sprint l'épuise") {
		private static final float MAX = 160f;
		private float stamina = MAX;
		private boolean exhausted;

		@Override
		public void onStart(Minecraft mc, long tick) {
			stamina = MAX;
			exhausted = false;
		}

		@Override
		public void onStartTick(Minecraft mc, LocalPlayer player, long tick) {
			if (exhausted) {
				mc.options.keySprint.setDown(false);
			}
		}

		@Override
		public void onEndTick(Minecraft mc, LocalPlayer player, long tick) {
			if (exhausted) {
				if (player.isSprinting()) {
					player.setSprinting(false);
					if (canAlterMovement(player)) {
						scaleHorizontalSpeed(player, 0.8);
					}
				}
				stamina += 0.7f;
				if (stamina >= MAX * 0.45f) {
					exhausted = false;
				}
				return;
			}
			if (player.isSprinting() && canAlterMovement(player)) {
				stamina -= 1f;
				if (stamina <= 0f) {
					stamina = 0f;
					exhausted = true;
					player.setSprinting(false);
					player.displayClientMessage(Component.literal("\u00A7cEssoufflé !"), true);
				}
			} else {
				stamina = Math.min(MAX, stamina + 0.8f);
			}
		}

		@Override
		public void renderTop(GuiGraphics g, Font font, int w, int h, long tick) {
			if (stamina >= MAX - 1f && !exhausted) {
				return;
			}
			int x0 = w / 2 - 30;
			int y0 = h / 2 + 16;
			g.fill(x0 - 1, y0 - 1, x0 + 61, y0 + 6, 0xAA000000);
			int filled = (int) (60 * Math.max(0f, stamina) / MAX);
			g.fill(x0, y0, x0 + filled, y0 + 5, exhausted ? 0xFFE53935 : 0xFF43A047);
		}
	};

	// ----------------------------------------------------------------- COGNITIF

	private static final class FakeNotification {
		final String text;
		final float fx;
		final float fy;
		final long expire;

		FakeNotification(String text, float fx, float fy, long expire) {
			this.text = text;
			this.fx = fx;
			this.fy = fy;
			this.expire = expire;
		}
	}

	private static final Handicap TDAH = new Handicap("notifications", "TDAH",
			"Fausses notifications qui détournent l'attention") {
		private final String[] texts = {
				"Nouveau message !", "1 notification", "Quelqu'un vous mentionne",
				"Mise à jour disponible", "Quête terminée ?", "Vous avez un cadeau !"
		};
		private final List<FakeNotification> notifications = new ArrayList<>();
		private long nextSpawn;

		@Override
		public void onStart(Minecraft mc, long tick) {
			notifications.clear();
			nextSpawn = tick + 100 + RNG.nextInt(100);
		}

		@Override
		public void onStop(Minecraft mc) {
			notifications.clear();
		}

		@Override
		public void onEndTick(Minecraft mc, LocalPlayer player, long tick) {
			notifications.removeIf(n -> tick >= n.expire);
			if (tick >= nextSpawn && notifications.size() < 3) {
				notifications.add(new FakeNotification(
						texts[RNG.nextInt(texts.length)],
						0.30f + RNG.nextFloat() * 0.45f,
						0.08f + RNG.nextFloat() * 0.70f,
						tick + 70));
				nextSpawn = tick + 100 + RNG.nextInt(200);
			}
		}

		@Override
		public void renderTop(GuiGraphics g, Font font, int w, int h, long tick) {
			for (FakeNotification n : notifications) {
				int tw = font.width(n.text);
				int x = Math.min((int) (n.fx * w), w - tw - 8);
				int y = (int) (n.fy * h);
				g.fill(x - 4, y - 4, x + tw + 4, y + font.lineHeight + 4, 0xCC202020);
				g.drawString(font, n.text, x, y, 0xFFFFFFFF, true);
			}
		}
	};

	/**
	 * L'ordre compte : "parole" doit rester avant "aphasie", "main" avant "reaction".
	 */
	public static final List<Handicap> ALL = List.of(
			CECITE, VISION_TUNNEL, HEMIANOPSIE, SCOTOME, ACHROMATOPSIE, SENSIBILITE_LUMIERE,
			SURDITE, PERTE_AUDITIVE, TROUBLE_PAROLE, APHASIE,
			TREMBLEMENTS, TICS, MOBILITE_REDUITE, JAMBE_LIMITEE, MAIN_SECONDAIRE, TEMPS_REACTION,
			FAIBLESSE_MUSCULAIRE, TDAH);
}
