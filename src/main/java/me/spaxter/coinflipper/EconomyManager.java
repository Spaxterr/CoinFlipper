package me.spaxter.coinflipper;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;

import java.math.BigDecimal;
import java.util.Optional;

public class EconomyManager {

    private final EconomyService economyService;
    private final CoinFlipper plugin;

    public EconomyManager(CoinFlipper plugin)
    {
        this.plugin = plugin;
        this.economyService = plugin.economyService;
    }

    /**
     * Check if a player has an amount of money
     * @param plr The player to check
     * @param amount The amount to check
     * @return True if the player has more than or equal to the amount, otherwise False
     */
    public boolean hasMoney(Player plr, int amount)
    {
        Optional<UniqueAccount> acc = economyService.getOrCreateAccount(plr.getUniqueId());
        return acc.filter(uniqueAccount -> uniqueAccount.getBalance(economyService.getDefaultCurrency()).compareTo(BigDecimal.valueOf(amount)) >= 0).isPresent();
    }

    /**
     * Transfer money from one player to the other
     * @param from The player to take from
     * @param to The player to transfer the money to
     * @param amount The amount of money to transfer
     */
    public void transferMoney(Player from, Player to, int amount)
    {
        Optional<UniqueAccount> oAcc1 = economyService.getOrCreateAccount(from.getUniqueId());
        Optional<UniqueAccount> oAcc2 = economyService.getOrCreateAccount(to.getUniqueId());
        if (oAcc1.isPresent() && oAcc2.isPresent()) {
            UniqueAccount accFrom = oAcc1.get();
            UniqueAccount accTo = oAcc2.get();
            BigDecimal money = BigDecimal.valueOf(amount);
            accFrom.transfer(accTo, economyService.getDefaultCurrency(), money, Cause.of(EventContext.builder().
                    add(EventContextKeys.PLUGIN, plugin.pluginContainer).build(), plugin));
        }
    }
}
