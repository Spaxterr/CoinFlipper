package me.spaxter.coinflipper;

import com.google.inject.Inject;
import me.spaxter.coinflipper.command.*;
import me.spaxter.coinflipper.inventory.CoinflipGUI;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(
        id = "coinflipper",
        version = "1.0",
        name = "CoinFlipper",
        description = "Allows players to flip a coin against each other for money.",
        dependencies = {@Dependency(id = "teslalibs")},
        authors = {
                "Spaxter"
        }
)

public class CoinFlipper {

    @Inject
    private Logger logger;

    @Inject
    public PluginContainer pluginContainer;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path defaultConfigPath;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configLoader;

    public EconomyService economyService;
    public EconomyManager economyManager;
    public ConfigurationNode config;
    public CoinflipGUI gui;


    @Listener
    public void onServerStarting(GameStartingServerEvent event)
    {
        logger = pluginContainer.getLogger();
        logger.info("Reading config file...");
        try {
            pluginContainer.getAsset("coinflipper.conf").get().copyToFile(defaultConfigPath, false, true);
            configLoader = HoconConfigurationLoader.builder().setPath(defaultConfigPath).build();
            config = configLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        Optional<EconomyService> oEconService = Sponge.getServiceManager().provide(EconomyService.class);
        logger.info("Checking for economy service...");
        if (!oEconService.isPresent()) {
            logger.warn("No economy service found! The plugin will not work as expected.");
            return;
        }
        economyService = oEconService.get();
        logger.info("Found economy service with default currency \"" + economyService.getDefaultCurrency().getDisplayName() + "\"");
        economyManager = new EconomyManager(this);
        gui = new CoinflipGUI(this, economyManager);
        gui.reset();
        logger.info("Loading commands...");
        loadCommands();
        logger.info("Started CoinFlipper!");
    }


    private void loadCommands()
    {
        CommandSpec createCommandSpec = CommandSpec.builder()
                .arguments(GenericArguments.onlyOne(GenericArguments.integer(Text.of("bet"))))
                .executor(new CoinflipCreate(this))
                .build();
        CommandSpec removeCommandSpec = CommandSpec.builder()
                .executor(new CoinflipCancel(gui))
                .build();
        CommandSpec challengeCommandSpec = CommandSpec.builder()
                .arguments(GenericArguments.onlyOne(GenericArguments.player(Text.of("player"))),
                        GenericArguments.onlyOne(GenericArguments.integer(Text.of("bet"))))
                .executor(new CoinflipChallenge(this))
                .build();
        CommandSpec acceptCommandSpec = CommandSpec.builder()
                .executor(new CoinflipAccept(gui))
                .build();
        CommandSpec declineCommandSpec = CommandSpec.builder()
                .executor(new CoinflipDecline())
                .build();
        CommandSpec mainCommandSpec = CommandSpec.builder()
                .description(Text.of("Opens the CoinFlip menu"))
                .executor(new Coinflip())
                .child(createCommandSpec, "create")
                .child(removeCommandSpec, "cancel")
                .child(challengeCommandSpec, "challenge")
                .child(acceptCommandSpec, "accept")
                .child(declineCommandSpec, "decline", "deny")
                .build();

        Sponge.getCommandManager().register(this, mainCommandSpec, "coinflip", "cf");
    }


    @Listener
    public void onPlayerLeave(ClientConnectionEvent.Disconnect event)
    {
        // If the player has an active bet when disconnecting, remove it
        if (CoinflipGUI.hasActiveBet(event.getTargetEntity()))
        {
            gui.removePlayer(event.getTargetEntity());
        }
    }


    @Listener
    public void onCloseInventory(InteractInventoryEvent.Close e, @Root Player p)
    {
        // If the player is currently flipping, don't let them close their inventory
        if(CoinflipGUI.active.contains(p.getUniqueId()) && e.getContext().containsKey(EventContextKeys.OWNER))
        {
            p.sendMessage(Text.of(TextColors.RED, "Please wait until the coinflip is done before closing the menu."));
            e.setCancelled(true);
        }
    }
}
