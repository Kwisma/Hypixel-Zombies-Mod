package com.example.client.data;

import java.util.Arrays;
import java.util.List;

/**
 * 道具（Powerup）回合预测，目前只含 Alien Arcadium。
 *
 * <p>原理：Hypixel 的 Insta Kill / Max Ammo / Shopping Spree 刷新是固定模式，
 * 每局随机选其中一套。两套（r2/r3，SS 是 r5/r6/r7）的回合表互不相交，
 * 所以本局第一次检测到某道具落在哪套表里，就锁定这一局该道具的全部回合。</p>
 *
 * <p>数据来自 Seosean/ShowSpawnTime。其它地图需要各自的回合表。</p>
 */
public final class PowerupPredictor {

    public enum Type { INSTA, MAX, SS }

    // AA 各模式回合表
    private static final List<Integer> INSTA_R2 = Arrays.asList(2, 5, 8, 11, 14, 17, 20, 23);
    private static final List<Integer> INSTA_R3 = Arrays.asList(3, 6, 9, 12, 15, 18, 21);
    private static final List<Integer> MAX_R2 = Arrays.asList(2, 5, 8, 12, 16, 21, 26, 31, 36, 41, 46, 51, 61, 66, 71, 76, 81, 86, 91, 96);
    private static final List<Integer> MAX_R3 = Arrays.asList(3, 6, 9, 13, 17, 22, 27, 32, 37, 42, 47, 52, 62, 67, 72, 77, 82, 87, 92, 97);
    private static final List<Integer> SS_R5 = Arrays.asList(5, 15, 45, 55, 65, 75, 85, 95, 105);
    private static final List<Integer> SS_R6 = Arrays.asList(6, 16, 26, 36, 46, 66, 76, 86, 96);
    private static final List<Integer> SS_R7 = Arrays.asList(7, 17, 27, 37, 47, 67, 77, 87, 97);

    // 本局锁定的模式（null = 还没识别出来）
    private List<Integer> instaPattern;
    private List<Integer> maxPattern;
    private List<Integer> ssPattern;

    /** 进入/离开一局时清空。 */
    public void reset() {
        instaPattern = null;
        maxPattern = null;
        ssPattern = null;
    }

    /**
     * 检测到一个道具时调用。第一次检测到该类型就锁定本局模式。
     *
     * @param round          检测到时的回合号
     * @param roundElapsedMs 回合开始至今毫秒（处理"跨回合"：道具在新回合开头掉，其实属于上一回合）
     */
    public void onDetect(Type type, int round, long roundElapsedMs) {
        boolean early = roundElapsedMs <= 1000; // 回合刚开就掉 → 可能属于上一回合
        switch (type) {
            case INSTA -> { if (instaPattern == null) instaPattern = match(round, early, INSTA_R2, INSTA_R3); }
            case MAX   -> { if (maxPattern == null)   maxPattern   = match(round, early, MAX_R2, MAX_R3); }
            case SS    -> { if (ssPattern == null)     ssPattern    = matchSS(round, early); }
        }
    }

    /** 在两套候选表里定位：直接命中优先；命中不上且在回合开头，按"上一回合"再试（跨回合）。 */
    private static List<Integer> match(int round, boolean early, List<Integer> a, List<Integer> b) {
        if (a.contains(round)) return a;
        if (b.contains(round)) return b;
        if (early) {
            if (a.contains(round - 1)) return a;
            if (b.contains(round - 1)) return b;
        }
        return null; // 没识别出来（中途进的局、漏了，等下次检测再锁）
    }

    private static List<Integer> matchSS(int round, boolean early) {
        for (List<Integer> p : List.of(SS_R5, SS_R6, SS_R7)) {
            if (p.contains(round)) return p;
        }
        if (early) {
            for (List<Integer> p : List.of(SS_R5, SS_R6, SS_R7)) {
                if (p.contains(round - 1)) return p;
            }
        }
        return null;
    }

    // ===== 查询 =====

    /** 该道具的模式是否已锁定（锁定后才能预测）。 */
    public boolean isLocked(Type type) {
        return pattern(type) != null;
    }

    /** 该回合是否会出这种道具；模式没锁定返回 false。 */
    public boolean isPowerupRound(Type type, int round) {
        List<Integer> p = pattern(type);
        return p != null && p.contains(round);
    }

    /** 下一个该道具的回合（fromRound 之后）；没有/没锁定返回 -1。 */
    public int nextRound(Type type, int fromRound) {
        List<Integer> p = pattern(type);
        if (p == null) return -1;
        for (int r : p) {
            if (r > fromRound) return r;
        }
        return -1;
    }

    private List<Integer> pattern(Type type) {
        return switch (type) {
            case INSTA -> instaPattern;
            case MAX -> maxPattern;
            case SS -> ssPattern;
        };
    }
}
