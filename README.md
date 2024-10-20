# 爆破连锁

## 一款更适合在GTNH挖矿的连锁MOD, 可以正确处理拥有相同矿词的矿物, 相较于已有的连锁模组, 增强了连锁相邻检测的范围, 此配置项可以在config/qz_miner.cfg中找到, 理论上可以一镐子挖爆不停.

目前有两种主要模式: 

* 连锁模式 - 主要逻辑为判断相邻方块进行挖掘

* 爆破模式 - 主要逻辑为以挖掘点为中心遍历一定范围的所有Point尝试挖掘

两种模式有对应的更细致逻辑, 主要是针对范围进行调整:

* 球形模式, 挖出一个球形的范围

* 矩形模式, 挖出一个立方体, 四四方方, 就是普通连锁的功能

* 平面模式, 以玩家脚底为基准, 挖掉上方的所有方块, 该模式目前只有矩形范围, 毕竟矩形更常见

## 更新日志

v1.0.0-alpha 当前版本已知的存在的致命bug, 由于没有做异步和多线程, 当连锁范围过大极有可能让 检索<->挖矿 两个相互作用的函数陷入死循环, 最终卡死逻辑服务器--具体逻辑为检索到一格后进行挖掘, 挖掘的同时检索下一个连锁位置列表, 但是如果方块未实际被挖掘掉会再次检索重复位置挖掘它.