package com.turt2live.antishare.money;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.turt2live.antishare.ASUtils;
import com.turt2live.antishare.AntiShare.LogType;
import com.turt2live.antishare.api.ASGameMode;
import com.turt2live.antishare.metrics.TrackerList.TrackerType;
import com.turt2live.antishare.permissions.PermissionNodes;

/**
 * Reward for doing something
 * 
 * @author turt2live
 */
public class Reward extends Tender {

	/**
	 * Creates a new reward
	 * 
	 * @param type the type
	 * @param amount the amount (positive to add to account)
	 * @param enabled true to enable
	 * @param affect the Game Mode(s) to affect
	 */
	public Reward(TenderType type, double amount, boolean enabled, ASGameMode affect){
		super(type, amount, enabled, affect);
	}

	@Override
	public void apply(Player player){
		if(!isEnabled() || plugin.getPermissions().has(player, PermissionNodes.MONEY_NO_REWARD) || !super.affect(player.getGameMode())){
			return;
		}

		// Apply to account
		TransactionResult result = plugin.getMoneyManager().addToAccount(player, getAmount());
		if(!result.completed){
			ASUtils.sendToPlayer(player, ChatColor.RED + "Reward Failed: " + ChatColor.ITALIC + result.message);
			plugin.getMessenger().log("Reward Failed (" + player.getName() + "): " + result.message, Level.WARNING, LogType.BYPASS);
			return;
		}else{
			String formatted = plugin.getMoneyManager().formatAmount(getAmount());
			String balance = plugin.getMoneyManager().formatAmount(plugin.getMoneyManager().getBalance(player));
			if(!plugin.getMoneyManager().isSilent(player.getName())){
				ASUtils.sendToPlayer(player, ChatColor.GREEN + "You've been rewarded " + formatted + "!");
				ASUtils.sendToPlayer(player, "Your new balance is " + ChatColor.YELLOW + balance);
			}
		}

		// Increment statistic
		plugin.getTrackers().getTracker(TrackerType.REWARD_GIVEN).increment(1); // Does not have a name!
	}

}
