package com.turt2live.antishare.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import com.turt2live.antishare.ASUtils;
import com.turt2live.antishare.AntiShare;
import com.turt2live.antishare.Notification;
import com.turt2live.antishare.debug.Bug;
import com.turt2live.antishare.debug.Debugger;
import com.turt2live.antishare.enums.NotificationType;

public class EntityListener implements Listener {

	private AntiShare plugin;

	public EntityListener(AntiShare plugin){
		this.plugin = plugin;
	}

	@EventHandler (priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageByEntityEvent event){
		try{
			String entityName = event.getEntity().getClass().getName().replace("Craft", "").replace("org.bukkit.craftbukkit.entity.", "");
			Entity damager = event.getDamager();
			if(event.getEntity() instanceof Player){
				entityName = ((Player) event.getEntity()).getName();
				if(damager instanceof Projectile){
					if(((Projectile) damager).getShooter() instanceof Player){
						Player player = (Player) ((Projectile) damager).getShooter();
						if(!plugin.config().getBoolean("other.pvp", player.getWorld())){
							if(plugin.isBlocked(player, "AntiShare.allow.pvp", player.getWorld())){
								Notification.sendNotification(NotificationType.ILLEGAL_PLAYER_PVP, plugin, player, entityName, null);
								ASUtils.sendToPlayer(player, plugin.config().getString("messages.pvp", event.getEntity().getWorld()));
								event.setCancelled(true);
							}else{
								Notification.sendNotification(NotificationType.LEGAL_PLAYER_PVP, plugin, player, entityName, null);
							}
						}
					}
				}else if(damager instanceof Player){
					Player player = (Player) damager;
					if(!plugin.config().getBoolean("other.pvp", player.getWorld())){
						if(plugin.isBlocked(player, "AntiShare.allow.pvp", player.getWorld())){
							Notification.sendNotification(NotificationType.ILLEGAL_PLAYER_PVP, plugin, player, entityName, null);
							ASUtils.sendToPlayer(player, plugin.config().getString("messages.pvp", event.getEntity().getWorld()));
							event.setCancelled(true);
						}else{
							Notification.sendNotification(NotificationType.LEGAL_PLAYER_PVP, plugin, player, entityName, null);
						}
					}
				}
			}else{ //Mob
				if(damager instanceof Projectile){
					if(((Projectile) damager).getShooter() instanceof Player){
						Player player = (Player) ((Projectile) damager).getShooter();
						if(!plugin.config().getBoolean("other.pvp-mobs", player.getWorld())){
							if(plugin.isBlocked(player, "AntiShare.allow.mobpvp", player.getWorld())){
								Notification.sendNotification(NotificationType.ILLEGAL_MOB_PVP, plugin, player, entityName, null);
								ASUtils.sendToPlayer(player, plugin.config().getString("messages.mobpvp", event.getEntity().getWorld()));
								event.setCancelled(true);
							}else{
								Notification.sendNotification(NotificationType.LEGAL_MOB_PVP, plugin, player, entityName, null);
							}
						}
					}
				}else if(damager instanceof Player){
					Player player = (Player) damager;
					if(!plugin.config().getBoolean("other.pvp-mobs", damager.getWorld())){
						if(plugin.isBlocked(player, "AntiShare.allow.mobpvp", player.getWorld())){
							Notification.sendNotification(NotificationType.ILLEGAL_MOB_PVP, plugin, player, entityName, null);
							ASUtils.sendToPlayer(player, plugin.config().getString("messages.mobpvp", event.getEntity().getWorld()));
							event.setCancelled(true);
						}else{
							Notification.sendNotification(NotificationType.LEGAL_MOB_PVP, plugin, player, entityName, null);
						}
					}
				}
			}
		}catch(Exception e){
			Bug bug = new Bug(e, e.getMessage(), this.getClass(), null);
			Debugger.sendBug(bug);
		}
	}

	@EventHandler (priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event){
		if(!(event.getTarget() instanceof Player)){
			return;
		}
		try{
			Player player = (Player) event.getTarget();
			if(!plugin.config().getBoolean("other.pvp-mobs", player.getWorld())){
				if(plugin.isBlocked(player, "AntiShare.allow.mobpvp", player.getWorld())){
					event.setCancelled(true);
				}
			}
		}catch(Exception e){
			Bug bug = new Bug(e, e.getMessage(), this.getClass(), (Player) event.getTarget());
			Debugger.sendBug(bug);
		}
	}
}
