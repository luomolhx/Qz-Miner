package club.heiqi.qz_miner.MineModeSelect.PointFonder;

import club.heiqi.qz_miner.Config;
import club.heiqi.qz_miner.CustomData.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PointFonder_Center extends PointFonder{
    public Collection<Point> nextSearch = new ArrayList<>();
    public List<Point> visitedPoints = new ArrayList<>() {
        /**
         * 超出容量自动删除头部元素
         */
        @Override
        public boolean add(Point lP) {
            if(visitedPoints.size() > Config.pointFounderCacheSize*3) {
                visitedPoints.remove(0);
            }
            return super.add(lP);
        }
    };
    public int searchP = 0;  // 遍历cache的索引

    public PointFonder_Center(Point point) {
        super(point);
        nextSearch.add(centerP);
        maxX = centerP.x + Config.radiusLimit;
        maxY = Math.max(centerP.y + Config.radiusLimit, 255);
        maxZ = centerP.z + Config.radiusLimit;
        minX = centerP.x - Config.radiusLimit;
        minY = Math.min(centerP.y - Config.radiusLimit,0);
        minZ = centerP.z - Config.radiusLimit;
    }

    /**
     * 核心搜索逻辑
     */
    @Override
    public void startPhase() {
        while(nextSearch.iterator().hasNext()) {
            Point curPoint = nextSearch.iterator().next();
            nextSearch.remove(curPoint); // 取出点并删除
            visitedPoints.add(curPoint);
            List<Point> surround = surroundPoints(curPoint);
            List<Point> validSurround = new ArrayList<>();
            for(int i = searchP; i < surround.size();) {
                Point p = surround.get(i);
                i++; searchP++;
                if(visitedPoints.contains(p)) {
                    if(checkShouldStop() || checkCacheOverSize()) return;
                    continue;
                }
                validSurround.add(p);
                if(checkShouldStop() || checkCacheOverSize()) return;
            }
            if(validSurround.isEmpty()) continue;
            nextSearch.addAll(validSurround);
            cache.addAll(validSurround);

            if(checkShouldStop() || checkCacheOverSize()) return;
        }
        curState = TaskState.End;
    }

    public List<Point> surroundPoints(Point p) {
        Point up = p.topPoint();
        Point down = p.bottomPoint();
        Point left = p.xMinusPoint();
        Point right = p.xPlusPoint();
        Point front = p.zPlusPoint();
        Point back = p.zMinusPoint();
        return Arrays.asList(up, down, left, right, front, back);
    }
}