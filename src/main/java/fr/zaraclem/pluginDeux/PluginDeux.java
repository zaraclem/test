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
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PluginDeux extends JavaPlugin implements Listener {

    private BedWars bedWarsAPI;
    private static final String TWIST_MENU_TITLE = "Choisissez un Twist";
    private Map<IArena, String> activeTwists = new HashMap<>();
    private Map<IArena, Boolean> sharedInventories = new HashMap<>();
    private Map<IArena, Boolean> sharedHealth = new HashMap<>();
    private Map<IArena, Integer> playersHealth = new HashMap<>();

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

            if (arena != null && (arena.getStatus() == GameState.waiting || arena.getStatus() == GameState.starting)) {
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

            String twistName = clickedItem.getItemMeta().getDisplayName();
            boolean isActivated = activeTwists.containsKey(arena) && activeTwists.get(arena).equals(twistName);

            // Gestion de l'activation/désactivation des twists
            if (isActivated) {
                deactivateTwist(arena, twistName);
                player.sendMessage("§cDésactivation de " + twistName);
                ItemMeta meta = clickedItem.getItemMeta();
                meta.removeEnchant(Enchantment.DURABILITY);
                clickedItem.setItemMeta(meta);
            } else {
                activateTwist(arena, twistName, player);
                player.sendMessage("§aActivation de " + twistName);
                ItemMeta meta = clickedItem.getItemMeta();
                meta.addEnchant(Enchantment.DAMAGE_ALL, 200, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                clickedItem.setItemMeta(meta);
            }
            player.updateInventory();
        }
    }

    private void activateTwist(IArena arena, String twistName, Player player) {
        deactivateTwist(arena, activeTwists.get(arena)); // désactive le twist précédent
        activeTwists.put(arena, twistName);

        switch (twistName) {
            case "1 Cœur":
                setOneHeartTwist(arena, player);
                break;
            case "Vie Partagée":
                enableSharedHealth(arena);
                break;
            case "Inventaire Partagé":
                enableSharedInventory(arena);
                break;
        }
    }

    private void setOneHeartTwist(IArena arena, Player player) {
        for (Player p : arena.getPlayers()) {
            p.setHealth(1.0); // Mettre tous les joueurs à 1 cœur
        }
        player.sendMessage("§aTous les joueurs ont maintenant 1 cœur !");
    }

    private void deactivateTwist(IArena arena, String twistName) {
        if (twistName == null) return;

        switch (twistName) {
            case "1 Cœur":
                resetHealth(arena);
                break;
            case "Vie Partagée":
                sharedHealth.remove(arena);
                break;
            case "Inventaire Partagé":
                sharedInventories.remove(arena);
                break;
        }
        activeTwists.remove(arena);
    }

    private void resetHealth(IArena arena) {
        for (Player p : arena.getPlayers()) {
            p.setHealth(p.getMaxHealth()); // Réinitialiser la santé des joueurs
        }
        // Ne pas envoyer de message ici, car cela doit être uniquement pour le joueur qui a activé le twist
    }

    private void enableSharedHealth(IArena arena) {
        sharedHealth.put(arena, true);
        for (Player player : arena.getPlayers()) {
            playersHealth.put(arena, (int) player.getHealth());
        }
        // Ne pas envoyer de message ici, car cela doit être uniquement pour le joueur qui a activé le twist
    }

    private void enableSharedInventory(IArena arena) {
        sharedInventories.put(arena, true);
        // Ne pas envoyer de message ici, car cela doit être uniquement pour le joueur qui a activé le twist
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        IArena arena = bedWarsAPI.getArenaUtil().getArenaByPlayer(player);
        if (arena != null && sharedHealth.getOrDefault(arena, false)) {
            Integer health = playersHealth.get(arena);
            if (health != null) {
                player.setHealth(health);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        IArena arena = bedWarsAPI.getArenaUtil().getArenaByPlayer(player);
        if (arena != null && sharedHealth.getOrDefault(arena, false)) {
            playersHealth.remove(arena);
        }
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        IArena arena = bedWarsAPI.getArenaUtil().getArenaByPlayer(player);

        if (arena != null && sharedInventories.getOrDefault(arena, false)) {
            ItemStack pickedItem = event.getItem().getItemStack();
            for (Player p : arena.getPlayers()) {
                if (p != player) {
                    p.getInventory().addItem(pickedItem.clone());
                }
            }
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        IArena arena = bedWarsAPI.getArenaUtil().getArenaByPlayer(player);

        if (arena != null && sharedInventories.getOrDefault(arena, false)) {
            ItemStack droppedItem = event.getItemDrop().getItemStack();
            for (Player p : arena.getPlayers()) {
                if (p != player) {
                    p.getInventory().addItem(droppedItem.clone());
                }
            }
            event.getItemDrop().remove();
        }
    }
}