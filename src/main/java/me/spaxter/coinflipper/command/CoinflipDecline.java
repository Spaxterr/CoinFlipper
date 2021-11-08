package me.spaxter.coinflipper.command;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;

public class CoinflipDecline implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player plr = (Player) src;
        if (CoinflipChallenge.requests.containsKey(plr.getUniqueId())) {
            src.sendMessage(Text.of(TextColors.YELLOW, "You have declined the coinflip challenge."));
            Optional<Player> requester = Sponge.getServer().getPlayer(CoinflipChallenge.requests.get(plr.getUniqueId()));
            requester.ifPresent(player -> player.sendMessage(Text.of(TextColors.RED, src.getName() + " has declined your coinflip challenge.")));
            CoinflipChallenge.requests.remove(((Player) src).getUniqueId());
            return CommandResult.success();
        } else {
            src.sendMessage(Text.of(TextColors.RED, "You don't have any active coinflip challenge requests."));
            return CommandResult.empty();
        }
    }
}
