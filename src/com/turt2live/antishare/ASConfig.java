package com.turt2live.antishare;

import java.io.File;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.feildmaster.lib.configuration.EnhancedConfiguration;

public class ASConfig {

	private AntiShare plugin;

	public ASConfig(AntiShare plugin){
		this.plugin = plugin;
	}

	public void create(){
		plugin.getConfig().loadDefaults(plugin.getResource("resources/config.yml"));
		if(!plugin.getConfig().fileExists() || !plugin.getConfig().checkDefaults()){
			plugin.getConfig().saveDefaults();
		}
		load();
	}

	public boolean onlyIfCreative(Player player){
		if(player.hasPermission("AntiShare.onlyIfCreative.global")){
			return getBoolean("other.only_if_creative", player.getWorld());
		}else if(player.hasPermission("AntiShare.onlyIfCreative")){
			return true;
		}
		return false;
	}

	public Object get(String path, World world){
		File worldConfig = new File(plugin.getDataFolder(), world.getName() + "_config.yml");
		EnhancedConfiguration config = new EnhancedConfiguration(worldConfig, plugin);
		Object value = null;
		if(!config.getString(path).equalsIgnoreCase("global")){
			value = config.get(path);
		}else{
			value = plugin.getConfig().get(path);
		}
		return value;
	}

	public boolean getBoolean(String path, World world){
		File worldConfig = new File(plugin.getDataFolder(), world.getName() + "_config.yml");
		EnhancedConfiguration config = new EnhancedConfiguration(worldConfig, plugin);
		boolean value = false;
		if(!config.getString(path).equalsIgnoreCase("global")){
			value = config.getBoolean(path);
		}else{
			value = plugin.getConfig().getBoolean(path);
		}
		return value;
	}

	public String getString(String path, World world){
		File worldConfig = new File(plugin.getDataFolder(), world.getName() + "_config.yml");
		EnhancedConfiguration config = new EnhancedConfiguration(worldConfig, plugin);
		String value = null;
		if(!config.getString(path).equalsIgnoreCase("global")){
			value = config.getString(path);
		}else{
			value = plugin.getConfig().getString(path);
		}
		return value;
	}

	public void reload(){
		load();
	}

	public void save(){
		plugin.saveConfig();
	}

	public void load(){
		plugin.reloadConfig();
	}
}
