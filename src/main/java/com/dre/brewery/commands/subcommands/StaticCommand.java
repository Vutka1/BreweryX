package com.dre.brewery.commands.subcommands;

import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.events.brew.BrewModifyEvent;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class StaticCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        ItemStack hand = player.getItemInHand();
        if (hand.getType() != Material.AIR) {
            Brew brew = Brew.get(hand);
            if (brew != null) {
                if (brew.isStatic()) {
                    if (!brew.isStripped()) {
                        brew.setStatic(false, hand);
                        lang.sendEntry(sender, "CMD_NonStatic");
                    } else {
                        lang.sendEntry(sender, "Error_SealedAlwaysStatic");
                        return;
                    }
                } else {
                    brew.setStatic(true, hand);
                    lang.sendEntry(sender, "CMD_Static");
                }
                brew.touch();
                ItemMeta meta = hand.getItemMeta();
                assert meta != null;
                BrewModifyEvent modifyEvent = new BrewModifyEvent(brew, meta, BrewModifyEvent.Type.STATIC);
                BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(modifyEvent);
				if (brew != modifyEvent.getBrew()) brew = modifyEvent.getBrew();
                if (modifyEvent.isCancelled()) {
                    return;
                }
                brew.save(meta);
                hand.setItemMeta(meta);
                return;
            }
        }
        lang.sendEntry(sender, "Error_ItemNotPotion");
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.static";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
