package com.example.client.tracker;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;

@Getter
@Setter
public class TeammateInfo {
    /** 主键：计分板里的真实用户名（语言无关，倒地也不会变） */
    private final String name;
    /** 计分板里的金币数 */
    private long gold;
    /** 倒地状态：来自 TAB 判定，单个实体读不出来，必须存 */
    private boolean isDown;
    /**
     * 当前关联的实体：活着=真玩家，倒地=Hypixel 假人，离屏=null。
     * 血量/护甲/潜行/格挡等都直接从它现读，不再做快照。
     */
    private transient Player renderEntity;

    public TeammateInfo(String name) {
        this.name = name;
    }

    public static TeammateInfo[] teammates = new TeammateInfo[0];
}
