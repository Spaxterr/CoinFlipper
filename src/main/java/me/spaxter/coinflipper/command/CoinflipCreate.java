package me.spaxter.coinflipper.command;

import me.spaxter.coinflipper.CoinFlipper;
import me.spaxter.coinflipper.EconomyManager;
import me.spaxter.coinflipper.inventory.CoinflipGUI;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CoinflipCreate implements CommandExecutor {

    private final EconomyManager economyManager;
    private final ConfigurationNode config;
    private final PluginContainer container;
    private final CoinflipGUI gui;

    public CoinflipCreate(CoinFlipper plugin)
    {
        config = plugin.config;
        container = plugin.pluginContainer;
        this.economyManager = plugin.economyManager;
        this.gui = plugin.gui;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (CoinflipGUI.bets.size() >= 35)
        {
            src.sendMessage(Text.of(TextColors.RED, "The coinflip pool is currently full."));
            return CommandResult.empty();
        }
        if(!CoinflipGUI.hasActiveBet((Player) src))
        {
            int minBet = config.getNode("min-bet").getInt();
            int maxBet = config.getNode("max-bet").getInt();
            int bet = args.<Integer>getOne("bet").get();
            long delay = config.getNode("coinflip-timeout").getLong();
            if (bet >= minBet)
            {
                if (bet <= maxBet)
                {
                    if (economyManager.hasMoney((Player) src, bet))
                    {
                        gui.addPlayer((Player) src, bet);
                        if (config.getNode("announce-on-create").getBoolean())
                        {
                            String announcement = config.getNode("announcement").getString();
                            assert announcement != null;
                            announcement = announcement.replace("%player%", src.getName());
                            announcement = announcement.replace("%bet%", NumberFormat.getNumberInstance(Locale.US).format(bet));
                            Sponge.getServer().getBroadcastChannel().send(TextSerializers.FORMATTING_CODE.deserialize(announcement));
                        }
                        src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&bCreated a coinflip for &a$" + NumberFormat.getNumberInstance(Locale.US).format(bet) +
                                "\n&bUse &e/coinflip cancel &bif you change your mind."));
                        Task.builder().execute(
                                () -> {
                                    if (CoinflipGUI.hasActiveBet((Player) src))
                                    {
                                        gui.removePlayer((Player) src);
                                        src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                                                "&eYour coinflip has expired. Use &6/coinflip create <bet> &eif you would like to create a new one"
                                        ));
                                    }
                                }
                        ).delay(delay, TimeUnit.SECONDS).submit(container);
                        return CommandResult.success();
                    } else {
                        src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                                "&cYou don't have &e$" + NumberFormat.getNumberInstance(Locale.US).format(bet)
                                        + "\n&cCheck your balance with &e/balance"
                        ));
                        return CommandResult.empty();
                    }
                } else {
                    src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                            "&cYour bet needs to be lower than &e$" + NumberFormat.getNumberInstance(Locale.US).format(maxBet)
                    ));
                    return CommandResult.empty();
                }
            } else {
                src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                        "&cYour bet needs to be higher than &e$" + NumberFormat.getNumberInstance(Locale.US).format(minBet)
                ));
                return CommandResult.empty();
            }
        } else {
            src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize("&cYou already have an active coinflip bet!" +
                    "\nUse &e/coinflip cancel &cto remove your current bet."));
            return CommandResult.empty();
        }
    }
}
