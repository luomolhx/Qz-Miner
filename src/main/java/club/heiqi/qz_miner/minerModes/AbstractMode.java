package club.heiqi.qz_miner.minerModes;

import club.heiqi.qz_miner.Config;
import club.heiqi.qz_miner.Mod_Main;
import club.heiqi.qz_miner.minerModes.breakBlock.BlockBreaker;
import club.heiqi.qz_miner.statueStorage.SelfStatue;
import club.heiqi.qz_miner.threadPool.QzMinerThreadPool;
import club.heiqi.qz_miner.util.MessageUtil;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static club.heiqi.qz_miner.MY_LOG.LOG;
import static club.heiqi.qz_miner.Mod_Main.allPlayerStorage;

/**
 * 现在这个类会被反射多次实例化，不再是单例模式!
 */
public abstract class AbstractMode {
    public static int timeout = 1000;
    public static int timeLimit = Config.taskTimeLimit;
    public static int perTickBlock = Config.perTickBlockLimit;
    @Nullable
    public PositionFounder positionFounder;
    public BlockBreaker breaker;
    // 用于控制是否结束
    public AtomicBoolean isRunning = new AtomicBoolean(false);
    public AtomicBoolean isWait = new AtomicBoolean(false);
    public long timer;
    public long getCacheFailTimeOutTimer;   // 获取cache超时会结束
    public int getCacheFailCount = 0;       // 获取cache失败次数，用于重置超时时间
    public int blockCount;                  // 挖掘方块数量，达到挖掘数量时也会结束
    public int perTickCounter;

    public ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public Block blockSample;
    public List<ItemStack> dropSample = new ArrayList<>(); // 掉落物样本数组

    public void updateTaskType() {
        isWait.set(checkShouldWait());
        isRunning.set(!checkShouldShutdown());
    }

    public void setup(World world, EntityPlayerMP player, Vector3i center) {
        isRunning.set(true);
        isWait.set(false);
        breaker = new BlockBreaker(player, world);

        ModeManager modeManager = allPlayerStorage.playerStatueMap.get(player.getUniqueID());
        positionFounder = modeManager.getPositionFounder(center, player, lock);
    }

    public void run() {
        readConfig();
        register();
        if (positionFounder != null) {
            QzMinerThreadPool.pool.submit(positionFounder);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        timer = System.currentTimeMillis();
        updateTaskType();
        // 每次循环只挖掘一个点
        while (isRunning.get()) {
            try {
                Vector3i pos = null;
                if (positionFounder != null) {
                    pos = positionFounder.cache.poll(5, TimeUnit.MILLISECONDS);
                }
                if (pos != null && checkCanBreak(pos) && filter(pos)) {
                    breaker.tryHarvestBlock(pos);
                    blockCount++;
                    perTickCounter++;
                }
            } catch (InterruptedException e) {
                LOG.warn("线程异常");
            }
            updateTaskType();
            if (isWait.get()) {
//                Mod_Main.LOG.info("正在等待");
                break;
            }
        }
        perTickCounter = 0;
        updateTaskType();
        if (!isRunning.get()) {
            unregister();
        }
    }

    public void readConfig() {
        timeLimit = Config.taskTimeLimit;
        perTickBlock = Config.perTickBlockLimit;
        if (perTickBlock <= 0) {
            perTickBlock = Integer.MAX_VALUE;
        }
    }

    public void register() {
        isRunning.set(true);
        // 注册监听器
        FMLCommonHandler.instance().bus().register(this);
        LOG.info("玩家: {} 的挖掘任务已启动，注册监听器", breaker.player.getDisplayName());
    }

    public void unregister() {
        isRunning.set(false);
        // 注销监听器
        LOG.info("玩家: {} 的挖掘任务已结束，卸载监听器", breaker.player.getDisplayName());
        ModeManager modeManager = allPlayerStorage.playerStatueMap.get(breaker.player.getUniqueID());
        modeManager.isRunning.set(false);
        if (positionFounder != null && modeManager.getPrintResult()) {
            String text = "挖掘任务结束，共挖掘" + blockCount + "方块，" + positionFounder.getRadius() + "格半径";
            sendMessage(text);
        }
        FMLCommonHandler.instance().bus().unregister(this);
    }

    public boolean checkTimeout() {
        long time = System.currentTimeMillis() - timer;
        return time > timeLimit;
    }

    /**
     * 是否应该等待<br>
     * 判定条件: 1. 缓存为空<br>
     * 2. 超时
     * @return true: 需要等待
     */
    public boolean checkShouldWait() {
        if (positionFounder != null && positionFounder.cache.isEmpty()) { // 如果缓存为空
            return true;
        }
        if (perTickCounter >= perTickBlock) {
            return true;
        }
        if (checkTimeout()) { // 超时
            return true;
        }
        return false;
    }

    /**
     * 该方法指示了一定会达成停止的条件
     */
    public boolean checkShouldShutdown() {
        if (blockCount >= Config.blockLimit) { // 达到限制数量
//            Mod_Main.LOG.info("达到数量上限停止");
            return true;
        }
        if (positionFounder != null
            && positionFounder.cache.isEmpty()
            && !positionFounder.isRunning.get()) { // 缓存为空且任务结束
//            Mod_Main.LOG.info("缓存为空且任务结束停止");;
            return true;
        }
        if (breaker.player.getHealth() <= 2) { // 玩家生命值过低
//            Mod_Main.LOG.info("玩家生命值过低");
            return true;
        }
        if (positionFounder != null && positionFounder.cache.isEmpty()) { // 获取缓存失败
            if (getCacheFailCount <= 1) {
                getCacheFailCount++;
                getCacheFailTimeOutTimer = System.currentTimeMillis();
            } else {
                if (System.currentTimeMillis() - getCacheFailTimeOutTimer > timeout) {
//                    Mod_Main.LOG.info("获取缓存失败");
                    return true;
                }
            }
        }
        if (!getIsReady()) {
//            Mod_Main.LOG.info("挖掘任务未就绪");
            return true;
        }
        return false;
    }

    public boolean checkCanBreak(Vector3i pos) {
        World world = breaker.world;
        Block block = world.getBlock(pos.x, pos.y, pos.z);
        int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);
        EntityPlayerMP player = breaker.player;
        ItemInWorldManager iwm = player.theItemInWorldManager;
        ItemStack holdItem = iwm.thisPlayerMP.getCurrentEquippedItem();
        // 判断是否为创造模式
        if (iwm.getGameType().isCreative()) {
            return true;
        }
        // 判断是否为空气
        if (block == Blocks.air) {
            return false;
        }
        // 判断是否为流体
        if (block.getMaterial().isLiquid()) {
            return false;
        }
        // 判断是否为基岩
        if (block == Blocks.bedrock) {
            return false;
        }
        // 判断是否为非固体
        if (!block.getMaterial().isSolid()) {
            return false;
        }
        // 判断工具能否挖掘
        if (holdItem != null) {
            return block.canHarvestBlock(player, meta);
        }
        return true;
    }

    public boolean filter(Vector3i pos) {
        return true;
    }

    public boolean getIsReady() {
        try {
            if (allPlayerStorage.playerStatueMap.get(breaker.player.getUniqueID()).getIsReady()) { // 玩家未就绪
                return true;
            }
        } catch (Exception e) {
            Mod_Main.LOG.error("服务端获取就绪状态时出错: {}", e.toString());
            try {
                if (SelfStatue.modeManager.getIsReady()) {
                    return true;
                }
            } catch (Exception ee) {
                LOG.error("获取就绪状态时出错: {}", ee.toString());
                try {
                    MessageUtil.broadcastMessage("获取就绪状态时出错");
                } catch (Exception ex) {
                    Mod_Main.LOG.error("发送消息时出错");
                }
            }
        }
        return false;
    }

    public void sendMessage(String message) {
        ChatComponentText text = new ChatComponentText(message);
        if (text == null) {
            ChatComponentText error = new ChatComponentText("[QZ_Miner] 错误：你不应该看到这段文本，如果看到该段请上报至该模组的github仓库issue，或者在GTNH中文一群报告此信息");
            breaker.player.addChatMessage(error);
            return;
        }
        breaker.player.addChatMessage(text);
    }
}
