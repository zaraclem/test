package fr.zaraclem.pluginDeux;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PluginDeux extends JavaPlugin implements Listener {

    private BedWars bedWarsAPI;
    private static final String TWIST_MENU_TITLE = "Choisissez un Twist";
    private Map<IArena, ItemStack> activeTwists = new HashMap<>(); // Twist actif par arène

    @Override
    public void onEnable() {
        bedWarsAPI = Bukkit.getServicesManager().getRegistration(BedWars.class).getProvider();
        getCommand("twist").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("twist") && sender instanceof Player) {
            Player player = (Player) sender;

            // Vérifiez si le joueur est un administrateur
            if (!player.isOp()) {
                player.sendMessage("§cSeuls les administrateurs peuvent utiliser cette commande.");
                return true;
            }

            IArena arena = bedWarsAPI.getArenaUtil().getArenaByPlayer(player);

            if (arena != null && arena.getStatus() == GameState.waiting) {
                openTwistMenu(player);
            } else {
                player.sendMessage("§cVous devez être dans une zone d'attente pour choisir un twist.");
            }
            return true;
        }
        return false;
    }

    private void openTwistMenu(Player player) {
        Inventory twistMenu = Bukkit.createInventory(null, 9, TWIST_MENU_TITLE);



        ItemStack oneHeart = createItem(Material.REDSTONE, "1 Cœur", "Tous les joueurs auront 1 cœur.");
        ItemStack sharedHealthItem = createItem(Material.GOLDEN_APPLE, "Vie Partagée", "Tous les joueurs partagent leur vie.");
        ItemStack sharedInventory = createItem(Material.CHEST, "Inventaire Partagé", "Tous les joueurs partagent leur inventaire.");

        twistMenu.setItem(2, oneHeart);
        twistMenu.setItem(4, sharedHealthItem);
        twistMenu.setItem(6, sharedInventory);

        player.openInventory(twistMenu);
    }

    private ItemStack createItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(TWIST_MENU_TITLE)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            IArena arena = bedWarsAPI.getArenaUtil().getArenaByPlayer(player);
            if (arena == null) return;

            // Vérifie si l'item est déjà sélectionné
            ItemStack activeTwist = activeTwists.get(arena);
            if (activeTwist != null && activeTwist.isSimilar(clickedItem)) {
                // Désactivation si déjà activé
                activeTwists.remove(arena);
                clickedItem.removeEnchantment(Enchantment.DAMAGE_ALL);
                player.sendMessage("§cDésactivation de " + clickedItem.getItemMeta().getDisplayName() + " !");
            } else {
                // Désactive le twist précédent, s'il y en a un
                if (activeTwist != null) {
                    activeTwist.removeEnchantment(Enchantment.DAMAGE_ALL);
                    player.sendMessage("§cDésactivation de " + activeTwist.getItemMeta().getDisplayName() + " !");
                }

                // Active le nouveau twist
                activeTwists.put(arena, clickedItem);
                clickedItem.addEnchantment(Enchantment.DAMAGE_ALL, 200);

                player.sendMessage("§aActivation de " + clickedItem.getItemMeta().getDisplayName() + " !");
            }

            player.closeInventory();
        }
    }
}
