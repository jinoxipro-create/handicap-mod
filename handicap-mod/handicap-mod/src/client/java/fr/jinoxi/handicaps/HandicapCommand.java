package fr.jinoxi.handicaps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Commandes de test cote client : /handicap ... */
public final class HandicapCommand {
	private HandicapCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		HandicapManager manager = HandicapManager.INSTANCE;

		dispatcher.register(ClientCommandManager.literal("handicap")
				.then(ClientCommandManager.literal("status").executes(ctx -> {
					FabricClientCommandSource src = ctx.getSource();
					src.sendFeedback(Component.literal("Palier " + manager.getRound()
							+ " | prochain dans " + manager.getSecondsToNext() + " s"
							+ " | intervalle " + manager.getIntervalMinutes() + " min"
							+ (manager.isTimerPaused() ? " | CHRONO EN PAUSE" : "")));
					for (Handicap h : manager.getActive()) {
						src.sendFeedback(Component.literal(" - " + h.name + " (" + h.id + ")"));
					}
					return 1;
				}))
				.then(ClientCommandManager.literal("list").executes(ctx -> {
					for (Handicap h : Handicaps.ALL) {
						ctx.getSource().sendFeedback(Component.literal(h.id + " : " + h.name + " - " + h.description));
					}
					return 1;
				}))
				.then(ClientCommandManager.literal("skip").executes(ctx -> {
					manager.startRound(Minecraft.getInstance(), manager.getRound() + 1);
					return 1;
				}))
				.then(ClientCommandManager.literal("reset").executes(ctx -> {
					manager.reset(Minecraft.getInstance());
					ctx.getSource().sendFeedback(Component.literal("Defi remis a zero : aucun handicap, chrono relance."));
					return 1;
				}))
				.then(ClientCommandManager.literal("round")
						.then(ClientCommandManager.argument("numero", IntegerArgumentType.integer(1, 50)).executes(ctx -> {
							manager.startRound(Minecraft.getInstance(), IntegerArgumentType.getInteger(ctx, "numero"));
							return 1;
						})))
				.then(ClientCommandManager.literal("interval")
						.then(ClientCommandManager.argument("minutes", IntegerArgumentType.integer(1, 120)).executes(ctx -> {
							manager.setIntervalMinutes(IntegerArgumentType.getInteger(ctx, "minutes"));
							ctx.getSource().sendFeedback(Component.literal("Intervalle : " + manager.getIntervalMinutes() + " min (chrono relance)."));
							return 1;
						})))
				.then(ClientCommandManager.literal("test")
						.then(ClientCommandManager.argument("id", StringArgumentType.word())
								.suggests((ctx, builder) -> {
									for (Handicap h : Handicaps.ALL) {
										builder.suggest(h.id);
									}
									return builder.buildFuture();
								})
								.executes(ctx -> {
									String id = StringArgumentType.getString(ctx, "id");
									if (!manager.test(Minecraft.getInstance(), id)) {
										ctx.getSource().sendFeedback(Component.literal("Handicap inconnu : " + id + " (voir /handicap list)"));
										return 0;
									}
									return 1;
								}))));
	}
}
