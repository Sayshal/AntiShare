package com.turt2live.antishare.regions;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.turt2live.antishare.ASUtils;
import com.turt2live.antishare.AntiShare;
import com.turt2live.antishare.inventory.ASInventory;
import com.turt2live.antishare.inventory.ASInventory.InventoryType;
import com.turt2live.antishare.regions.RegionKey.RegionKeyType;

/**
 * Creates, edits, and removes regions
 * 
 * @author turt2live
 */
public class RegionFactory {

	/**
	 * Inner class to safely use WorldEdit
	 * 
	 * @author turt2live
	 */
	private class WorldEditHook {

		private WorldEditPlugin plugin;

		/**
		 * Creates a new WorldEdit Hook
		 * 
		 * @param hasWorldEdit used for hook handling
		 */
		public WorldEditHook(boolean hasWorldEdit){
			if(hasWorldEdit){
				plugin = (WorldEditPlugin) AntiShare.getInstance().getServer().getPluginManager().getPlugin("WorldEdit");
			}
		}

		/**
		 * Gets a WorldEdit selection from a player
		 * 
		 * @param player the player
		 * @return the selection
		 */
		public Selection getSelection(Player player){
			return plugin.getSelection(player);
		}

		/**
		 * Determines if a player has a "complete" selection
		 * 
		 * @param player the player
		 * @return true if a complete selection is present
		 */
		public boolean hasSelection(Player player){
			Selection selection = getSelection(player);
			if(selection == null){
				return false;
			}
			return selection.getMaximumPoint() != null && selection.getMinimumPoint() != null;
		}

		/**
		 * Determines if a region exists in a Selection
		 * 
		 * @param selection the selection
		 * @return true if a region is there
		 */
		public boolean isRegionInSelection(Selection selection){
			for(ASRegion key : AntiShare.getInstance().getRegionManager().getAllRegions(selection.getWorld())){
				Selection region = key.getSelection();
				// Thanks to Sleaker for letting me use this code :D
				// Modified from: https://github.com/MilkBowl/LocalShops/blob/master/src/net/milkbowl/localshops/ShopManager.java#L216
				if(selection.getMaximumPoint().getBlockX() < region.getMinimumPoint().getBlockX()
						|| selection.getMinimumPoint().getBlockX() > region.getMaximumPoint().getBlockX()){
					continue;
				}else if(selection.getMaximumPoint().getBlockZ() < region.getMinimumPoint().getBlockZ()
						|| selection.getMinimumPoint().getBlockZ() > region.getMaximumPoint().getBlockZ()){
					continue;
				}else if(selection.getMaximumPoint().getBlockY() < region.getMinimumPoint().getBlockY()
						|| selection.getMinimumPoint().getBlockY() > region.getMaximumPoint().getBlockY()){
					continue;
				}else{
					return true; // All 3 planes meet, therefore regions are in contact
				}
			}
			return false; // No region
		}

		/**
		 * Determines if a region exists in a Selection and is not a specified region
		 * 
		 * @param selection the selection
		 * @param region the region to compare against
		 * @return true if a region is there
		 */
		public boolean isRegionInSelectionAndNot(Selection selection, ASRegion region){
			for(ASRegion key : AntiShare.getInstance().getRegionManager().getAllRegions(selection.getWorld())){
				if(key.getUniqueID().equals(region.getUniqueID())){
					continue;
				}
				Selection regionSelection = key.getSelection();
				// Thanks to Sleaker for letting me use this code :D
				// Modified from: https://github.com/MilkBowl/LocalShops/blob/master/src/net/milkbowl/localshops/ShopManager.java#L216
				if(selection.getMaximumPoint().getBlockX() < regionSelection.getMinimumPoint().getBlockX()
						|| selection.getMinimumPoint().getBlockX() > regionSelection.getMaximumPoint().getBlockX()){
					continue;
				}else if(selection.getMaximumPoint().getBlockZ() < regionSelection.getMinimumPoint().getBlockZ()
						|| selection.getMinimumPoint().getBlockZ() > regionSelection.getMaximumPoint().getBlockZ()){
					continue;
				}else if(selection.getMaximumPoint().getBlockY() < regionSelection.getMinimumPoint().getBlockY()
						|| selection.getMinimumPoint().getBlockY() > regionSelection.getMaximumPoint().getBlockY()){
					continue;
				}else{
					return true; // All 3 planes meet, therefore regions are in contact
				}
			}
			return false; // No region
		}

	}

	private RegionManager regions;
	private WorldEditHook hook;

	/**
	 * Creates a new region factory
	 */
	public RegionFactory(){
		regions = AntiShare.getInstance().getRegionManager();
		hook = new WorldEditHook(regions.hasWorldEdit());
	}

	/**
	 * Removes a region by name
	 * 
	 * @param sender the CommandSender removing the region
	 * @param name the region name
	 */
	public void removeRegionByName(CommandSender sender, String name){
		if(!regions.regionNameExists(name)){
			ASUtils.sendToPlayer(sender, ChatColor.RED + "No region has the name " + ChatColor.DARK_RED + name);
			return;
		}
		regions.removeRegion(name);
		ASUtils.sendToPlayer(sender, ChatColor.GREEN + "Region removed.");
	}

	/**
	 * Removes a region by a point in the region
	 * 
	 * @param sender the CommandSender removing the region
	 * @param location the point
	 */
	public void removeRegionByLocation(CommandSender sender, Location location){
		if(!regions.isRegion(location)){
			ASUtils.sendToPlayer(sender, ChatColor.RED + "There is no region there");
			return;
		}
		regions.removeRegion(location);
		ASUtils.sendToPlayer(sender, ChatColor.GREEN + "Region removed.");
	}

	/**
	 * Edits a region
	 * 
	 * @param region the region to edit
	 * @param key the key
	 * @param value the value
	 * @param sender the command sender
	 */
	public void editRegion(ASRegion region, RegionKeyType key, String value, CommandSender sender){
		if(!AntiShare.getInstance().getRegionManager().hasWorldEdit()){
			ASUtils.sendToPlayer(sender, ChatColor.RED + "WorldEdit is not installed.");
			return;
		}

		// Handle key
		boolean changed = false;
		switch (key){
		case NAME:
			if(AntiShare.getInstance().getRegionManager().regionNameExists(value)){
				ASUtils.sendToPlayer(sender, ChatColor.RED + "Region name '" + value + "' already exists!");
			}else{
				region.setName(value);
				changed = true;
			}
			break;
		case ENTER_MESSAGE_SHOW:
			if(ASUtils.getBoolean(value) != null){
				region.setMessageOptions(ASUtils.getBoolean(value), region.isExitMessageActive());
				changed = true;
			}else{
				ASUtils.sendToPlayer(sender, ChatColor.RED + "Value '" + value + "' is unknown, did you mean 'true' or 'false'?");
			}
			break;
		case EXIT_MESSAGE_SHOW:
			if(ASUtils.getBoolean(value) != null){
				region.setMessageOptions(region.isEnterMessageActive(), ASUtils.getBoolean(value));
				changed = true;
			}else{
				ASUtils.sendToPlayer(sender, ChatColor.RED + "Value '" + value + "' is unknown, did you mean 'true' or 'false'?");
			}
			break;
		case INVENTORY:
			if(value.equalsIgnoreCase("none")){
				region.setInventory(null);
				changed = true;
			}else if(value.equalsIgnoreCase("set")){
				if(sender instanceof Player){
					region.setInventory(ASInventory.generate((Player) sender, InventoryType.REGION));
					changed = true;
				}else{
					ASUtils.sendToPlayer(sender, ChatColor.RED + "You can't set an inventory from the console, only clear.");
				}
			}else{
				ASUtils.sendToPlayer(sender, ChatColor.RED + "Value '" + value + "' is unknown to me, did you mean 'none' or 'set'?");
			}
			break;
		case SELECTION_AREA:
			if(!AntiShare.getInstance().getRegionManager().hasWorldEdit()){
				ASUtils.sendToPlayer(sender, ChatColor.DARK_RED + "WorldEdit is not installed. No region set.");
				break;
			}
			if(!(sender instanceof Player)){
				ASUtils.sendToPlayer(sender, ChatColor.DARK_RED + "You are not a player, sorry!");
				break;
			}
			if(!hook.hasSelection((Player) sender)){
				ASUtils.sendToPlayer(sender, ChatColor.DARK_RED + "You do not have a selection!");
				break;
			}
			if(hook.isRegionInSelectionAndNot(hook.getSelection((Player) sender), region)){
				ASUtils.sendToPlayer(sender, ChatColor.DARK_RED + "There is a region where you have selected!");
				break;
			}
			Selection selection = hook.getSelection((Player) sender);
			region.setRegion(selection);
			changed = true;
			break;
		case GAMEMODE:
			if(value.equalsIgnoreCase("creative") || value.equalsIgnoreCase("c") || value.equalsIgnoreCase("1")){
				region.setGameMode(GameMode.CREATIVE);
				changed = true;
			}else if(value.equalsIgnoreCase("survival") || value.equalsIgnoreCase("s") || value.equalsIgnoreCase("0")){
				region.setGameMode(GameMode.SURVIVAL);
				changed = true;
			}else{
				ASUtils.sendToPlayer(sender, ChatColor.RED + "I don't know what Game Mode '" + value + "' is!");
			}
			break;
		case ENTER_MESSAGE:
			region.setEnterMessage(value);
			changed = true;
			break;
		case EXIT_MESSAGE:
			region.setExitMessage(value);
			changed = true;
			break;
		}
		if(changed){
			ASUtils.sendToPlayer(sender, ChatColor.GREEN + "Region saved.");
		}
	}

	/**
	 * Adds a region
	 * 
	 * @param player the player creating the region
	 * @param gamemodeStr the gamemode string
	 * @param name the region name
	 */
	public void addRegion(Player player, String gamemodeStr, String name){
		if(!regions.hasWorldEdit()){
			ASUtils.sendToPlayer(player, ChatColor.RED + "You don't have WorldEdit!");
			return;
		}

		// Check conditions
		GameMode gamemode = ASUtils.getGameMode(gamemodeStr);
		if(gamemode == null){
			ASUtils.sendToPlayer(player, ChatColor.RED + "I don't know what gamemode '" + gamemodeStr + "' is, sorry!");
			return;
		}
		if(!hook.hasSelection(player)){
			ASUtils.sendToPlayer(player, ChatColor.RED + "You don't have a valid WorldEdit selection!");
			return;
		}
		Selection selection = hook.getSelection(player);
		if(hook.isRegionInSelection(selection)){
			ASUtils.sendToPlayer(player, ChatColor.RED + "Another region intersects your selection!");
			return;
		}
		if(regions.regionNameExists(name)){
			ASUtils.sendToPlayer(player, ChatColor.RED + "Another region already has that name!");
			return;
		}

		// Add the region
		regions.addRegion(new com.turt2live.antishare.Selection(selection), player.getName(), name, gamemode);
		ASUtils.sendToPlayer(player, ChatColor.GREEN + "Region '" + name + "' added.");
	}

}
