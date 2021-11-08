package me.spaxter.coinflipper.command;

import me.spaxter.coinflipper.CoinFlipper;
import me.spaxter.coinflipper.EconomyManager;
import me.spaxter.coinflipper.inventory.CoinflipGUI;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CoinflipChallenge implements CommandExecutor {

    private final EconomyManager economyManager;

    public static Map<UUID, UUID> requests;
    public static Map<UUID, Integer> bets;
    private final CoinFlipper plugin;

    public CoinflipChallenge(CoinFlipper plugin)
    {
        this.plugin = plugin;
        requests = new HashMap<>();
        bets = new HashMap<>();
        economyManager = plugin.economyManager;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player target = args.<Player>getOne("player").get();
        int bet = args.<Integer>getOne("bet").get();
        int maxBet = plugin.config.getNode("max-bet").getInt();
        int minBet = plugin.config.getNode("min-bet").getInt();
        if (target.equals(src))
        {
            src.sendMessage(Text.of(TextColors.RED, "You can't play against yourself."));
            return CommandResult.empty();
        }
        if (bet > maxBet)
        {
            src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                    "&cYour bet needs to be lower than &e$" + NumberFormat.getNumberInstance(Locale.US).format(maxBet)
            ));
            return CommandResult.empty();
        }
        else if (bet < minBet)
        {
            src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                    "&cYour bet needs to be higher than &e$" + NumberFormat.getNumberInstance(Locale.US).format(minBet)
            ));
            return CommandResult.empty();
        }
        long delay = plugin.config.getNode("challenge-timeout").getLong();
        if (CoinflipGUI.hasActiveBet(target))
        {
            src.sendMessage(Text.of(TextSerializers.FORMATTING_CODE.deserialize(
                    "&cThat player already has an active coinflip!\n" +
                            "&cUse &e/coinflip &cto play against them."
            )));
            return CommandResult.empty();
        }
        if (CoinflipGUI.active.contains(target.getUniqueId()))
        {
            src.sendMessage(Text.of(TextColors.RED, target.getName() + " is currently playing against someone else. Please wait for them to finish."));
            return CommandResult.empty();
        }
        if (economyManager.hasMoney((Player) src, bet))
        {
            if (economyManager.hasMoney(target, bet))
            {
                src.sendMessage(Text.of(TextColors.GREEN, "Successfully sent a challenge request to " + target.getName()));
                target.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                        "&e" + src.getName() + " &dhas challenged you to a coinflip for &e$" + NumberFormat.getNumberInstance(Locale.US).format(bet) +
                                "\n&dType &e/coinflip accept &dor &e/coinflip deny\n" +
                                "&eYou have &6" + delay + "&e seconds to respond."
                ));
                requests.put(target.getUniqueId(), ((Player) src).getUniqueId());
                bets.put(target.getUniqueId(), bet);
                Task.builder().execute(
                        () -> {
                            try {
                                Player plr = (Player) src;
                                if (requests.containsKey(plr.getUniqueId()))
                                {
                                    requests.remove(plr.getUniqueId());
                                    bets.remove(plr.getUniqueId());
                                    src.sendMessage(Text.of(TextColors.YELLOW, "Coinflip challenge request timed out"));
                                    target.sendMessage(Text.of(TextColors.YELLOW, "Coinflip challenge request timed out"));
                                }
                            } catch (NullPointerException ignored)
                            {
                            }
                        }
                ).delay(delay, TimeUnit.SECONDS).submit(plugin.pluginContainer);
                return CommandResult.success();
            } else {
                src.sendMessage(Text.of(TextColors.RED, target.getName() + " doesn't have $" + NumberFormat.getNumberInstance(Locale.US).format(bet)));
                return CommandResult.empty();
            }

        } else {
            src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                    "&cYou don't have $" + NumberFormat.getNumberInstance(Locale.US).format(bet)
            ));
            return CommandResult.empty();
        }
    }
}
