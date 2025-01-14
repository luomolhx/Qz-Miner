package club.heiqi.qz_miner.minerModes.rangeMode.posFounder;

import club.heiqi.qz_miner.minerModes.PositionFounder;
import net.minecraft.entity.player.EntityPlayer;
import org.joml.Vector3i;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static club.heiqi.qz_miner.MY_LOG.LOG;

public class Sphere extends PositionFounder {
    public Set<Vector3i> cacheSet = new HashSet<>();
    public Set<Vector3i> shellSet = new HashSet<>();
    /**
     * 构造函数准备执行搜索前的准备工作
     *
     * @param center 被破坏方块的中心坐标
     * @param lock
     */
    public Sphere(Vector3i center, EntityPlayer player, ReentrantReadWriteLock lock) {
        super(center, player, lock);
        shellSet.add(center);
        setRadius(1);
    }

    @Override
    public void loopLogic() {
        scanSurrounding();
        setRadius(getRadius() + 1);
    }

    public void scanSurrounding() {
        // 缓存上一次的cacheSet
        Set<Vector3i> lastCacheSet = new HashSet<>(cacheSet);
        // 获取球体外围一圈点附加到cacheSet中 - 获取下一个壳层
        for (Vector3i pos : shellSet) { // 通过遍历当前壳层获取一下壳层
            Vector3i up = new Vector3i(pos.x, pos.y + 1, pos.z);
            Vector3i down = new Vector3i(pos.x, pos.y - 1, pos.z);
            Vector3i left = new Vector3i(pos.x - 1, pos.y, pos.z);
            Vector3i right = new Vector3i(pos.x + 1, pos.y, pos.z);
            Vector3i front = new Vector3i(pos.x, pos.y, pos.z + 1);
            Vector3i back = new Vector3i(pos.x, pos.y, pos.z - 1);
            if (checkCanBreak(up))
                cacheSet.add(up);
            if (checkCanBreak(down))
                cacheSet.add(down);
            if (checkCanBreak(left))
                cacheSet.add(left);
            if (checkCanBreak(right))
                cacheSet.add(right);
            if (checkCanBreak(front))
                cacheSet.add(front);
            if (checkCanBreak(back))
                cacheSet.add(back);
        }
        shellSet.clear(); // 清空当前壳层，准备放入下一壳层
        // 新球体减去旧球体 - 新: cacheSet - 旧: lastCacheSet
        for (Vector3i pos : cacheSet) {
            // 检查pos是否在lastCacheSet中，并且检查是否在指定球体大小中，加入壳层
            if (!lastCacheSet.contains(pos) && checkInSphere(pos)) {
                shellSet.add(pos);
                if (beforePutCheck()) {
                    return;
                }
                try {
                    if (checkCanBreak(pos)) { // 只放入可挖掘的点
                        cache.put(pos); canBreakBlockCount++;
                    }
                } catch (InterruptedException e) {
                    LOG.warn("缓存队列异常");
                    Thread.currentThread().interrupt(); // 恢复中断状态
                    return;
                }
            }
        }
    }

    public boolean checkInSphere(Vector3i pos) {
        int distanceSquared = getRadius() * getRadius();
        int actualDistanceSquared = (center.x - pos.x) * (center.x - pos.x)
                                  + (center.y - pos.y) * (center.y - pos.y)
                                  + (center.z - pos.z) * (center.z - pos.z);
        return actualDistanceSquared <= distanceSquared;
    }
}
