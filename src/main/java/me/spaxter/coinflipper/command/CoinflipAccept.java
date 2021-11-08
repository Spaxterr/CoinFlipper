package me.spaxter.coinflipper.command;

import me.spaxter.coinflipper.inventory.CoinflipGUI;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

public class CoinflipAccept implements CommandExecutor {

    private final CoinflipGUI gui;

    public CoinflipAccept(CoinflipGUI gui)
    {
        this.gui = gui;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player plr = (Player) src;
        if (CoinflipGUI.hasActiveBet(plr))
        {
            src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                    "&cYou already have an active coinflip!\n" +
                            "Use &c/coinflip cancel &cto cancel it."
            ));
            return CommandResult.empty();
        }
        if (CoinflipChallenge.requests.containsKey(plr.getUniqueId())) {
            Optional<Player> requester = Sponge.getServer().getPlayer(CoinflipChallenge.requests.get(plr.getUniqueId()));
            if (!requester.isPresent())
            {
                src.sendMessage(Text.of(TextColors.RED, "Whoever challenged you is no longer online."));
                return CommandResult.empty();
            } else {
                CoinflipChallenge.requests.remove(plr.getUniqueId());
                gui.startCoinflip(requester.get(), plr, Optional.of(CoinflipChallenge.bets.get(plr.getUniqueId())));
                CoinflipChallenge.bets.remove(plr.getUniqueId());
                return CommandResult.success();
            }
        } else {
            src.sendMessage(Text.of(TextColors.RED, "You don't have any active coinflip challenge requests."));
            return CommandResult.empty();
        }
    }
}
