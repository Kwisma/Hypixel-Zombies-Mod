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
    /** 快速复活窗口的总时长，用于 HUD 绘制准确的冷却进度。 */
    private long fastReviveDurationMs;

    public TeammateInfo(String name) {
        this.name = name;
    }

    /** 被救起时调用，开始 durationMs 毫秒的快速复活倒计时。 */
    public void startFastRevive(long durationMs) {
        this.fastReviveDurationMs = Math.max(0L, durationMs);
        this.fastReviveEndMs = System.currentTimeMillis() + fastReviveDurationMs;
    }

    public boolean isFastReviveActive() {
        return System.currentTimeMillis() < fastReviveEndMs;
    }

    /** 剩余秒数（>0）；没有或已过期返回 0。 */
    public double getFastReviveSecondsLeft() {
        long left = fastReviveEndMs - System.currentTimeMillis();
        return left > 0 ? left / 1000.0 : 0.0;
    }

    /** 快速复活冷却剩余比例：刚开始为 1，到期为 0。 */
    public float getFastReviveProgress() {
        if (fastReviveDurationMs <= 0L) return 0.0F;
        long left = Math.max(0L, fastReviveEndMs - System.currentTimeMillis());
        return Math.clamp((float) left / fastReviveDurationMs, 0.0F, 1.0F);
    }

    public void clearFastRevive() {
        fastReviveEndMs = 0L;
        fastReviveDurationMs = 0L;
    }

    public boolean isTerminalState() {
        return playerState == PlayerState.TERMINAL;
    }

    /**
     * 将计分板的状态应用到队友。QUIT/DEAD 不会被临时 REVIVE 行覆盖，
     * 但计分板恢复为金币数（ALIVE）时，说明该玩家已经重新加入，需要解除终止状态。
     */
    public void applyScoreboardState(PlayerState observedState, String observedStatusText) {
        if (observedState == PlayerState.TERMINAL) {
            playerState = PlayerState.TERMINAL;
            if (observedStatusText != null && !observedStatusText.isBlank()) {
                statusText = observedStatusText;
            }
            isDown = false;
            return;
        }

        if (playerState == PlayerState.TERMINAL) {
            if (observedState != PlayerState.ALIVE) {
                return;
            }
            clearTerminalAfterReconnect();
        }

        playerState = observedState;
        statusText = observedStatusText == null ? "" : observedStatusText;
        isDown = observedState == PlayerState.DOWN;
    }

    /** TAB 中重新出现同名真实玩家实体时调用，处理计分板状态仍滞后一两帧的重连。 */
    public void clearTerminalAfterReconnect() {
        playerState = PlayerState.ALIVE;
        statusText = "";
        isDown = false;
        beingRevived = false;
        reviveSeconds = 0.0;
        clearFastRevive();
    }

    public static TeammateInfo[] teammates = new TeammateInfo[0];
}
