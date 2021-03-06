package com.turt2live.antishare.notification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.feildmaster.lib.configuration.EnhancedConfiguration;
import com.turt2live.antishare.ASUtils;
import com.turt2live.antishare.AntiShare;
import com.turt2live.antishare.metrics.TrackerList.TrackerType;
import com.turt2live.antishare.permissions.PermissionNodes;

/**
 * Alerts people
 * 
 * @author turt2live
 */
public class Alert {

	/**
	 * An enum to represent what type of alert is being sent
	 * 
	 * @author turt2live
	 */
	public static enum AlertType{
		ILLEGAL,
		LEGAL,
		GENERAL,
		REGION;
	}

	/**
	 * An enum to better determine what alerts to send based on configuration
	 * 
	 * @author turt2live
	 */
	public static enum AlertTrigger{
		BLOCK_BREAK("types.block-break", "BLOCK_BREAK"),
		BLOCK_PLACE("types.block-place", "BLOCK_PLACE"),
		PLAYER_DEATH("types.player-death", "DEATH"),
		ITEM_DROP("types.item-drop", "DROP"),
		ITEM_PICKUP("types.item-pickup", "PICKUP"),
		RIGHT_CLICK("types.right-click", "RIGHT_CLICK"),
		USE_ITEM("types.use-item", "USE"),
		CREATIVE_BLOCK("types.creative-block-break", "CREATIVE_BLOCK"),
		SURVIVAL_BLOCK("types.survival-block-break", "SURVIVAL_BLOCK"),
		HIT_PLAYER("types.hit-player", "HIT_PLAYER"),
		HIT_MOB("types.hit-mob", "HIT_MOB"),
		COMMAND("types.command", "COMMAND"),
		GENERAL("send-general-notifications", null),
		CLOSE_TO_WORLD_SPLIT(null, null);

		private String node;
		private String tracker;

		private AlertTrigger(String node, String tracker){
			this.node = node;
			this.tracker = tracker;
		}

		/**
		 * Determines whether or not to show this alert
		 * 
		 * @return true if it should be shown
		 */
		public boolean show(){
			if(node == null){
				return false;
			}
			EnhancedConfiguration notifications = new EnhancedConfiguration(new File(AntiShare.getInstance().getDataFolder(), "notifications.yml"), AntiShare.getInstance());
			notifications.loadDefaults(AntiShare.getInstance().getResource("resources/notifications.yml"));
			if(!notifications.fileExists() || !notifications.checkDefaults()){
				notifications.saveDefaults();
			}
			notifications.load();
			return notifications.getBoolean(node);
		}

		/**
		 * Gets the tracker for this alert
		 * 
		 * @param type the alert type
		 * @return the tracker
		 */
		public TrackerType tracker(AlertType type){
			if(tracker == null){
				return null;
			}
			return TrackerType.valueOf(tracker + "_" + type.name());
		}
	}

	/**
	 * A class to represent alert details
	 * 
	 * @author turt2live
	 */
	private class AlertDetails {
		public long admin_last_sent = 0L;
		public long player_last_sent = 0L;
	}

	private ConcurrentHashMap<String, AlertDetails> alerts = new ConcurrentHashMap<String, AlertDetails>();
	private boolean send = true;
	private boolean toConsole = true;
	private boolean toPlayers = true;
	private boolean sendIllegal = true;
	private boolean sendLegal = false;
	private boolean sendGeneral = false;

	/**
	 * Creates a new Alerter
	 */
	public Alert(){
		reload();
	}

	/**
	 * Sends an alert
	 * 
	 * @param message the message
	 * @param sender the sender
	 * @param playerMessage the player message
	 * @param type the Alert Type
	 * @param trigger the Alert Trigger
	 */
	public void alert(String message, CommandSender sender, String playerMessage, AlertType type, AlertTrigger trigger){
		alert(message, sender, playerMessage, type, trigger, 1000, true);
	}

	/**
	 * Sends an alert
	 * 
	 * @param message the message
	 * @param sender the sender
	 * @param playerMessage the player message
	 * @param type the Alert Type
	 * @param trigger the Alert Trigger
	 * @param reward true to send rewards/fines, false to disable them
	 */
	public void alert(String message, CommandSender sender, String playerMessage, AlertType type, AlertTrigger trigger, boolean reward){
		alert(message, sender, playerMessage, type, trigger, 1000, reward);
	}

	/**
	 * Sends an alert
	 * 
	 * @param message the message
	 * @param sender the sender
	 * @param playerMessage the player message
	 * @param type the Alert Type
	 * @param trigger the Alert Trigger
	 * @param time the time between alerts
	 */
	public void alert(String message, CommandSender sender, String playerMessage, AlertType type, AlertTrigger trigger, long time){
		alert(message, sender, playerMessage, type, trigger, time, true);
	}

	/**
	 * Sends an alert
	 * 
	 * @param message the message
	 * @param sender the sender
	 * @param playerMessage the player message
	 * @param type the Alert Type
	 * @param trigger the Alert Trigger
	 * @param time the time between alerts
	 * @param reward true to send rewards/fines, false to disable them
	 */
	public void alert(String message, CommandSender sender, String playerMessage, AlertType type, AlertTrigger trigger, long time, boolean reward){
		String hashmapKey = type.name() + message + playerMessage + sender.hashCode();
		boolean sendToPlayer = true;
		boolean sendToAdmins = true;

		// Determine if we should even send the message
		if(!send || AntiShare.getInstance().getPermissions().has(sender, PermissionNodes.SILENT_NOTIFICATIONS)){
			if(type != AlertType.REGION){
				return;
			}else{
				sendToAdmins = false;
			}
		}

		// Alert Switch
		switch (type){
		case ILLEGAL:
			if(!sendIllegal){
				sendToAdmins = false;
			}
			break;
		case LEGAL:
			if(!sendLegal){
				sendToAdmins = false;
			}
			break;
		case GENERAL:
			if(!sendGeneral){
				sendToAdmins = false;
			}
			break;
		case REGION:
			if(!sendGeneral){
				sendToAdmins = false;
			}
			break;
		}

		// Check who we need to send to
		AlertDetails details = alerts.get(hashmapKey);
		if(details == null){
			details = new AlertDetails();
			details.admin_last_sent = System.currentTimeMillis();
			details.player_last_sent = System.currentTimeMillis();
		}else{
			long now = System.currentTimeMillis();
			if((now - details.admin_last_sent) < time){
				sendToAdmins = false;
			}
			if((now - details.player_last_sent) < time){
				sendToPlayer = false;
			}
		}

		// Check trigger
		if(sendToAdmins){
			sendToAdmins = trigger.show();
		}

		// Send if needed
		if(sendToPlayer && (type == AlertType.ILLEGAL || type == AlertType.GENERAL || type == AlertType.REGION)){
			details.player_last_sent = System.currentTimeMillis();
			ASUtils.sendToPlayer(sender, playerMessage);
		}
		if(sendToAdmins && toPlayers){
			details.admin_last_sent = System.currentTimeMillis();
			for(Player player : Bukkit.getOnlinePlayers()){
				if(AntiShare.getInstance().getPermissions().has(player, PermissionNodes.GET_NOTIFICATIONS)){
					if(!player.getName().equalsIgnoreCase(sender.getName())){
						ASUtils.sendToPlayer(player, message);
					}
				}
			}
		}
		if(sendToAdmins && toConsole){
			ASUtils.sendToPlayer(Bukkit.getConsoleSender(), "[" + type.name() + "] " + message);
		}

		// Send to tracker
		if(trigger.tracker(type) != null){
			TrackerType tt = trigger.tracker(type);
			AntiShare.getInstance().getTrackers().getTracker(tt).increment(1);
		}

		// Send fine/reward
		if(sender instanceof Player && reward){
			Player player = (Player) sender;
			AntiShare.getInstance().getMoneyManager().fire(trigger, type, player);
		}

		// Reinsert (or insert if not found before) into the hashmap
		alerts.put(hashmapKey, details);
	}

	/**
	 * Reloads the alerter
	 */
	public void reload(){
		// Setup configurations
		EnhancedConfiguration notifications = new EnhancedConfiguration(new File(AntiShare.getInstance().getDataFolder(), "notifications.yml"), AntiShare.getInstance());
		notifications.loadDefaults(AntiShare.getInstance().getResource("resources/notifications.yml"));
		if(!notifications.fileExists() || !notifications.checkDefaults()){
			notifications.saveDefaults();
		}
		notifications.load();

		// Setup settings
		send = notifications.getBoolean("send-notifications");
		toConsole = notifications.getBoolean("send-to-console");
		toPlayers = notifications.getBoolean("send-to-players");
		sendIllegal = notifications.getBoolean("send-illegal-notifications");
		sendLegal = notifications.getBoolean("send-legal-notifications");
		sendGeneral = notifications.getBoolean("send-general-notifications");
	}

	/**
	 * Prints the entire contents of the notifications.yml to the writer
	 * 
	 * @param out the writer
	 * @throws IOException for external handling
	 */
	public void print(BufferedWriter out) throws IOException{
		EnhancedConfiguration notifications = new EnhancedConfiguration(new File(AntiShare.getInstance().getDataFolder(), "notifications.yml"), AntiShare.getInstance());
		notifications.load();
		for(String key : notifications.getKeys(true)){
			out.write(key + ": " + (notifications.getString(key).startsWith("MemorySection") ? "" : notifications.getString(key, "")) + "\r\n");
		}
	}

}
