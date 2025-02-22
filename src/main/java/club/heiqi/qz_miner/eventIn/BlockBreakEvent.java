package club.heiqi.qz_miner.eventIn;

import club.heiqi.qz_miner.minerModes.ModeManager;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import org.joml.Vector3i;

import static club.heiqi.qz_miner.Mod_Main.LOG;
import static club.heiqi.qz_miner.Mod_Main.allPlayerStorage;

/**
 * 整个连锁逻辑的入口点
 */
public class BlockBreakEvent {
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new BlockBreakEvent());
    }

    @SubscribeEvent
    public void blockBreakEvent(BlockEvent.BreakEvent event) {
        World world = event.world;
        EntityPlayer player = event.getPlayer();
        if (player instanceof FakePlayer) {
            return;
        }
        try {
            if (!allPlayerStorage.playerStatueMap.get(player.getUniqueID()).getIsReady()) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        // 获取破坏方块的坐标
        Vector3i breakBlockPos = new Vector3i(event.x, event.y, event.z);
        ModeManager modeManager = allPlayerStorage.playerStatueMap.get(player.getUniqueID());
        try {
            modeManager.proxyMine(world, breakBlockPos, player);
        } catch (Exception e) {
            LOG.info("代理挖掘时发生错误![An error occurred while proxy mining]");
            LOG.info(e);
        }
    }
}
