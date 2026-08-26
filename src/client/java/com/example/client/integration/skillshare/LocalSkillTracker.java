package com.example.client.integration.skillshare;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/** Observes Hypixel's fifth hotbar slot and exposes a protocol-ready snapshot. */
final class LocalSkillTracker {

    static final int SLOT_INDEX = 4;
    private static final long LR_COOLDOWN_MS = 20_000L;
    private static final long HEAL_COOLDOWN_MS = 30_000L;

    enum State { UNKNOWN, READY, COOLING }

    record Snapshot(State state, String name, int remainingSeconds, long releasedAtMs, long readyAtMs) {
        boolean known() {
            return name != null && state != State.UNKNOWN;
        }
    }

    private State state = State.UNKNOWN;
    private String skillName;
    private long releasedAtMs;
    private long readyAtMs;

    void observe(ItemStack stack, long now) {
        expire(now);
        if (stack == null || stack.isEmpty()) {
            return;
        }

        String canonical = canonicalName(stack.getHoverName().getString());
        if (canonical == null) {
            return;
        }

        boolean readyItem = stack.is(Items.BLAZE_ROD) || stack.is(Items.GOLDEN_APPLE);
        if (readyItem) {
            // Chat and inventory updates can arrive in either order. Do not erase
            // a just-observed release because the old ready item survived one tick.
            if (state == State.COOLING && now - releasedAtMs < 750L) {
                return;
            }
            state = State.READY;
            skillName = canonical;
            releasedAtMs = 0L;
            readyAtMs = 0L;
            return;
        }

        state = State.COOLING;
        skillName = canonical;
        if (releasedAtMs == 0L) {
            releasedAtMs = now;
        }
        int reportedSeconds = Math.max(1, stack.getCount());
        long reportedReadyAt = now + reportedSeconds * 1000L;
        if (readyAtMs <= now || Math.abs(readyAtMs - reportedReadyAt) > 1500L) {
            readyAtMs = reportedReadyAt;
        }
    }

    void markReleased(String canonical, long now) {
        skillName = canonical;
        state = State.COOLING;
        releasedAtMs = now;
        readyAtMs = now + cooldownFor(canonical);
    }

    Snapshot snapshot(long now) {
        expire(now);
        int remaining = state == State.COOLING
                ? (int) Math.max(0L, (readyAtMs - now + 999L) / 1000L)
                : 0;
        return new Snapshot(state, skillName, remaining, releasedAtMs,
                state == State.COOLING ? readyAtMs : 0L);
    }

    void reset() {
        state = State.UNKNOWN;
        skillName = null;
        releasedAtMs = 0L;
        readyAtMs = 0L;
    }

    private void expire(long now) {
        if (state == State.COOLING && readyAtMs > 0L && readyAtMs <= now) {
            state = skillName == null ? State.UNKNOWN : State.READY;
            readyAtMs = 0L;
        }
    }

    private static long cooldownFor(String canonical) {
        return "Heal".equals(canonical) ? HEAL_COOLDOWN_MS : LR_COOLDOWN_MS;
    }

    static String canonicalName(String raw) {
        if (raw == null) return null;
        String clean = raw.replaceAll("§.", "").trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.contains("lightning rod") || lower.contains("lighting rod")
                || clean.contains("闪电棒") || clean.contains("烈焰棒")) {
            return "Lightning Rod";
        }
        if (lower.equals("heal") || lower.contains("heal skill")
                || lower.contains("golden apple") || clean.contains("金苹果")) {
            return "Heal";
        }
        return null;
    }
}
