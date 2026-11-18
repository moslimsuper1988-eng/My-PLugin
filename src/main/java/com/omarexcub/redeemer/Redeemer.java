package com.omarexcub.redeemer;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Redeemer extends JavaPlugin implements Listener, CommandExecutor {

    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ Redeem ✦ " + ChatColor.GOLD + "متجر الجوائز";
    private static final String ITEM_DISPLAY_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "✦ متجر Redeem ✦ " + ChatColor.GRAY + "(اضغط كليك يمين)";

    private final Map<UUID, RewardEntry> awaitingCodeInput = new HashMap<>();
    private File usedCodesFile;
    private FileConfiguration usedCodesConfig;
    private Set<String> usedCodes = new HashSet<>();
    private final Map<String, RewardEntry> rewardRegistry = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("redeem") != null) {
            getCommand("redeem").setExecutor(this);
        }

        setupUsedCodesStorage();
        registerAllRewards();

        getLogger().info("==========================================");
        getLogger().info("  Redeemer v1.0 (Paper 1.21.11) Enabled!  ");
        getLogger().info("  OMAREX NETWORK Store Ready!             ");
        getLogger().info("==========================================");
    }

    @Override
    public void onDisable() {
        saveUsedCodes();
        getLogger().info("Redeemer Plugin Disabled.");
    }

    private void setupUsedCodesStorage() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        usedCodesFile = new File(getDataFolder(), "used_codes.yml");
        if (!usedCodesFile.exists()) {
            try {
                usedCodesFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("تعذر إنشاء ملف used_codes.yml: " + e.getMessage());
            }
        }
        usedCodesConfig = YamlConfiguration.loadConfiguration(usedCodesFile);
        List<String> loaded = usedCodesConfig.getStringList("redeemed-codes");
        usedCodes = new HashSet<>(loaded);
    }

    private void saveUsedCodes() {
        if (usedCodesConfig != null && usedCodesFile != null) {
            usedCodesConfig.set("redeemed-codes", new ArrayList<>(usedCodes));
            try {
                usedCodesConfig.save(usedCodesFile);
            } catch (IOException e) {
                getLogger().severe("تعذر حفظ ملف used_codes.yml: " + e.getMessage());
            }
        }
    }

    private void registerAllRewards() {
        rewardRegistry.put("VIP1", new RewardEntry("VIP1", "رتبة VIP البرونزية", Material.EMERALD, 10, RewardType.RANK, "vip"));
        rewardRegistry.put("VIP2", new RewardEntry("VIP2", "رتبة VIP+ الفضية", Material.EMERALD_BLOCK, 11, RewardType.RANK, "vipplus"));
        rewardRegistry.put("SUPER", new RewardEntry("SUPER", "رتبة SUPER MEMBER", Material.DIAMOND, 12, RewardType.RANK, "super"));
        rewardRegistry.put("ULTRA", new RewardEntry("ULTRA", "رتبة ULTRA LEGEND", Material.AMETHYST_SHARD, 13, RewardType.RANK, "ultra"));
        rewardRegistry.put("MVP", new RewardEntry("MVP", "رتبة MVP الأسطورية", Material.GOLD_INGOT, 14, RewardType.RANK, "mvp"));
        rewardRegistry.put("MVP2", new RewardEntry("MVP2", "رتبة MVP++ الملكية", Material.GOLD_BLOCK, 15, RewardType.RANK, "mvpplus"));
        rewardRegistry.put("MOD", new RewardEntry("MOD", "رتبة MOD الإشرافية", Material.NETHER_STAR, 16, RewardType.RANK, "mod"));

        rewardRegistry.put("WPN1", new RewardEntry("WPN1", "سيف النذرايت Sharpness V + Fire II", Material.NETHERITE_SWORD, 19, RewardType.SWORD_MAX, ""));
        rewardRegistry.put("WPN2", new RewardEntry("WPN2", "سيف الغريب الأسطوري (Zenith Blade)", Material.NETHERITE_SWORD, 20, RewardType.ZENITH_BLADE, ""));
        rewardRegistry.put("WPN3", new RewardEntry("WPN3", "قوس البرق Power V + Infinity", Material.BOW, 21, RewardType.BOW_MAX, ""));

        rewardRegistry.put("ARM1", new RewardEntry("ARM1", "طقم دروع نذرايت Protection IV كامل", Material.NETHERITE_CHESTPLATE, 22, RewardType.ARMOR_SET, ""));
        rewardRegistry.put("ARM7", new RewardEntry("ARM7", "أجنحة الإليترا Elytra + Unbreaking III", Material.ELYTRA, 23, RewardType.ELYTRA_KIT, ""));
        rewardRegistry.put("ARM8", new RewardEntry("ARM8", "معول النذرايت Fortune III + Efficiency V", Material.NETHERITE_PICKAXE, 24, RewardType.PICKAXE_MAX, ""));
        rewardRegistry.put("ARM18", new RewardEntry("ARM18", "توتم الخلود Totem of Undying (x5)", Material.TOTEM_OF_UNDYING, 25, RewardType.TOTEMS, ""));

        rewardRegistry.put("CRT1", new RewardEntry("CRT1", "صندوق ZenithSMP الأسطوري (موارد نادرة)", Material.ENDER_CHEST, 31, RewardType.ZENITH_CRATE, ""));
    }

    public ItemStack createRedeemShopItem() {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ITEM_DISPLAY_NAME);
            meta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "اضغط كليك يمين لفتح متجر " + ChatColor.GOLD + "Redeem",
                    ChatColor.GRAY + "اكتب الكود الذي اشتريته من موقع " + ChatColor.AQUA + "OMAREX Hub",
                    ChatColor.GREEN + "واحصل على رتبتك وأسلحتك الحقيقية فوراً!"
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            star.setItemMeta(meta);
        }
        return star;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean hasItem = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isRedeemShopItem(item)) {
                hasItem = true;
                break;
            }
        }
        if (!hasItem) {
            player.getInventory().addItem(createRedeemShopItem());
        }
    }

    private boolean isRedeemShopItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && ITEM_DISPLAY_NAME.equals(meta.getDisplayName());
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isRedeemShopItem(event.getItem())) {
                event.setCancelled(true);
                openRedeemGUI(event.getPlayer());
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "هذا الأمر مخصص للاعبين فقط!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("get")) {
            player.getInventory().addItem(createRedeemShopItem());
            player.sendMessage(ChatColor.GREEN + "✔ تم إعطاؤك أداة متجر " + ChatColor.GOLD + "Redeem" + ChatColor.GREEN + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
            return true;
        }

        openRedeemGUI(player);
        return true;
    }

    public void openRedeemGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, GUI_TITLE);

        ItemStack borderGlass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = borderGlass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            borderGlass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 45; i++) {
            if (i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, borderGlass);
            }
        }

        for (RewardEntry entry : rewardRegistry.values()) {
            ItemStack icon = new ItemStack(entry.iconMaterial);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + entry.displayName);
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "رمز الكود المطلوب: " + ChatColor.AQUA + "OMAREX-" + entry.prefix + "-XXXX",
                        "",
                        ChatColor.YELLOW + "▶ اضغط هنا لكتابة الكود في الشات",
                        ChatColor.GREEN + "▶ واستلام الجائزة الحقيقية فوراً!"
                ));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                icon.setItemMeta(meta);
            }
            gui.setItem(entry.slot, icon);
        }

        ItemStack anyCodeBook = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta bookMeta = anyCodeBook.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "✦ إدخال أي كود من OMAREX Hub ✦");
            bookMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "يدعم جميع الأكواد الـ 100 من المتجر!",
                    ChatColor.YELLOW + "اضغط هنا ثم اكتب الكود في الشات مباشرة."
            ));
            anyCodeBook.setItemMeta(bookMeta);
        }
        gui.setItem(40, anyCodeBook);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.1f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int clickedSlot = event.getRawSlot();
        if (clickedSlot < 0 || clickedSlot >= 45) return;

        RewardEntry selected = null;
        for (RewardEntry entry : rewardRegistry.values()) {
            if (entry.slot == clickedSlot) {
                selected = entry;
                break;
            }
        }

        if (selected != null || clickedSlot == 40) {
            player.closeInventory();
            awaitingCodeInput.put(player.getUniqueId(), selected);

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            player.sendMessage("");
            player.sendMessage(ChatColor.DARK_PURPLE + "========================================");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "  متجر Redeem - OMAREX NETWORK");
            if (selected != null) {
                player.sendMessage(ChatColor.WHITE + "  الجائزة المختارة: " + ChatColor.AQUA + selected.displayName);
                player.sendMessage(ChatColor.GRAY + "  اكتب الكود في الشات الآن (مثال: " + ChatColor.YELLOW + "OMAREX-" + selected.prefix + "-A9X4821" + ChatColor.GRAY + ")");
            } else {
                player.sendMessage(ChatColor.WHITE + "  اكتب كود الجائزة الكامل في الشات الآن:");
            }
            player.sendMessage(ChatColor.RED + "  (أو اكتب 'cancel' للإلغاء)");
            player.sendMessage(ChatColor.DARK_PURPLE + "========================================");
            player.sendMessage("");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!awaitingCodeInput.containsKey(uuid)) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim().toUpperCase();
        RewardEntry expectedEntry = awaitingCodeInput.remove(uuid);

        Bukkit.getScheduler().runTask(this, () -> {
            if (message.equalsIgnoreCase("CANCEL") || message.equals("الغاء") || message.equals("إلغاء")) {
                player.sendMessage(ChatColor.YELLOW + "✖ تم إلغاء عملية استرداد الكود.");
                return;
            }

            String[] parts = message.split("-");
            if (parts.length != 3 || !parts[0].equals("OMAREX") || parts[2].length() < 6) {
                player.sendMessage(ChatColor.RED + "✖ صيغة الكود غير صحيحة! يجب أن يبدأ الكود بـ OMAREX-...");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (usedCodes.contains(message)) {
                player.sendMessage(ChatColor.RED + "✖ هذا الكود مستخدم من قبل ولا يمكن استخدامه مرتين!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            String prefix = parts[1];

            if (expectedEntry != null && !expectedEntry.prefix.equals(prefix)) {
                player.sendMessage(ChatColor.RED + "✖ هذا الكود لا يطابق الجائزة التي اخترتها (" + expectedEntry.displayName + ")!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            usedCodes.add(message);
            saveUsedCodes();

            deliverRealReward(player, prefix, message);
        });
    }

    private void deliverRealReward(Player player, String prefix, String fullCode) {
        RewardEntry entry = rewardRegistry.get(prefix);

        if (entry != null) {
            switch (entry.type) {
                case RANK -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent add " + entry.rankGroup);
                    player.sendMessage(ChatColor.GREEN + "✔ تم تفعيل " + ChatColor.GOLD + entry.displayName + ChatColor.GREEN + " لحسابك بنجاح!");
                }
                case SWORD_MAX -> {
                    ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
                    ItemMeta meta = sword.getItemMeta();
                    meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "سيف النذرايت الأسطوري");
                    meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                    meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
                    sword.setItemMeta(meta);
                    giveOrDrop(player, sword);
                }
                case ZENITH_BLADE -> {
                    ItemStack zenith = new ItemStack(Material.NETHERITE_SWORD);
                    ItemMeta meta = zenith.getItemMeta();
                    meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "⚡ سيف الغريب (Zenith Blade) ⚡");
                    meta.addEnchant(Enchantment.SHARPNESS, 6, true);
                    meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                    meta.addEnchant(Enchantment.LOOTING, 3, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    zenith.setItemMeta(meta);
                    giveOrDrop(player, zenith);
                }
                case BOW_MAX -> {
                    ItemStack bow = new ItemStack(Material.BOW);
                    ItemMeta meta = bow.getItemMeta();
                    meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "⚡ قوس البرق الخارق ⚡");
                    meta.addEnchant(Enchantment.POWER, 5, true);
                    meta.addEnchant(Enchantment.FLAME, 1, true);
                    meta.addEnchant(Enchantment.PUNCH, 2, true);
                    meta.addEnchant(Enchantment.INFINITY, 1, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    bow.setItemMeta(meta);
                    giveOrDrop(player, bow);
                    giveOrDrop(player, new ItemStack(Material.ARROW, 1));
                }
                case ARMOR_SET -> {
                    Material[] armorPieces = {
                            Material.NETHERITE_HELMET,
                            Material.NETHERITE_CHESTPLATE,
                            Material.NETHERITE_LEGGINGS,
                            Material.NETHERITE_BOOTS
                    };
                    for (Material mat : armorPieces) {
                        ItemStack piece = new ItemStack(mat);
                        ItemMeta meta = piece.getItemMeta();
                        meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "درع OMAREX الملكي");
                        meta.addEnchant(Enchantment.PROTECTION, 4, true);
                        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                        meta.addEnchant(Enchantment.MENDING, 1, true);
                        piece.setItemMeta(meta);
                        giveOrDrop(player, piece);
                    }
                }
                case ELYTRA_KIT -> {
                    ItemStack elytra = new ItemStack(Material.ELYTRA);
                    ItemMeta meta = elytra.getItemMeta();
                    meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "أجنحة OMAREX السماوية");
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    elytra.setItemMeta(meta);
                    giveOrDrop(player, elytra);
                    giveOrDrop(player, new ItemStack(Material.FIREWORK_ROCKET, 64));
                }
                case PICKAXE_MAX -> {
                    ItemStack pick = new ItemStack(Material.NETHERITE_PICKAXE);
                    ItemMeta meta = pick.getItemMeta();
                    meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "معول التدمير الأسطوري");
                    meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                    meta.addEnchant(Enchantment.FORTUNE, 3, true);
                    meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    meta.addEnchant(Enchantment.MENDING, 1, true);
                    pick.setItemMeta(meta);
                    giveOrDrop(player, pick);
                }
                case TOTEMS -> giveOrDrop(player, new ItemStack(Material.TOTEM_OF_UNDYING, 5));
                case ZENITH_CRATE -> {
                    giveOrDrop(player, new ItemStack(Material.NETHERITE_INGOT, 8));
                    giveOrDrop(player, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 6));
                    giveOrDrop(player, new ItemStack(Material.DIAMOND_BLOCK, 16));
                }
            }
        } else {
            if (prefix.startsWith("WPN")) {
                ItemStack bonusSword = new ItemStack(Material.NETHERITE_SWORD);
                bonusSword.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
                bonusSword.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
                giveOrDrop(player, bonusSword);
            } else if (prefix.startsWith("ARM")) {
                giveOrDrop(player, new ItemStack(Material.NETHERITE_INGOT, 6));
                giveOrDrop(player, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4));
            } else if (prefix.startsWith("CRT")) {
                giveOrDrop(player, new ItemStack(Material.DIAMOND, 32));
                giveOrDrop(player, new ItemStack(Material.GOLDEN_APPLE, 16));
            } else if (prefix.startsWith("COS")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set omarex.cosmetic." + prefix.toLowerCase() + " true");
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent add " + prefix.toLowerCase());
            }
        }

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        String rewardTitle = (entry != null) ? entry.displayName : ("جائزة المتجر (" + prefix + ")");
        Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "✦ " + ChatColor.GOLD + player.getName()
                + ChatColor.WHITE + " استردّ كوداً من متجر " + ChatColor.AQUA + "OMAREX Hub"
                + ChatColor.WHITE + " وحصل على: " + ChatColor.GREEN + ChatColor.BOLD + rewardTitle + ChatColor.DARK_PURPLE + " ✦");
    }

    private void giveOrDrop(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    private enum RewardType {
        RANK, SWORD_MAX, ZENITH_BLADE, BOW_MAX, ARMOR_SET, ELYTRA_KIT, PICKAXE_MAX, TOTEMS, ZENITH_CRATE
    }

    private static class RewardEntry {
        final String prefix;
        final String displayName;
        final Material iconMaterial;
        final int slot;
        final RewardType type;
        final String rankGroup;

        RewardEntry(String prefix, String displayName, Material iconMaterial, int slot, RewardType type, String rankGroup) {
            this.prefix = prefix;
            this.displayName = displayName;
            this.iconMaterial = iconMaterial;
            this.slot = slot;
            this.type = type;
            this.rankGroup = rankGroup;
        }
    }
}
