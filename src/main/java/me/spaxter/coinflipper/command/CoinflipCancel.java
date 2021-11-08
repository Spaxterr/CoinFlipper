package me.spaxter.coinflipper.command;

import me.spaxter.coinflipper.inventory.CoinflipGUI;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

public class CoinflipCancel implements CommandExecutor {

    private final CoinflipGUI gui;

    public CoinflipCancel(CoinflipGUI gui)
    {
        this.gui = gui;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (CoinflipGUI.hasActiveBet((Player) src))
        {
            gui.removePlayer((Player) src);
            src.sendMessage(Text.of(TextColors.GREEN, "&aSuccessfully cancelled your current coinflip."));
            return CommandResult.success();
        } else {
            src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                    "&cYou do not have an active coinflip.\nUse &e/coinflip create <bet> &cto create one!"
            ));
            return CommandResult.empty();
        }
    }
}
