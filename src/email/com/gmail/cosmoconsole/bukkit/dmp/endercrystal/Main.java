package email.com.gmail.cosmoconsole.bukkit.dmp.endercrystal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Bed.Part;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import email.com.gmail.cosmoconsole.bukkit.deathmsg.DMPReloadEvent;
import email.com.gmail.cosmoconsole.bukkit.deathmsg.DeathMessageCustomEvent;
import email.com.gmail.cosmoconsole.bukkit.deathmsg.DeathMessagesPrime;
import email.com.gmail.cosmoconsole.bukkit.deathmsg.DeathPreDMPEvent;

public class Main extends JavaPlugin implements Listener {
    private static final int CONFIG_VERSION = 2;
    private static final String CRYSTAL_KILL_TAG = "natural.EnderCrystalKill";
    private static final String CRYSTAL_SUICIDE_TAG = "natural.EnderCrystalSuicide";
    private static final String BED_KILL_TAG = "natural.BedKill";
    private static final String BED_SUICIDE_TAG = "natural.BedSuicide";
    DeathMessagesPrime dmp;
    FileConfiguration config;
    Map<UUID, UUID> do_eck;
    Map<UUID, UUID> do_bedk;
    private boolean enableBed = false;
    private boolean trackPlacerNotHitter = true;
    private boolean trackBedPlacerNotHitter = true;
    private static Set<String> bedTypes;
    
    static {
        bedTypes = new HashSet<String>();
        bedTypes.addAll(Arrays.asList("BED", "BED_BLOCK",
                "BLACK_BED", "BLUE_BED", "BROWN_BED", "CYAN_BED",
                "GRAY_BED", "GREEN_BED", "LIGHT_BLUE_BED", "LIGHT_GRAY_BED",
                "LIME_BED", "MAGENTA_BED", "ORANGE_BED", "PINK_BED",
                "PURPLE_BED", "RED_BED", "WHITE_BED", "YELLOW_BED"));
    }
    
    public Main() {
        do_eck = new HashMap<>();
        do_bedk = new HashMap<>();  
    }
    
    public void onEnable() {
        dmp = (DeathMessagesPrime) getServer().getPluginManager().getPlugin("DeathMessagesPrime");
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.loadConfig();
        try {
            String ver = Bukkit.getServer().getVersion().split("\\(MC:")[1].split("\\)")[0].trim().split(" ")[0].trim();
            String[] tokens = ver.split("\\.");
            int mcMajor = Integer.parseInt(tokens[0]);
            int mcMinor = 0;
            int mcRevision = 0;
            if (tokens.length > 1) {
                mcMinor = Integer.parseInt(tokens[1]);
            }
            if (tokens.length > 2) {
                mcRevision = Integer.parseInt(tokens[2]);
            }
            mc_ver = mcMajor * 1000 + mcMinor;
            mc_rev = mcRevision;
            // 1.8 = 1_008
            // 1.9 = 1_009
            // 1.10 = 1_010
            // ...
            // 1.14 = 1_014
            // 1.15 = 1_015
        } catch (Exception ex) {
            this.getLogger().warning("Cannot detect Minecraft version from string - " +
                                     "some features will not work properly. " + 
                                     "Please contact the plugin author if you are on " +
                                     "standard CraftBukkit or Spigot. This plugin " + 
                                     "expects getVersion() to return a string " + 
                                     "containing '(MC: 1.15)' or similar. The version " + 
                                     "DMP tried to parse was '" + Bukkit.getServer().getVersion() + "'");
        }
    }

    @EventHandler
    public void reloadConfig(DMPReloadEvent e) {
        this.loadConfig();
    }
    
    private void loadConfig() {
        this.config = this.getConfig();
        try {
            this.config.load(new File(this.getDataFolder(), "config.yml"));
            if (!this.config.contains("config-version")) {
                throw new Exception();
            }
            if (this.config.getInt("config-version") < CONFIG_VERSION) {
                throw new ConfigTooOldException();
            }
        }
        catch (FileNotFoundException e6) {
            this.getLogger().info("Extracting default config.");
            this.saveResource("config.yml", true);
            try {
                this.config.load(new File(this.getDataFolder(), "config.yml"));
            }
            catch (IOException | InvalidConfigurationException ex3) {
                ex3.printStackTrace();
                this.getLogger().severe("The JAR config is broken, disabling");
                this.getServer().getPluginManager().disablePlugin((Plugin)this);
                this.setEnabled(false);
            }
        }
        catch (ConfigTooOldException e3) {
            this.getLogger().warning("!!! WARNING !!! Your configuration is old. There may be new features or some config behavior might have changed, so it is advised to regenerate your config when possible!");
        }
        catch (Exception e4) {
            e4.printStackTrace();
            this.getLogger().severe("Configuration is invalid. Re-extracting it.");
            final boolean success = !new File(this.getDataFolder(), "config.yml").isFile() || new File(this.getDataFolder(), "config.yml").renameTo(new File(this.getDataFolder(), "config.yml.broken" + new Date().getTime()));
            if (!success) {
                this.getLogger().severe("Cannot rename the broken config, disabling");
                this.getServer().getPluginManager().disablePlugin((Plugin)this);
                this.setEnabled(false);
            }
            this.saveResource("config.yml", true);
            try {
                this.config.load(new File(this.getDataFolder(), "config.yml"));
            }
            catch (IOException | InvalidConfigurationException ex4) {
                ex4.printStackTrace();
                this.getLogger().severe("The JAR config is broken, disabling");
                this.getServer().getPluginManager().disablePlugin((Plugin)this);
                this.setEnabled(false);
            }
        }
        this.enableBed = this.config.getBoolean("use-bed-messages", false);
        this.trackPlacerNotHitter = this.config.getBoolean("track-placer-not-hitter", true);
        this.trackBedPlacerNotHitter = this.config.getBoolean("track-bed-placer-not-hitter", true);
    }

    private static int mc_ver = 0;
    private static int mc_rev = 0;
    
    public static boolean mcVer(int comp) {
        return mc_ver >= comp;
    }

    public static boolean mcVerRev(int comp, int rev) {
        return mc_ver > comp || (mc_ver == comp && mc_rev >= rev);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        if (isBed(event.getBlockPlaced().getType())) {
            String us = event.getPlayer().getUniqueId().toString();
            Block b = event.getBlockPlaced();
            b.setMetadata("dmp.bedPlacer", new FixedMetadataValue(this, us));
            b = getOtherBedBlock(event, b);
            if (isBed(b.getType()))
                b.setMetadata("dmp.bedPlacer", new FixedMetadataValue(this, us));
        }
    }
    
    private Block getOtherBedBlock(final BlockPlaceEvent event, final Block block) {
        BlockFace orientation = getBedOrientation(block, event.getBlockPlaced().getState());
        return block.getRelative(orientation);
    }

    private BlockFace getBedOrientation(Block block, BlockState newState) {
        if (mcVer(1_015))
            return getBedOrientation15(block, newState);
        else if (mcVer(1_008))
            return getBedOrientation8(block, newState);
        else
            return getBedOrientationRawData(block, newState);
    }

    private BlockFace getBedOrientation15(Block block, BlockState newState) {
        Bed bed = (Bed)newState.getBlockData();
        BlockFace orientation = bed.getFacing();
        if (bed.getPart() == Part.HEAD)
            orientation = orientation.getOppositeFace();
        return orientation;
    }

    @SuppressWarnings("deprecation")
    private BlockFace getBedOrientation8(Block block, BlockState newState) {
        BlockFace orientation = BlockFace.SELF;
        org.bukkit.material.MaterialData md = newState.getData();
        if (md instanceof org.bukkit.material.Directional)
            orientation = ((org.bukkit.material.Directional)md).getFacing();
        if (md instanceof org.bukkit.material.Bed)
        {
            org.bukkit.material.Bed bed = (org.bukkit.material.Bed)md;
            if (bed.isHeadOfBed())
                orientation = orientation.getOppositeFace();   
        }
        return orientation;
    }

    @SuppressWarnings("deprecation")
    private BlockFace getBedOrientationRawData(Block block, BlockState newState) {
        byte b =  newState.getRawData();
        BlockFace orientation = (new BlockFace[] {BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST})[b & 3];
        if ((b & 8) != 0)
            orientation = orientation.getOppositeFace();
        return orientation;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        // track players who place ender crystals
        if (Action.RIGHT_CLICK_BLOCK == event.getAction()) {
            if (Material.OBSIDIAN == event.getClickedBlock().getType() || Material.BEDROCK == event.getClickedBlock().getType()) {
                if (Material.END_CRYSTAL == event.getMaterial()) {
                    Bukkit.getScheduler().runTask(this, new Runnable() {
                        @Override
                        public void run() {
                            List<Entity> entities = event.getPlayer().getNearbyEntities(5, 5, 5);

                            for (Entity entity : entities) {
                                if (EntityType.ENDER_CRYSTAL == entity.getType()) {
                                    EnderCrystal crystal = (EnderCrystal) entity;
                                    Block belowCrystal = crystal.getLocation().getBlock().getRelative(BlockFace.DOWN);

                                    if (event.getClickedBlock().equals(belowCrystal) && crystal.getTicksLived() < 3) {
                                        crystal.setMetadata("dmp.enderCrystalPlacer", new FixedMetadataValue(Main.this, event.getPlayer().getUniqueId().toString()));
                                    }
                                }
                            }
                        }
                    });
                }
            }
            else if (isBed(event.getClickedBlock().getType())) {
                UUID u = event.getPlayer().getUniqueId();
                if (trackBedPlacerNotHitter) {
                    u = null;
                    Block b = event.getClickedBlock();
                    if (b.hasMetadata("dmp.bedPlacer") && b.getMetadata("dmp.bedPlacer").size() > 0)
                        u = UUID.fromString(b.getMetadata("dmp.bedPlacer").get(0).asString());   
                }
                if (u != null)
                    for (Entity e: event.getClickedBlock().getWorld().getNearbyEntities(event.getClickedBlock().getLocation(), 8, 8, 8)) {
                        if (e instanceof Player) {
                            Player p = (Player)e;
                            do_bedk.put(p.getUniqueId(), u);
                        }
                    }
            }
        }
    }
    
    private boolean isBed(Material type) {
        return bedTypes.contains(type.name());
    }

    @EventHandler
    public void prePrepare(DeathPreDMPEvent e) {
        Entity damager = e.getDamager();
        if (damager != null && damager instanceof EnderCrystal) {
            EnderCrystal ec = (EnderCrystal) damager;
            UUID ped = null;
            if (!trackPlacerNotHitter) { // ped = hitter
                if (damager.getLastDamageCause() != null && ((damager.getLastDamageCause().getCause() == DamageCause.ENTITY_ATTACK || damager.getLastDamageCause().getCause() == DamageCause.ENTITY_SWEEP_ATTACK)) && damager.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent ed = (EntityDamageByEntityEvent) damager.getLastDamageCause();
                    if (ed.getDamager() instanceof Player) {
                        ped = ((Player) ed.getDamager()).getUniqueId();
                    }
                }
            } else { // ped = placer
                if (ec.hasMetadata("dmp.enderCrystalPlacer") && ec.getMetadata("dmp.enderCrystalPlacer").size() > 0) {
                    ped = UUID.fromString(ec.getMetadata("dmp.enderCrystalPlacer").get(0).asString());
                }
            }
            
            if (ped != null) { //&& !ped.equals(e.getPlayer().getUniqueId())) {
                // store killer player
                do_eck.put(e.getPlayer().getUniqueId(), ped);
            }            
        } else if (e.getCause() != DamageCause.BLOCK_EXPLOSION) {
            do_bedk.remove(e.getPlayer().getUniqueId());
        }
    }
    
    @EventHandler
    public void preBroadcast(DeathMessageCustomEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        // apply custom ender crystal kill message
        if (this.enableBed && do_bedk.containsKey(u)) {
            UUID ku = do_bedk.get(u);
            if (ku != null) {
                Player p = getServer().getPlayer(ku);
                if (p != null) {
                    if (p.getUniqueId().equals(u)) {
                        e.setTag(BED_SUICIDE_TAG);
                    } else {
                        e.setTag(BED_KILL_TAG);
                        e.setKiller(p.getName());
                        e.setKiller2(p.getDisplayName());
                    }
                }
            }
            do_bedk.remove(u);
        }
        if (do_eck.containsKey(u)) {
            UUID ku = do_eck.get(u);
            if (ku != null) {
                Player p = getServer().getPlayer(ku);
                if (p != null) {
                    if (p.getUniqueId().equals(u)) {
                        e.setTag(CRYSTAL_SUICIDE_TAG);
                    } else {
                        e.setTag(CRYSTAL_KILL_TAG);
                        e.setKiller(p.getName());
                        e.setKiller2(p.getDisplayName());
                    }
                }
            }
            do_eck.remove(u);
        }
    }
}