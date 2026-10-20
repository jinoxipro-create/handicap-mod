package fr.jinoxi.handicaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

/**
 * Cerveau du mod : gere les paliers, les handicaps actifs, l'affichage et le son.
 *
 * Regle des paliers : palier N = N handicaps, tous differents de ceux du palier precedent
 * (les anciens sont retires, pas accumules).
 */
public final class HandicapManager {
	public static final HandicapManager INSTANCE = new HandicapManager();

	private static final int DEFAULT_INTERVAL_MINUTES = 10;

	private final Random random = new Random();
	private final List<Handicap> active = new ArrayList<>();

	private int round = 0;
	private long tick = 0;
	private int intervalMinutes = DEFAULT_INTERVAL_MINUTES;
	private long ticksToNext = DEFAULT_INTERVAL_MINUTES * 60L * 20L;
	private boolean timerPaused = false;

	private Double savedVolume = null;
	private static boolean bypass = false;

	private HandicapManager() {
	}

	// ------------------------------------------------------------ cycle de vie

	/** Remet tout a zero (nouvelle partie, ou commande /handicap reset). */
	public void reset(Minecraft mc) {
		stopAll(mc);
		round = 0;
		tick = 0;
		timerPaused = false;
		ticksToNext = intervalMinutes * 60L * 20L;
		restoreSound(mc);
	}

	/** Arrete tous les handicaps et rend le son (deconnexion, fermeture du jeu). */
	public void shutdown(Minecraft mc) {
		stopAll(mc);
		restoreSound(mc);
	}

	private void stopAll(Minecraft mc) {
		for (Handicap h : active) {
			h.onStop(mc);
		}
		active.clear();
	}

	// ------------------------------------------------------------------ paliers

	/** Demarre le palier donne : choisit N handicaps differents de ceux du palier precedent. */
	public void startRound(Minecraft mc, int newRound) {
		round = Math.max(1, newRound);
		int count = Math.min(round, Handicaps.ALL.size());

		List<Handicap> previous = new ArrayList<>(active);
		stopAll(mc);

		List<Handicap> candidates = new ArrayList<>(Handicaps.ALL);
		candidates.removeAll(previous);
		Collections.shuffle(candidates, random);

		List<Handicap> chosen = new ArrayList<>(candidates.subList(0, Math.min(count, candidates.size())));
		if (chosen.size() < count) {
			// Pas assez de nouveaux handicaps (paliers tres eleves) : on complete avec d'anciens.
			Collections.shuffle(previous, random);
			for (Handicap h : previous) {
				if (chosen.size() >= count) {
					break;
				}
				chosen.add(h);
			}
		}
		chosen.sort((a, b) -> Integer.compare(Handicaps.ALL.indexOf(a), Handicaps.ALL.indexOf(b)));

		active.addAll(chosen);
		for (Handicap h : active) {
			h.onStart(mc, tick);
		}

		ticksToNext = intervalMinutes * 60L * 20L;
		timerPaused = false;
		announce(mc);
	}

	/** Mode test : un seul handicap, chrono en pause. */
	public boolean test(Minecraft mc, String id) {
		for (Handicap h : Handicaps.ALL) {
			if (h.id.equalsIgnoreCase(id)) {
				stopAll(mc);
				active.add(h);
				h.onStart(mc, tick);
				timerPaused = true;
				return true;
			}
		}
		return false;
	}

	public void setIntervalMinutes(int minutes) {
		intervalMinutes = Math.max(1, minutes);
		ticksToNext = intervalMinutes * 60L * 20L;
	}

	public int getRound() {
		return round;
	}

	public int getIntervalMinutes() {
		return intervalMinutes;
	}

	public boolean isTimerPaused() {
		return timerPaused;
	}

	public long getSecondsToNext() {
		return Math.max(0L, (ticksToNext + 19L) / 20L);
	}

	public List<Handicap> getActive() {
		return Collections.unmodifiableList(active);
	}

	private void announce(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			return;
		}
		p.displayClientMessage(Component.literal("\u00A76\u00A7lPALIER " + round + " \u00A7r\u00A7e: nouveaux handicaps !"), true);
		p.displayClientMessage(Component.literal("\u00A76\u00A7l=== PALIER " + round + " ==="), false);
		for (Handicap h : active) {
			p.displayClientMessage(Component.literal("\u00A7c- " + h.name + " \u00A77: " + h.description), false);
		}
		mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.0F));
	}

	// -------------------------------------------------------------------- ticks

	public void onStartTick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null || mc.isPaused()) {
			return;
		}
		for (Handicap h : active) {
			h.onStartTick(mc, p, tick);
		}
	}

	public void onEndTick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null || mc.isPaused()) {
			return;
		}
		tick++;
		if (!timerPaused) {
			ticksToNext--;
			if (ticksToNext <= 0) {
				startRound(mc, round + 1);
			}
		}
		for (Handicap h : active) {
			h.onEndTick(mc, p, tick);
		}
		updateSound(mc);
	}

	// -------------------------------------------------------------------- sons

	private void updateSound(Minecraft mc) {
		float multiplier = 1.0f;
		for (Handicap h : active) {
			multiplier = Math.min(multiplier, h.soundMultiplier());
		}
		OptionInstance<Double> master = mc.options.getSoundSourceOptionInstance(SoundSource.MASTER);
		if (multiplier >= 0.999f) {
			restoreSound(mc);
			return;
		}
		if (savedVolume == null) {
			savedVolume = master.get();
		}
		double target = savedVolume * multiplier;
		if (Math.abs(master.get() - target) > 0.0001) {
			master.set(target);
		}
	}

	private void restoreSound(Minecraft mc) {
		if (savedVolume != null) {
			mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(savedVolume);
			savedVolume = null;
		}
	}

	// --------------------------------------------------------- chat / interactions

	public boolean allowChat(String message) {
		if (bypass || active.isEmpty()) {
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		for (Handicap h : new ArrayList<>(active)) {
			if (!h.allowChat(mc, message, tick)) {
				return false;
			}
		}
		return true;
	}

	/** Envoie un message de chat sans repasser par les handicaps (utilise par l'aphasie). */
	public static void sendChatBypassingHandicaps(Minecraft mc, String text) {
		if (mc.player == null) {
			return;
		}
		bypass = true;
		try {
			mc.player.connection.sendChat(text);
		} finally {
			bypass = false;
		}
	}

	public boolean allowInteraction(Level level, InteractionHand hand) {
		// Les evenements sont aussi appeles cote serveur integre : on ne touche qu'au client.
		if (!level.isClientSide() || active.isEmpty()) {
			return true;
		}
		for (Handicap h : active) {
			if (!h.allowInteraction(hand, tick)) {
				return false;
			}
		}
		return true;
	}

	// ----------------------------------------------------------------- affichage

	public void renderOverlay(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || active.isEmpty()) {
			return;
		}
		int w = g.guiWidth();
		int h = g.guiHeight();
		for (Handicap handicap : active) {
			handicap.renderOverlay(g, w, h, tick);
		}
	}

	public void renderTop(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		Font font = mc.font;
		int w = g.guiWidth();
		int h = g.guiHeight();

		// Panneau en haut a gauche
		List<String> lines = new ArrayList<>();
		String time = formatTime(getSecondsToNext());
		if (timerPaused) {
			lines.add("\u00A76\u00A7lMODE TEST \u00A7r\u00A7e(chrono en pause, /handicap reset pour relancer)");
		} else if (round == 0) {
			lines.add("\u00A76\u00A7lPremier handicap dans \u00A7r\u00A7b" + time);
		} else {
			lines.add("\u00A76\u00A7lPALIER " + round + " \u00A7r\u00A7e- prochain dans \u00A7b" + time);
		}
		for (Handicap handicap : active) {
			lines.add("\u00A7c\u00A7l" + handicap.name + "\u00A7r \u00A77- " + handicap.description);
		}

		int maxWidth = 0;
		for (String line : lines) {
			maxWidth = Math.max(maxWidth, font.width(line));
		}
		int step = font.lineHeight + 2;
		g.fill(3, 3, 3 + maxWidth + 10, 3 + lines.size() * step + 6, 0x80000000);
		int y = 7;
		for (String line : lines) {
			g.drawString(font, line, 8, y, 0xFFFFFFFF, true);
			y += step;
		}

		for (Handicap handicap : active) {
			handicap.renderTop(g, font, w, h, tick);
		}
	}

	private static String formatTime(long seconds) {
		return String.format("%02d:%02d", seconds / 60, seconds % 60);
	}
}
