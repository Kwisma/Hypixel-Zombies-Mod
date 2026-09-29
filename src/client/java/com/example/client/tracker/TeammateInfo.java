package com.example.client.tracker;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;

@Getter
@Setter
public class TeammateInfo {
    public enum PlayerState {
        ALIVE,
        DOWN,
        TERMINAL
    }

    /** 主键：计分板里的真实用户名（语言无关，倒地也不会变） */
    private final String name;
    /** 计分板里的金币数 */
    private long gold;
    /** 倒地状态：来自 TAB 判定，单个实体读不出来，必须存 */
    private boolean isDown;
    /** 计分板玩家状态；DEAD/QUIT 不允许被当作可救援的倒地。 */
    private PlayerState playerState = PlayerState.ALIVE;
    /** 计分板右侧原始状态文字，例如 DEAD / QUIT / REVIVE。 */
    private String statusText = "";
    /** 是否正在被救（倒地者头顶 holo 有计时、且不含 SHIFT 提示）。 */
    private boolean beingRevived;
    /** 正在被救时，剩余多少秒救起（来自 holo 计时 X.Xs）。 */
    private double reviveSeconds;

    /** 最后已知坐标（活着时持续更新；倒地后人不动，用它定位救援 holo）。 */
    private double lastX, lastY, lastZ;
    private boolean posKnown;

    /** 活着时记录的皮肤标识（用于完全重叠时按皮肤对应倒地假人）。 */
    private String skin;

    public void setLastPos(double x, double y, double z) {
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.posKnown = true;
    }
    /**
     * 当前关联的实体：活着=真玩家，倒地=Hypixel 假人，离屏=null。
     * 血量/护甲/潜行/格挡等都直接从它现读，不再做快照。
     */
    private transient Player renderEntity;

    /** 快速复活窗口结束的时间戳（毫秒）；被救起时设为 now+5000。0/过期 = 无。 */
    private long fastReviveEndMs;

    public TeammateInfo(String name) {
        this.name = name;
    }

    /** 被救起时调用，开始 durationMs 毫秒的快速复活倒计时。 */
    public void startFastRevive(long durationMs) {
        this.fastReviveEndMs = System.currentTimeMillis() + durationMs;
    }

    public boolean isFastReviveActive() {
        return System.currentTimeMillis() < fastReviveEndMs;
    }

    /** 剩余秒数（>0）；没有或已过期返回 0。 */
    public double getFastReviveSecondsLeft() {
        long left = fastReviveEndMs - System.currentTimeMillis();
        return left > 0 ? left / 1000.0 : 0.0;
    }

    public void clearFastRevive() {
        fastReviveEndMs = 0L;
    }

    public boolean isTerminalState() {
        return playerState == PlayerState.TERMINAL;
    }

    public static TeammateInfo[] teammates = new TeammateInfo[0];
}
