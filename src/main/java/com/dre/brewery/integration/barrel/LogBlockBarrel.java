package com.dre.brewery.integration.barrel;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.LegacyUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.util.BukkitUtils;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.util.BukkitUtils.compareInventories;
import static de.diddiz.util.BukkitUtils.compressInventory;

@SuppressWarnings("JavaReflectionMemberAccess")
public class LogBlockBarrel {

	private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
	private static final List<LogBlockBarrel> opened = new ArrayList<>();

	public static Consumer consumer = LogBlock.getInstance().getConsumer();
	private static Method rawData;
	private static Method queueChestAccess;

	static {
		if (VERSION.isOrEarlier(MinecraftVersion.V1_13)) {
			try {
				rawData = BukkitUtils.class.getDeclaredMethod("rawData", ItemStack.class);
				queueChestAccess = Consumer.class.getDeclaredMethod("queueChestAccess", String.class, Location.class, int.class, short.class, short.class, short.class);
			} catch (NoSuchMethodException e) {
				Logging.errorLog("Failed to hook into LogBlock to log barrels. Logging barrel contents is not going to work.", e);
				Logging.errorLog("Brewery was tested with version 1.12 to 1.13.1 of LogBlock.");
				Logging.errorLog("Disable LogBlock support in the configuration file and type /brew reload.");
			}
		}
	}

	private HumanEntity player;
	private ItemStack[] items;
	private Location loc;

	public LogBlockBarrel(HumanEntity player, ItemStack[] items, Location spigotLoc) {
		this.player = player;
		this.items = items;
		this.loc = spigotLoc;
		opened.add(this);
	}

	private void compareInv(final ItemStack[] after) {
		if (consumer == null) {
			return;
		}
		final ItemStack[] diff = compareInventories(items, after);
		for (final ItemStack item : diff) {
			if (VERSION.isOrEarlier(MinecraftVersion.V1_13)) {
				try {
					//noinspection deprecation
					queueChestAccess.invoke(consumer, player.getName(), loc, LegacyUtil.getBlockTypeIdAt(loc), (short) item.getType().getId(), (short) item.getAmount(), rawData.invoke(null, item));
				} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					Logging.errorLog("Failed to log barrel access.", e);
				}
			} else {
				ItemStack i2 = item;
				if (item.getAmount() < 0) {
					i2 = item.clone();
					i2.setAmount(Math.abs(item.getAmount()));
				}
				consumer.queueChestAccess(Actor.actorFromEntity(player), loc, loc.getBlock().getBlockData(), i2, item.getAmount() < 0);
			}
		}
	}

	public static LogBlockBarrel get(HumanEntity player) {
		for (LogBlockBarrel open : opened) {
			if (open.player.equals(player)) {
				return open;
			}
		}
		return null;
	}

	public static void openBarrel(HumanEntity player, Inventory inv, Location spigotLoc) {
		if (!isLogging(player.getWorld(), de.diddiz.LogBlock.Logging.CHESTACCESS)) return;
		new LogBlockBarrel(player, compressInventory(inv.getContents()), spigotLoc);
	}

	public static void closeBarrel(HumanEntity player, Inventory inv) {
		if (!isLogging(player.getWorld(), de.diddiz.LogBlock.Logging.CHESTACCESS)) return;
		LogBlockBarrel open = get(player);
		if (open != null) {
			open.compareInv(compressInventory(inv.getContents()));
			opened.remove(open);
		}
	}

	public static void breakBarrel(Player player, ItemStack[] contents, Location spigotLoc) {
		if (consumer == null) {
			return;
		}
		if (!isLogging(spigotLoc.getWorld(), de.diddiz.LogBlock.Logging.CHESTACCESS)) return;
		final ItemStack[] items = compressInventory(contents);
		for (final ItemStack item : items) {
			if (VERSION.isOrEarlier(MinecraftVersion.V1_13)) {
				try {
					//noinspection deprecation
					queueChestAccess.invoke(consumer, player.getName(), spigotLoc, LegacyUtil.getBlockTypeIdAt(spigotLoc), (short) item.getType().getId(), (short) (item.getAmount() * -1), rawData.invoke(null, item));
				} catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					Logging.errorLog("Failed to log barrel break.", e);
				}
			} else {
				consumer.queueChestAccess(Actor.actorFromEntity(player), spigotLoc, spigotLoc.getBlock().getBlockData(), item, false);
			}
		}
	}

	public static void clear() {
		opened.clear();
	}
}
