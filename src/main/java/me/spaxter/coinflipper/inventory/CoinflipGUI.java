package me.spaxter.coinflipper.inventory;

import com.mcsimonflash.sponge.teslalibs.inventory.Action;
import com.mcsimonflash.sponge.teslalibs.inventory.Element;
import com.mcsimonflash.sponge.teslalibs.inventory.Layout;
import com.mcsimonflash.sponge.teslalibs.inventory.View;
import me.spaxter.coinflipper.CoinFlipper;
import me.spaxter.coinflipper.EconomyManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class CoinflipGUI {

    public static Map<UUID, Integer> bets;          // Map for storing active bets
    public static HashSet<UUID> active;             // Set containing active coinflips
    public static HashSet<UUID> shouldCancel;       // Set for keeping track of if a coinflip should be cancelled
    public static View mainMenu;                    // Main menu inventory view
    private static PluginContainer container;
    private final EconomyManager economyManager;

    public CoinflipGUI(CoinFlipper plugin, EconomyManager pluginEconomyManager)
    {
        economyManager = pluginEconomyManager;
        active = new HashSet<>();
        shouldCancel = new HashSet<>();
        container = plugin.pluginContainer;
    }


    /**
     * Resets the main menu GUI
     */
    public void reset()
    {
        // Create ItemStacks for the background and border
        ItemStack backgroundItem = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        ItemStack borderItem = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);

        // Set the colour of the border to red
        borderItem.offer(Keys.DYE_COLOR, DyeColors.RED);

        // Set click action to cancel
        Consumer<Action.Click> action = a -> a.getEvent().setCancelled(true);

        // Create background and border elements
        Element backgroundElement = Element.of(backgroundItem, action);
        Element borderElement = Element.of(borderItem, action);

        // Create the layout
        Layout layout = Layout.builder()
                .border(borderElement)
                .fill(backgroundElement)
                .build();
        InventoryArchetype archetype = InventoryArchetype.builder()
                .title(Text.of("Coinflip Menu"))
                .property(InventoryDimension.of(9, 6))
                .build(UUID.randomUUID().toString(), "Coinflip Main Menu");
        // Assign mainMenu the view
        mainMenu = View.of(archetype, container).define(layout);
    }


    /**
     * Opens the main menu
     * @param plr The player to open the menu for
     */
    public static void open(Player plr)
    {
        mainMenu.open(plr);
    }


    /**
     * Opens the menu to challenge another player to a coinflip
     * @param plr The player to open the menu for
     * @param target The player they are challenging
     */
    public void openCoinflip(Player plr, Player target)
    {
        ItemStack backgroundItem = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        ItemStack borderItem = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        ItemStack playButton = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        ItemStack playerHead = ItemStack.of(ItemTypes.SKULL, 1);
        ItemStack targetHead = ItemStack.of(ItemTypes.SKULL, 1);

        borderItem.offer(Keys.DYE_COLOR, DyeColors.RED);
        playButton.offer(Keys.DYE_COLOR, DyeColors.LIME);
        playButton.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GREEN, "Play!"));
        // Create an action that starts a coinflip when clicked
        Consumer<Action.Click> playAction = a -> {
            if (bets.containsKey(target.getUniqueId())) {
                plr.playSound(SoundTypes.UI_BUTTON_CLICK, a.getPlayer().getPosition(), 0.7);
                startCoinflip(plr, target, Optional.empty());
            }
            else {
                a.getPlayer().getOpenInventory().get().close(plr);
                plr.sendMessage(Text.of(TextColors.RED, "That coinflip is no longer active."));
                a.getEvent().setCancelled(true);
            }
        };

        // Create the playerheads
        playerHead.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER);
        playerHead.offer(Keys.REPRESENTED_PLAYER, plr.getProfile());
        targetHead.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER);
        targetHead.offer(Keys.REPRESENTED_PLAYER, target.getProfile());

        // Create the layout
        Layout layout = Layout.builder()
                .border(Element.of(borderItem))
                .set(Element.of(playButton, playAction), 49)
                .set(Element.of(playerHead), 20)
                .set(Element.of(targetHead), 24)
                .fill(Element.of(backgroundItem))
                .build();
        InventoryArchetype archetype = InventoryArchetype.builder()
                .title(Text.of("Coinflip Menu"))
                .property(InventoryDimension.of(9, 6))
                .build(UUID.randomUUID().toString(), target.getName() + "'s Coinflip");

        View coinflipMenu = View.of(archetype, container).define(layout);
        coinflipMenu.open(plr);
    }


    /**
     * Starts a coinflip between two players
     * @param plr The player who clicked
     * @param target The player who created the coinflip
     */
    public void startCoinflip(Player plr, Player target, Optional<Integer> money)
    {
        int bet = money.orElseGet(() ->
            bets.get(target.getUniqueId())
        );

        removePlayer(target);
        ItemStack backgroundItem = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        ItemStack borderItem = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        ItemStack playerHead = ItemStack.of(ItemTypes.SKULL, 1);
        ItemStack targetHead = ItemStack.of(ItemTypes.SKULL, 1);

        ItemStack colorItem1 = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        ItemStack colorItem2 = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        colorItem1.offer(Keys.DYE_COLOR, DyeColors.YELLOW);
        colorItem2.offer(Keys.DYE_COLOR, DyeColors.BLUE);

        ItemStack coinItem = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
        coinItem.offer(Keys.DYE_COLOR, DyeColors.YELLOW);

        borderItem.offer(Keys.DYE_COLOR, DyeColors.RED);

        // Create the playerheads
        playerHead.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER);
        playerHead.offer(Keys.REPRESENTED_PLAYER, plr.getProfile());
        targetHead.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER);
        targetHead.offer(Keys.REPRESENTED_PLAYER, target.getProfile());

        // Create the layout
        Layout layout = Layout.builder()
                .border(Element.of(borderItem))
                .set(Element.of(playerHead), 20)
                .set(Element.of(coinItem), 22)
                .set(Element.of(colorItem1), 29)
                .set(Element.of(targetHead), 24)
                .set(Element.of(colorItem2), 33)
                .fill(Element.of(backgroundItem))
                .build();
        InventoryArchetype archetype = InventoryArchetype.builder()
                .title(Text.of("Coinflip Menu"))
                .property(InventoryDimension.of(9, 6))
                .build(UUID.randomUUID().toString(), target.getName() + "'s Coinflip");

        View view = View.of(archetype, container).define(layout);
        view.open(plr);
        view.open(target);
        active.add(target.getUniqueId());
        active.add(plr.getUniqueId());
        AtomicInteger counter = new AtomicInteger();
        Random rand = new Random();
        int limit = rand.nextInt(18 - 11) + 11;
        short winner = (short) rand.nextInt(2);
        // Create a task that
        Task.builder().interval(250, TimeUnit.MILLISECONDS).execute(task -> {
            counter.addAndGet(1);
            // If the counter is even, set color to yellow
            if (counter.get() % 2 == 0) {
                coinItem.offer(Keys.DYE_COLOR, DyeColors.YELLOW);
                Layout newLayout = Layout.builder()
                        .border(Element.of(borderItem))
                        .set(Element.of(playerHead), 20)
                        .set(Element.of(coinItem), 22)
                        .set(Element.of(colorItem1), 29)
                        .set(Element.of(targetHead), 24)
                        .set(Element.of(colorItem2), 33)
                        .fill(Element.of(backgroundItem))
                        .build();
                view.update(newLayout);
                plr.playSound(SoundTypes.BLOCK_STONE_BUTTON_CLICK_OFF, plr.getPosition(), 0.5);
                target.playSound(SoundTypes.BLOCK_STONE_BUTTON_CLICK_OFF, plr.getPosition(), 0.5);
            } else { // Otherwise, blue
                coinItem.offer(Keys.DYE_COLOR, DyeColors.BLUE);
                Layout newLayout = Layout.builder()
                        .border(Element.of(borderItem))
                        .set(Element.of(playerHead), 20)
                        .set(Element.of(coinItem), 22)
                        .set(Element.of(colorItem1), 29)
                        .set(Element.of(targetHead), 24)
                        .set(Element.of(colorItem2), 33)
                        .fill(Element.of(backgroundItem))
                        .build();
                view.update(newLayout);
                plr.playSound(SoundTypes.BLOCK_STONE_BUTTON_CLICK_ON, plr.getPosition(), 0.5);
                target.playSound(SoundTypes.BLOCK_STONE_BUTTON_CLICK_ON, plr.getPosition(), 0.5);
            }
            // If the counter has reached it's limit
            if (counter.get() == limit)
            {
                if(!economyManager.hasMoney(plr, bet) || !economyManager.hasMoney(target, bet))
                {
                    active.remove(target.getUniqueId());
                    active.remove(plr.getUniqueId());
                    plr.sendMessage(Text.of(TextColors.RED, "The coinflip was cancelled due to one (or both) of the players no longer having enough money to play."));
                    target.sendMessage(Text.of(TextColors.RED, "The coinflip was cancelled due to one (or both) of the players no longer having enough money to play."));

                    task.cancel();
                    return;
                }
                ItemStack playAgainButton = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
                playAgainButton.offer(Keys.DYE_COLOR, DyeColors.GREEN);
                playAgainButton.offer(Keys.DISPLAY_NAME, Text.of(TextColors.YELLOW, "Play again?"));
                AtomicBoolean playAgain1 = new AtomicBoolean(false), playAgain2 = new AtomicBoolean(false);
                Consumer<Action.Click> playAgainAction = a -> {
                    if (a.getPlayer().equals(plr))
                    {
                        if (economyManager.hasMoney(plr, bet))
                        {
                            playAgain1.set(true);
                            ItemStack eItemStack = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
                            eItemStack.offer(Keys.DYE_COLOR, DyeColors.GREEN);
                            Element e = Element.of(eItemStack);
                            Layout newLayout = Layout.builder()
                                    .border(Element.of(borderItem))
                                    .set(Element.of(playerHead), 20)
                                    .set(Element.of(coinItem), 22)
                                    .set(Element.of(colorItem1), 29)
                                    .set(Element.of(targetHead), 24)
                                    .set(Element.of(colorItem2), 33)
                                    .set(e, 38)
                                    .fill(Element.of(backgroundItem))
                                    .build();
                            view.update(newLayout);
                        } else {
                            plr.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                                    "&cYou don't have enough money to play again."
                            ));
                            if (plr.getOpenInventory().isPresent())
                                plr.getOpenInventory().get().close(plr);
                        }
                    }
                    if (a.getPlayer().equals(target))
                    {
                        if (economyManager.hasMoney(target, bet))
                        {
                            playAgain2.set(true);
                            ItemStack eItemStack = ItemStack.of(ItemTypes.STAINED_GLASS_PANE, 1);
                            eItemStack.offer(Keys.DYE_COLOR, DyeColors.GREEN);
                            Element e = Element.of(eItemStack);
                            Layout newLayout = Layout.builder()
                                    .border(Element.of(borderItem))
                                    .set(Element.of(playerHead), 20)
                                    .set(Element.of(coinItem), 22)
                                    .set(Element.of(colorItem1), 29)
                                    .set(Element.of(targetHead), 24)
                                    .set(Element.of(colorItem2), 33)
                                    .set(e, 38)
                                    .fill(Element.of(backgroundItem))
                                    .build();
                            view.update(newLayout);
                        }  else {
                            plr.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                                    "&cYou don't have enough money to play again."
                            ));
                            if (plr.getOpenInventory().isPresent())
                                plr.getOpenInventory().get().close(plr);
                        }
                    }
                    if (playAgain1.get() && playAgain2.get())
                    {
                        startCoinflip(plr, target, Optional.of(bet));
                    }
                };
                if (winner == 0)
                {
                    coinItem.offer(Keys.DYE_COLOR, DyeColors.BLUE);
                    borderItem.offer(Keys.DYE_COLOR, DyeColors.BLUE);
                    Layout newLayout = Layout.builder()
                            .border(Element.of(borderItem))
                            .set(Element.of(playerHead), 20)
                            .set(Element.of(coinItem), 22)
                            .set(Element.of(colorItem1), 29)
                            .set(Element.of(targetHead), 24)
                            .set(Element.of(colorItem2), 33)
                            .set(Element.of(playAgainButton, playAgainAction), 49)
                            .fill(Element.of(backgroundItem))
                            .build();
                    view.update(newLayout);
                    economyManager.transferMoney(plr, target, bet);
                    target.sendMessage(Text.of(TextColors.GREEN, "You won the coinflip!"));
                    plr.sendMessage(Text.of(TextColors.RED, "You lost the coinflip."));
                } else {
                    coinItem.offer(Keys.DYE_COLOR, DyeColors.YELLOW);
                    borderItem.offer(Keys.DYE_COLOR, DyeColors.YELLOW);
                    Layout newLayout = Layout.builder()
                            .border(Element.of(borderItem))
                            .set(Element.of(playerHead), 20)
                            .set(Element.of(coinItem), 22)
                            .set(Element.of(colorItem1), 29)
                            .set(Element.of(targetHead), 24)
                            .set(Element.of(colorItem2), 33)
                            .set(Element.of(playAgainButton, playAgainAction), 49)
                            .fill(Element.of(backgroundItem))
                            .build();
                    view.update(newLayout);
                    economyManager.transferMoney(target, plr, bet);
                }
                plr.playSound(SoundTypes.ENTITY_EXPERIENCE_ORB_PICKUP, plr.getPosition(), 0.5);
                target.playSound(SoundTypes.ENTITY_EXPERIENCE_ORB_PICKUP, plr.getPosition(), 0.5);
                active.remove(target.getUniqueId());
                active.remove(plr.getUniqueId());
                task.cancel();
            }
        }).submit(container);
    }


    /**
     * Add a player to the main menu page
     * @param plr The player to add
     * @param bet The bet they submitted
     */
    public void addPlayer(Player plr, int bet)
    {
        bets.put(plr.getUniqueId(), bet);
        ItemStack playerHead = ItemStack.of(ItemTypes.SKULL, 1);
        playerHead.offer(Keys.SKULL_TYPE, SkullTypes.PLAYER);
        playerHead.offer(Keys.REPRESENTED_PLAYER, plr.getProfile());
        playerHead.offer(Keys.DISPLAY_NAME, TextSerializers.FORMATTING_CODE.deserialize("&f&l" + plr.getName()));
        Consumer<Action.Click> headAction = a -> {
            Optional<Player> targetOpt = Sponge.getServer().getPlayer(a.getElement().getItem().get(Keys.REPRESENTED_PLAYER).get().getUniqueId());
            if (!targetOpt.isPresent())
            {
                a.getPlayer().sendMessage(Text.of(TextColors.RED, "That player is no longer online."));
                a.getEvent().getTargetInventory().close(plr);
                a.getEvent().setCancelled(true);
            } else {
                if (targetOpt.get().equals(a.getPlayer()))
                {
                    a.getPlayer().sendMessage(Text.of(TextColors.RED, "You can't play against yourself."));
                    a.getEvent().setCancelled(true);
                } else {
                    if (economyManager.hasMoney(a.getPlayer(), bet))
                    {
                        if (economyManager.hasMoney(targetOpt.get(), bet))
                        {
                            openCoinflip(a.getPlayer(), targetOpt.get());
                            a.getPlayer().playSound(SoundTypes.UI_BUTTON_CLICK, a.getPlayer().getPosition(), 0.7);
                        } else {
                            removePlayer(targetOpt.get());
                            a.getPlayer().sendMessage(Text.of(TextColors.RED, "That player can no longer afford their bet."));
                            a.getEvent().setCancelled(true);
                        }
                    } else {
                        a.getPlayer().sendMessage(TextSerializers.FORMATTING_CODE.deserialize(
                                "&cYou don't have &e$" + NumberFormat.getNumberInstance(Locale.US).format(bet) + "\n" +
                                        "&cCheck your balance with &e/balance"
                        ));
                        a.getEvent().setCancelled(true);
                    }
                }
            }

        };
        List<Text> lore = new ArrayList<>();
        lore.add(Text.of(TextColors.GREEN, "$" + NumberFormat.getNumberInstance(Locale.US).format(bet)));
        lore.add(Text.of(TextColors.AQUA, "Click to play!"));
        playerHead.offer(Keys.ITEM_LORE, lore);

        int offset = Math.floorDiv(bets.size(), 9);
        int pos = bets.size() + 9 + offset;
        mainMenu.setElement(pos, Element.of(playerHead, headAction));
    }


    /**
     * Removes a player from the main page
     * @param plr The player to remove
     */
    public void removePlayer(Player plr)
    {
        bets.remove(plr.getUniqueId());
        update();
    }


    /**
     * Updates the main page
     */
    private void update()
    {
        Map<UUID, Integer> newBets = bets;
        bets.clear();
        reset();
        for (UUID uuid : newBets.keySet()) {
            if(Sponge.getServer().getPlayer(uuid).isPresent())
                addPlayer(Sponge.getServer().getPlayer(uuid).get(), newBets.get(uuid));
        }
    }


    /**
     * Checks if a user has an active coinflip
     * @param plr The player to check
     * @return whether the player has an active bet or not
     */
    public static boolean hasActiveBet(Player plr)
    {
        return bets.containsKey(plr.getUniqueId());
    }
}
