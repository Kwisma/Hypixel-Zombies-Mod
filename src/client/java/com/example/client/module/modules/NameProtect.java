package com.example.client.module.modules;

import com.example.client.config.ZombiesConfig;
import com.example.client.gui.NameProtectScreen;
import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 名字保护：把保护名单里的玩家名字显示成 Player1/Player2…，也可逐个自定义别名、颜色和 Hypixel 前缀。
 * 本模块作为一次性入口——开启即打开 GUI 再自动关闭。
 * 名字渲染替换在别处用 {@link #display(String)} 接入。
 */
@ModuleInfo(name = {
        @Text(label = "Name Protect", language = Language.English),
        @Text(label = "名字保护", language = Language.Chinese)
}, enable = false)
public class NameProtect extends AbstractModule {

    @Override
    protected void onEnable() {
        if (mc.player == null || mc.level == null) return;
        toggle();
        mc.setScreen(new NameProtectScreen(null));
        super.onEnable();
    }

    /** 这个名字（已去色码）是否被保护。 */
    public static boolean isProtected(String cleanName) {
        if (cleanName == null || cleanName.isEmpty()) return false;
        for (String n : ZombiesConfig.protectedNames) {
            if (n.equalsIgnoreCase(cleanName)) return true;
        }
        return false;
    }

    /** 被保护的名字返回配置别名（默认 PlayerN），否则原样返回。 */
    public static String display(String cleanName) {
        if (cleanName == null) return null;
        List<String> list = ZombiesConfig.protectedNames;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(cleanName)) {
                return aliasFor(list.get(i), i);
            }
        }

        return cleanName;
    }

    public static String defaultAlias(int index) {
        return "Player" + (index + 1);
    }

    public static String aliasFor(String protectedName, int index) {
        ZombiesConfig.NameProtectSettings settings = ZombiesConfig.findNameProtectSettings(protectedName);
        if (settings != null && !settings.isRenameName()) {
            return protectedName;
        }
        if (settings == null || settings.getCustomName().isBlank()) {
            return defaultAlias(index);
        }
        return settings.getCustomName();
    }

    /**
     * 中央替换：任意要渲染的字符串里，只要命中保护名就替换别名。
     * 如果这个名字紧跟 Hypixel 的 [RANK] 前缀，也会按该条目的前缀设置一并替换。
     * 渲染文字的入口（Font）统一调它即可。名单为空时原样返回（零开销）。
     */
    /** 管理界面本身要显示真名，打开它时跳过替换。 */
    private static boolean bypass() {
        return Minecraft.getInstance().screen instanceof NameProtectScreen;
    }

    public static String apply(String text) {
        if (text == null || text.isEmpty() || bypass()) return text;
        List<String> list = ZombiesConfig.protectedNames;
        if (list.isEmpty()) return text;

        return replacePlain(text, list);
    }

    // ====== 保留样式（颜色）的替换：Component / FormattedCharSequence ======

    private record Run(String text, Style style) {}
    private record RankMatch(int length, int protectedNameIndex) {}

    /** Component 版：命中则按样式分段重建（别名套用被换名字的颜色），否则原样返回。 */
    public static Component protect(Component comp) {
        if (comp == null || ZombiesConfig.protectedNames.isEmpty() || bypass()) return comp;
        List<Run> runs = replaceStyled(comp.getVisualOrderText());
        if (runs == null) return comp;
        MutableComponent out = Component.empty();
        for (Run r : runs) {
            out.append(Component.literal(r.text()).setStyle(r.style()));
        }
        return out;
    }

    /** FormattedCharSequence 版（聊天/计分板/TAB 等）。 */
    public static FormattedCharSequence protect(FormattedCharSequence fcs) {
        if (fcs == null || ZombiesConfig.protectedNames.isEmpty() || bypass()) return fcs;
        List<Run> runs = replaceStyled(fcs);
        if (runs == null) return fcs;
        List<FormattedCharSequence> parts = new ArrayList<>(runs.size());
        for (Run r : runs) {
            parts.add(FormattedCharSequence.forward(r.text(), r.style()));
        }
        return FormattedCharSequence.composite(parts);
    }

    /**
     * 核心：把字形流拆成 (码点, 样式)，逐位匹配保护名；命中则插入别名（套用名字首字符的样式），
     * 其余字符保留各自样式。按样式合并成 Run。没命中返回 null（让调用方保留原对象+原样式）。
     */
    private static List<Run> replaceStyled(FormattedCharSequence fcs) {
        List<Integer> cps = new ArrayList<>();
        List<Style> sty = new ArrayList<>();
        fcs.accept((position, style, codePoint) -> {
            cps.add(codePoint);
            sty.add(style);
            return true;
        });

        List<String> names = ZombiesConfig.protectedNames;
        List<Run> runs = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        Style[] curStyle = {null};
        boolean changed = false;

        int i = 0;
        int len = cps.size();
        while (i < len) {
            RankMatch rankMatch = findRankMatch(cps, i, names);
            if (rankMatch != null) {
                int hit = rankMatch.protectedNameIndex();
                ZombiesConfig.NameProtectSettings settings = ZombiesConfig.findNameProtectSettings(names.get(hit));
                String prefix = settings == null ? "" : settings.getPrefix();
                pushPrefix(runs, cur, curStyle, normalizedPrefix(prefix), settings, sty.get(i));
                pushChar(runs, cur, curStyle, ' ', sty.get(i));
                pushAlias(runs, cur, curStyle, names.get(hit), hit, sty.get(i + rankMatch.length() - names.get(hit).length()));
                i += rankMatch.length();
                changed = true;
                continue;
            }

            int hit = findNameMatch(cps, i, names);
            if (hit >= 0) {
                pushAlias(runs, cur, curStyle, names.get(hit), hit, sty.get(i));
                i += names.get(hit).length();
                changed = true;
            } else {
                pushCp(runs, cur, curStyle, cps.get(i), sty.get(i));
                i++;
            }
        }
        if (!changed) return null;
        if (cur.length() > 0) runs.add(new Run(cur.toString(), curStyle[0]));
        return runs;
    }

    private static boolean matchesAt(List<Integer> cps, int i, String n) {
        if (n == null || n.isEmpty() || i + n.length() > cps.size()) return false;
        for (int j = 0; j < n.length(); j++) {
            if (cps.get(i + j) != n.charAt(j)) return false; // 名字为 ASCII，码点==char
        }
        return true;
    }

    private static int findNameMatch(List<Integer> cps, int index, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            if (matchesAt(cps, index, names.get(i))) return i;
        }
        return -1;
    }

    /** 从 '[' 开始识别 [VIP] Name 这类 Hypixel rank 前缀；只匹配已设置新前缀的条目。 */
    private static RankMatch findRankMatch(List<Integer> cps, int index, List<String> names) {
        if (index >= cps.size() || cps.get(index) != (int) '[') return null;
        int close = -1;
        int max = Math.min(cps.size(), index + 33);
        for (int i = index + 1; i < max; i++) {
            if (cps.get(i) == (int) ']') {
                close = i;
                break;
            }
        }
        if (close < 0) return null;

        int nameStart = close + 1;
        while (nameStart < cps.size() && Character.isWhitespace(cps.get(nameStart))) nameStart++;
        for (int i = 0; i < names.size(); i++) {
            ZombiesConfig.NameProtectSettings settings = ZombiesConfig.findNameProtectSettings(names.get(i));
            if (settings != null && !settings.getPrefix().isBlank() && matchesAt(cps, nameStart, names.get(i))) {
                return new RankMatch(nameStart + names.get(i).length() - index, i);
            }
        }
        return null;
    }

    private static void pushAlias(List<Run> runs, StringBuilder cur, Style[] curStyle,
                                  String protectedName, int index, Style originalStyle) {
        ZombiesConfig.NameProtectSettings settings = ZombiesConfig.findNameProtectSettings(protectedName);
        Style aliasStyle = originalStyle;
        if (settings != null && settings.getNameColor() >= 0) {
            aliasStyle = originalStyle.withColor(settings.getNameColor());
        }
        pushText(runs, cur, curStyle, aliasFor(protectedName, index), aliasStyle);
    }

    private static void pushText(List<Run> runs, StringBuilder cur, Style[] curStyle, String text, Style style) {
        text.codePoints().forEach(codePoint -> pushCp(runs, cur, curStyle, codePoint, style));
    }

    /** 自定义 rank 中只有括号和加号使用独立颜色，字母部分保留服务器原 rank 颜色。 */
    private static void pushPrefix(List<Run> runs, StringBuilder cur, Style[] curStyle, String prefix,
                                   ZombiesConfig.NameProtectSettings settings, Style originalStyle) {
        for (int codePoint : prefix.codePoints().toArray()) {
            Style style = originalStyle;
            if ((codePoint == '[' || codePoint == ']') && settings.getBracketColor() >= 0) {
                style = originalStyle.withColor(settings.getBracketColor());
            } else if (codePoint == '+' && settings.getPlusColor() >= 0) {
                style = originalStyle.withColor(settings.getPlusColor());
            } else if (settings.getRankTextColor() >= 0) {
                style = originalStyle.withColor(settings.getRankTextColor());
            }
            pushCp(runs, cur, curStyle, codePoint, style);
        }
    }

    private static String replacePlain(String text, List<String> names) {
        StringBuilder out = new StringBuilder(text.length());
        for (int index = 0; index < text.length();) {
            RankMatch rank = findRankMatch(text, index, names);
            if (rank != null) {
                int hit = rank.protectedNameIndex();
                ZombiesConfig.NameProtectSettings settings = ZombiesConfig.findNameProtectSettings(names.get(hit));
                out.append(normalizedPrefix(settings.getPrefix())).append(' ').append(aliasFor(names.get(hit), hit));
                index += rank.length();
                continue;
            }

            int hit = findNameMatch(text, index, names);
            if (hit >= 0) {
                out.append(aliasFor(names.get(hit), hit));
                index += names.get(hit).length();
                continue;
            }

            int codePoint = text.codePointAt(index);
            out.appendCodePoint(codePoint);
            index += Character.charCount(codePoint);
        }
        return out.toString();
    }

    private static RankMatch findRankMatch(String text, int index, List<String> names) {
        if (index >= text.length() || text.charAt(index) != '[') return null;
        int close = text.indexOf(']', index + 1);
        if (close < 0 || close - index > 32) return null;

        int nameStart = close + 1;
        while (nameStart < text.length() && Character.isWhitespace(text.charAt(nameStart))) nameStart++;
        for (int i = 0; i < names.size(); i++) {
            ZombiesConfig.NameProtectSettings settings = ZombiesConfig.findNameProtectSettings(names.get(i));
            if (settings != null && !settings.getPrefix().isBlank()
                    && startsWithIgnoreCase(text, nameStart, names.get(i))) {
                return new RankMatch(nameStart + names.get(i).length() - index, i);
            }
        }
        return null;
    }

    private static int findNameMatch(String text, int index, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            if (startsWithIgnoreCase(text, index, names.get(i))) return i;
        }
        return -1;
    }

    private static boolean startsWithIgnoreCase(String text, int index, String target) {
        return target != null && !target.isEmpty()
                && index + target.length() <= text.length()
                && text.regionMatches(true, index, target, 0, target.length());
    }

    private static String normalizedPrefix(String prefix) {
        String value = prefix == null ? "" : prefix.trim();
        if (value.isEmpty()) return "";
        return value.startsWith("[") && value.endsWith("]") ? value : "[" + value + "]";
    }

    private static void pushChar(List<Run> runs, StringBuilder cur, Style[] curStyle, char ch, Style s) {
        if (curStyle[0] != null && !Objects.equals(curStyle[0], s)) {
            runs.add(new Run(cur.toString(), curStyle[0]));
            cur.setLength(0);
        }
        curStyle[0] = s;
        cur.append(ch);
    }

    private static void pushCp(List<Run> runs, StringBuilder cur, Style[] curStyle, int cp, Style s) {
        if (curStyle[0] != null && !Objects.equals(curStyle[0], s)) {
            runs.add(new Run(cur.toString(), curStyle[0]));
            cur.setLength(0);
        }
        curStyle[0] = s;
        cur.appendCodePoint(cp);
    }
}
