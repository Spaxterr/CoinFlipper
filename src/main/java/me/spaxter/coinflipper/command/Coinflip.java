package me.spaxter.coinflipper.command;

import me.spaxter.coinflipper.inventory.CoinflipGUI;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class Coinflip implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        CoinflipGUI.open((Player) src);
        return CommandResult.success();
    }
}
