package club.heiqi.qz_miner.statueStorage;

import club.heiqi.qz_miner.MOD_INFO;
import club.heiqi.qz_miner.minerModes.ModeManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static club.heiqi.qz_miner.MY_LOG.LOG;

public class AllPlayer {
    public Map<UUID, ModeManager> playerStatueMap = new HashMap<>();

    // 客户端连接服务器事件

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player.worldObj.isRemote) SelfStatue.modeManager = new ModeManager();
        EntityPlayer player = event.player;
        UUID uuid = player.getUniqueID();
        if (playerStatueMap.containsKey(uuid)) {
            LOG.info("玩家: {} 已在缓存连锁实例中，无需再次创建", player.getDisplayName());
        } else {
            LOG.info("玩家: {} 已登录，缓存连锁实例中不存在，已创建", player.getDisplayName());
            ModeManager modeManager = new ModeManager();
            modeManager.playerMP = (EntityPlayerMP) player;
            playerStatueMap.put(uuid, modeManager);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player.worldObj.isRemote) SelfStatue.modeManager = null;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        UUID playerUUID = player.getUniqueID();
        if (playerStatueMap.containsKey(playerUUID)) {
            LOG.info("玩家: {} 已登出，连锁实例中已删除", player.getDisplayName());
            playerStatueMap.remove(playerUUID);
        } else {
            LOG.info("玩家: {} 已登出，连锁实例中不存在，无需删除", player.getDisplayName());
        }
    }

    /**
     * 兼容性检查
     * @param mods
     * @param side
     * @return
     */
    @NetworkCheckHandler
    public boolean checkClientVersion(Map<String, String> mods, Side side) {
        if (mods.isEmpty()) {
            return true;
        }
        if (mods.containsKey(MOD_INFO.MODID)) {
            String clientVersion = mods.get(MOD_INFO.MODID);
            String serverVersion = MOD_INFO.VERSION;
            if (serverVersion.equals(clientVersion) || serverVersion.startsWith(clientVersion) || clientVersion.startsWith(serverVersion)) {
                return true;
            }
            int clientMajor = Integer.parseInt(clientVersion.split("\\.")[0]);
            int clientMinor = Integer.parseInt(clientVersion.split("\\.")[1]);
            int clientPatch = Integer.parseInt(clientVersion.split("\\.")[2]);
            int serverMajor = Integer.parseInt(serverVersion.split("\\.")[0]);
            int serverMinor = Integer.parseInt(serverVersion.split("\\.")[1]);
            int serverPatch = Integer.parseInt(serverVersion.split("\\.")[2]);
            if (serverMajor == clientMajor && serverMinor == clientMinor) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }
}
