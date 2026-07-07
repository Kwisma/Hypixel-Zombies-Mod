package com.example.client.tracker;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.example.client.data.ZombiesGuns;
import com.example.client.events.PacketEvent;
import com.example.client.events.TickEvent;
import com.example.client.utils.IMinecraft;
import com.example.client.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 弹药跟踪（本地模型 v3）：本地为主，服务器只在明确时机纠偏。
 *
 *  - 出站 UseItem（主手 + 手持是枪，限速）→ 本地弹药 -1（响应快，不被延迟拖）；
 *  - 入站 SetSlot/SetContent（containerId==0，hotbar i = 菜单槽 36+i）：
 *      · count 比上次"涨"了 → 补弹/换弹完成/MAX AMMO/木匠 → 本地设成新值（= 你要的"补满/填充"）；
 *      · 一段时间没开火（IDLE_MS）→ 完全信服务器值，清掉本地漂移；
 *      · 否则（正在连发，服务器只是慢慢确认）→ 保持本地领先，不拉回。
 *
 *  getRealAmmo(slot)：known ? local : -1（没见过该枪时不确定）
 *  isReloading(slot)：该槽枪 getDamageValue()>0 = 正在换弹
 */
public class AmmoTracker implements IMinecraft {

    private static final long MIN_SHOT_MS = 80;  // 两次预测扣弹的最小间隔（过滤超射速的右键狂点）
    private static final long IDLE_MS     = 300; // 停火多久后完全信服务器（纠偏）

    private static final int[]  local           = new int[9]; // 本地弹药
    private static final int[]  lastServerCount = new int[9]; // 上次服务器 count
    private static final int[]  magSize         = new int[9]; // 学到的满弹（最大见过的 count）
    private static final boolean[] known         = new boolean[9];
    private static final long[] lastShotMs       = new long[9];

    private int serverSelectedSlot = -1;

    public AmmoTracker() {
        EventManager.register(this);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        Packet<?> p = event.getPacket();

        if (p instanceof ServerboundSetCarriedItemPacket carried) {
            serverSelectedSlot = carried.getSlot();
            return;
        }

        if (p instanceof ServerboundUseItemPacket use) {
            if (use.getHand() != InteractionHand.MAIN_HAND) return;
            int s = serverSelectedSlot;
            if (s < 0 || s > 8 || mc.player == null) return;
            if (!ZombiesGuns.isZombiesGun(mc.player.getInventory().getItem(s))) return;

            long now = System.currentTimeMillis();
            if (now - lastShotMs[s] < MIN_SHOT_MS) return; // 限速
            lastShotMs[s] = now;

            if (known[s] && local[s] > 0) local[s]--;
            return;
        }

        if (p instanceof ClientboundContainerSetSlotPacket slot) {
            if (slot.getContainerId() != 0) return;
            int hot = slot.getSlot() - 36;
            if (hot < 0 || hot > 8) return;
            reconcile(hot, slot.getItem().getCount());
            return;
        }

        if (p instanceof ClientboundContainerSetContentPacket content) {
            if (content.containerId() != 0) return;
            List<ItemStack> items = content.items();
            for (int hot = 0; hot < 9; hot++) {
                int idx = 36 + hot;
                if (idx < items.size()) reconcile(hot, items.get(idx).getCount());
            }
        }
    }

    /** 服务器物品更新时的纠偏。 */
    private void reconcile(int hot, int count) {
        long now = System.currentTimeMillis();
        if (!known[hot]) {
            local[hot] = count;                       // 首次见到
        } else if (count > lastServerCount[hot]) {
            local[hot] = count;                       // 涨了 → 补弹/换弹完成/MAX AMMO → 补满
        } else if (now - lastShotMs[hot] > IDLE_MS) {
            local[hot] = count;                        // 停火 → 完全信服务器，清漂移
        }
        // 否则正在连发，保持本地领先

        if (count > magSize[hot]) magSize[hot] = count;
        lastServerCount[hot] = count;
        known[hot] = true;
    }

    /** 真实弹药；-1 = 还没见过该枪。 */
    public static int getRealAmmo(int slot) {
        if (slot < 0 || slot > 8 || !known[slot]) return -1;
        return local[slot];
    }

    /** 满弹（学到的）；-1 = 未知。 */
    public static int getMagSize(int slot) {
        if (slot < 0 || slot > 8 || magSize[slot] == 0) return -1;
        return magSize[slot];
    }

    /** 是否正在换弹（该槽枪 getDamageValue()>0）。 */
    public static boolean isReloading(int slot) {
        Minecraft m = Minecraft.getInstance();
        if (m.player == null || slot < 0 || slot > 8) return false;
        ItemStack s = m.player.getInventory().getItem(slot);
        return s.isDamageableItem() && s.getDamageValue() > 0;
    }

    // ---- 样本调试 ----
    private String last = "";

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        if (!PlayerUtils.isInHypZombies()) return;

        int s = serverSelectedSlot >= 0 ? serverSelectedSlot : mc.player.getInventory().getSelectedSlot();
        if (s < 0 || s > 8) return;

        ItemStack item = mc.player.getInventory().getItem(s);
        String now = "slot=" + s
                + " serverCount=" + item.getCount()
                + " LOCAL=" + getRealAmmo(s)
                + " mag=" + getMagSize(s)
                + " dmg=" + item.getDamageValue() + "/" + item.getMaxDamage()
                + " reloading=" + isReloading(s);
        if (!now.equals(last)) {
            last = now;
            System.out.println("[AMMO3] " + now);
        }
    }
}
