package net.buycraft.plugin.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.buycraft.plugin.BuyCraftAPI;
import net.buycraft.plugin.data.responses.ServerInformation;
import net.buycraft.plugin.fabric.BuycraftPlugin;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class TebexCommand {
    private final BuycraftPlugin plugin;

    public TebexCommand(BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("tebex").executes(context -> {
                    if (checkPermission(context.getSource())) {
                        onBaseCommand(context);
                    }
                    return 1;
                }).then(CommandManager.literal("secret").then(CommandManager.argument("token", StringArgumentType.string()).executes(context -> {
                    if (checkPermission(context.getSource())) {
                        onSecretCommand(context);
                    }
                    return 1;
                }))).then(CommandManager.literal("forcecheck").executes(context -> {
                    if (checkPermission(context.getSource())) {
                        onForceCheckCommand(context);
                    }
                    return 1;
                })).then(CommandManager.literal("info").executes(context -> {
                    if (checkPermission(context.getSource())) {
                        onInfoCommand(context);
                    }
                    return 1;
                }))
        );
    }

    private boolean checkPermission(ServerCommandSource source) {
        if (!Permissions.check(source, "buycraft.admin", 4)) {
            source.sendError(new LiteralText("You do not have permission to use this command."));
            return false;
        }

        return true;
    }

    private void onBaseCommand(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText("Tebex - ").formatted(Formatting.AQUA).append(new LiteralText("TODO").formatted(Formatting.BOLD)), false);
    }

    private void onSecretCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity) {
            source.sendFeedback(new LiteralText(plugin.getI18n().get("secret_console_only")), false);
            return;
        }

        String token = context.getArgument("token", String.class);

        plugin.getPlatform().executeAsync(() -> {
            String currentKey = plugin.getConfiguration().getServerKey();
            BuyCraftAPI client;
            try {
                client = BuyCraftAPI.create(token, plugin.getHttpClient());
                plugin.updateInformation(client);
            } catch (IOException e) {
                plugin.getLogger().error("Unable to verify secret", e);
                source.sendFeedback(new LiteralText(plugin.getI18n().get("secret_does_not_work")).formatted(Formatting.RED), false);
                return;
            }

            ServerInformation information = plugin.getServerInformation();
            plugin.setApiClient(client);
            plugin.getListingUpdateTask().run();
            plugin.getConfiguration().setServerKey(token);
            try {
                plugin.saveConfiguration();
            } catch (IOException e) {
                source.sendFeedback(new LiteralText(plugin.getI18n().get("secret_cant_be_saved")).formatted(Formatting.RED), false);
            }
            source.sendFeedback(new LiteralText(plugin.getI18n().get("secret_success",
                    information.getServer().getName(), information.getAccount().getName())).formatted(Formatting.GREEN), false);

            boolean repeatChecks = currentKey.equals("INVALID");

            plugin.getDuePlayerFetcher().run(repeatChecks);
        });
    }

    private void onForceCheckCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (plugin.getApiClient() == null) {
            source.sendFeedback(new LiteralText(plugin.getI18n().get("need_secret_key")).formatted(Formatting.RED), false);
            return;
        }

        if (plugin.getDuePlayerFetcher().inProgress()) {
            source.sendFeedback(new LiteralText(plugin.getI18n().get("already_checking_for_purchases")).formatted(Formatting.RED), false);
            return;
        }

        plugin.getPlatform().executeAsync(() -> plugin.getDuePlayerFetcher().run(false));
        source.sendFeedback(new LiteralText(plugin.getI18n().get("forcecheck_queued")).formatted(Formatting.GREEN), false);
    }

    private void onInfoCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (plugin.getApiClient() == null) {
            source.sendFeedback(new LiteralText(plugin.getI18n().get("generic_api_operation_error")).formatted(Formatting.RED), false);
            return;
        }

        if (plugin.getServerInformation() == null) {
            source.sendFeedback(new LiteralText(plugin.getI18n().get("information_no_server")).formatted(Formatting.RED), false);
            return;
        }

        String webstoreURL = plugin.getServerInformation().getAccount().getDomain();
        LiteralText webstore = (LiteralText) new LiteralText(webstoreURL)
                .formatted(Formatting.GREEN)
                .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, webstoreURL)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(webstoreURL))));

        LiteralText server = (LiteralText) new LiteralText(plugin.getServerInformation().getServer().getName())
                .formatted(Formatting.GREEN);

        Arrays.asList(new LiteralText(plugin.getI18n().get("information_title") + " ").formatted(Formatting.GRAY),
                new LiteralText(plugin.getI18n().get("information_sponge_server") + " ").formatted(Formatting.GRAY).append(server),
                new LiteralText(plugin.getI18n().get("information_currency", plugin.getServerInformation().getAccount().getCurrency().getIso4217()))
                        .formatted(Formatting.GRAY),
                new LiteralText(plugin.getI18n().get("information_domain", "")).formatted(Formatting.GRAY).append(webstore)).forEach(item -> source.sendFeedback(item, false));
    }
}
