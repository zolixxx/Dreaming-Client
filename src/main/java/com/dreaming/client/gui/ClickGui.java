package com.dreaming.client.gui;

import com.dreaming.client.DreamingClient;
import com.dreaming.client.module.Category;
import com.dreaming.client.module.Module;
import com.dreaming.client.module.ModuleManager;
import com.dreaming.client.modules.GuiSettingsModule;
import com.dreaming.client.setting.BoolSetting;
import com.dreaming.client.setting.NumSetting;
import com.dreaming.client.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/** Large animated menu: sidebar tabs, search, scrolling card grid, per-module settings. */
public class ClickGui extends Screen {
    private static final int TAB_H = 22, SIDE = 100, HEAD_H = 34, FOOT_H = 18, CARD_HEAD = 24, GAP = 6, MIN_CARD_W = 150;
    private static final int BG = 0xF2100D1E, PANEL = 0xFF0C0919, CARD = 0xFF1A1632, CARD_HOT = 0xFF272152;
    private static final int TRACK = 0xFF2E2A4A, GRAY = 0xFF9A96B8, WHITE = 0xFFFFFFFF;

    private static Category lastTab = Category.RENDER;

    private static final class Particle { float x, y, vy, size, phase; }

    private static final class CardAnim {
        final Smooth hover = new Smooth(0, 14), enable = new Smooth(0, 12),
                expand = new Smooth(0, 12), pulse = new Smooth(0, 5);
        final Map<Setting, Smooth> sets = new HashMap<>();
    }

    private static final class Card { Module m; CardAnim a; int x, y, w, h, index; }

    private static final Map<Module, CardAnim> ANIMS = new HashMap<>();

    private final Smooth open = new Smooth(0, 9), tabBar = new Smooth(0, 14), tabFade = new Smooth(1, 6),
            scroll = new Smooth(0, 14), focus = new Smooth(0, 14);
    private final Map<Category, Smooth> tabHover = new EnumMap<>(Category.class);
    private final List<Particle> particles = new ArrayList<>();
    private final Random rng = new Random();

    private Category tab = lastTab;
    private String search = "";
    private boolean searchFocus, closing;
    private float scrollTarget, maxScroll;
    private long last = System.nanoTime(), start = System.nanoTime();

    private NumSetting activeSlider;
    private int sliderX, sliderW;

    // window geometry (recomputed every frame)
    private int W, H, wx, wy, cx0, cy0, cx1, cy1, tabsTop;
    private float cx, cy, sc = 1f;

    public ClickGui() {
        super(Text.literal("Dreaming Client"));
        for (Category c : Category.values()) tabHover.put(c, new Smooth(0, 14));
    }

    @Override
    protected void init() {
        if (particles.isEmpty()) {
            for (int i = 0; i < 46; i++) {
                Particle p = new Particle();
                p.x = rng.nextFloat() * width; p.y = rng.nextFloat() * height;
                p.vy = 5 + rng.nextFloat() * 14; p.size = rng.nextFloat() < 0.2f ? 2 : 1;
                p.phase = rng.nextFloat() * 6.28f;
                particles.add(p);
            }
        }
        computeWindow();
        tabBar.snap(tabsTop + tab.ordinal() * TAB_H);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) {}

    @Override
    public void close() { closing = true; }

    // ---------------------------------------------------------------- helpers

    private void computeWindow() {
        W = Math.min(Math.min(width - 16, 780), Math.max(380, (int) (width * 0.8)));
        H = Math.min(Math.min(height - 16, 480), Math.max(240, (int) (height * 0.82)));
        wx = (width - W) / 2; wy = (height - H) / 2;
        cx = width / 2f; cy = height / 2f;
        cx0 = wx + SIDE + 10; cy0 = wy + HEAD_H + 4;
        cx1 = wx + W - 10; cy1 = wy + H - FOOT_H - 4;
        tabsTop = wy + 58;
        float t = Draw.clamp01(open.cur) - 1f;
        float back = 1f + 2.70158f * t * t * t + 1.70158f * t * t; // ease-out-back
        sc = 0.9f + 0.1f * back;
    }

    private double tmx(double mx) { return cx + (mx - cx) / sc; }
    private double tmy(double my) { return cy + (my - cy) / sc; }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int accent(float hueOffset) {
        float h = (float) GuiSettingsModule.INSTANCE.hue.value + hueOffset;
        h = ((h % 360f) + 360f) % 360f;
        return 0xFF000000 | MathHelper.hsvToRgb(h / 360f, 0.62f, 1.0f);
    }

    private CardAnim anim(Module m) {
        return ANIMS.computeIfAbsent(m, k -> {
            CardAnim a = new CardAnim();
            a.enable.snap(k.enabled ? 1 : 0);
            a.expand.snap(k.expanded || k.settingsOnly ? 1 : 0);
            for (Setting s : k.settings) {
                float v = s instanceof BoolSetting b ? (b.value ? 1 : 0) : (float) ((NumSetting) s).fraction();
                a.sets.put(s, new Smooth(v, 14));
            }
            return a;
        });
    }

    private static int rowH(Setting s) { return s instanceof NumSetting ? 20 : 14; }

    private static int settingsHeight(Module m) {
        int h = 0;
        for (Setting s : m.settings) h += rowH(s);
        return h;
    }

    private List<Module> visibleModules() {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : ModuleManager.all()) {
            if (q.isEmpty() ? m.category == tab : m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }

    private List<Card> layout() {
        List<Module> mods = visibleModules();
        int cw = cx1 - cx0;
        int cols = Math.max(1, (cw + GAP) / (MIN_CARD_W + GAP));
        int cardW = (cw - (cols - 1) * GAP) / cols;
        int[] colH = new int[cols];
        List<Card> out = new ArrayList<>();
        int i = 0;
        for (Module m : mods) {
            CardAnim a = anim(m);
            int col = i % cols;
            Card c = new Card();
            c.m = m; c.a = a; c.index = i++;
            c.w = cardW;
            c.h = CARD_HEAD + Math.round(a.expand.cur * (settingsHeight(m) + 4));
            c.x = cx0 + col * (cardW + GAP);
            c.y = cy0 + colH[col] - Math.round(scroll.cur);
            colH[col] += c.h + GAP;
            out.add(c);
        }
        int contentH = 0;
        for (int h : colH) contentH = Math.max(contentH, h);
        maxScroll = Math.max(0, contentH - (cy1 - cy0));
        return out;
    }

    private void switchTab(Category c) {
        if (c == tab && search.isEmpty()) return;
        tab = c; lastTab = c; search = "";
        tabFade.snap(0); tabFade.target = 1;
        scrollTarget = 0; scroll.snap(0);
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1_000_000_000f);
        last = now;
        GuiSettingsModule gs = GuiSettingsModule.INSTANCE;
        float sdt = dt * (float) gs.speed.value;
        float time = (now - start) / 1_000_000_000f;

        open.target = closing ? 0 : 1;
        open.update(sdt);
        if (closing && open.cur < 0.03f) { client.setScreen(null); return; }

        computeWindow();
        scrollTarget = MathHelper.clamp(scrollTarget, 0, maxScroll);
        scroll.target = scrollTarget;
        scroll.update(sdt); tabBar.update(sdt); tabFade.update(sdt);
        focus.target = searchFocus ? 1 : 0; focus.update(sdt);
        tabBar.target = tabsTop + tab.ordinal() * TAB_H;

        float ga = Draw.clamp01(open.cur);
        int accent = accent(0), accent2 = accent(55);
        double tx = tmx(mx), ty = tmy(my);

        // backdrop
        if (gs.dim.value) {
            ctx.fillGradient(0, 0, width, height, Draw.a(0xFF0B0718, 0.80f * ga), Draw.a(0xFF1B0F35, 0.62f * ga));
        }
        if (gs.particles.value) {
            for (Particle p : particles) {
                p.y -= p.vy * sdt;
                if (p.y < -4) { p.y = height + 4; p.x = rng.nextFloat() * width; }
                float tw = 0.5f + 0.5f * (float) Math.sin(time * 1.6f + p.phase);
                int s = (int) p.size;
                int px = Math.round(p.x + (float) Math.sin(time * 0.6f + p.phase) * 6f), py = Math.round(p.y);
                ctx.fill(px, py, px + s, py + s, Draw.a(0xFFD8CCFF, (0.12f + 0.4f * tw) * ga));
            }
        }

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0);
        ms.scale(sc, sc, 1f);
        ms.translate(-cx, -cy, 0);

        // window: soft shadow, animated gradient border, body
        for (int i = 4; i >= 1; i--) Draw.rrect(ctx, wx - i * 2, wy - i * 2, W + i * 4, H + i * 4, 8 + i, Draw.a(0xFF000000, 0.07f * ga));
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 1.5f);
        Draw.rrect(ctx, wx - 1, wy - 1, W + 2, H + 2, 7, Draw.a(Draw.mix(accent, accent2, pulse), 0.6f * ga));
        Draw.rrect(ctx, wx, wy, W, H, 6, Draw.a(BG, ga));

        // sidebar
        Draw.rrect(ctx, wx + 4, wy + 4, SIDE - 6, H - 8, 5, Draw.a(PANEL, ga));
        float lx = wx + 14;
        String logo = "Dreaming";
        for (int i = 0; i < logo.length(); i++) {
            String ch = String.valueOf(logo.charAt(i));
            float h = ((float) gs.hue.value + i * 14 + time * 45f) % 360f;
            int col = 0xFF000000 | MathHelper.hsvToRgb(h / 360f, 0.55f, 1f);
            Draw.text(ctx, textRenderer, ch, lx, wy + 13 + (float) Math.sin(time * 2 + i * 0.6f) * 1.2f, Draw.a(col, ga), 1.4f);
            lx += textRenderer.getWidth(ch) * 1.4f;
        }
        Draw.text(ctx, textRenderer, "C L I E N T", wx + 15, wy + 30, Draw.a(GRAY, ga), 0.75f);

        // tab indicator + tabs
        int ti = Math.round(tabBar.cur);
        Draw.rrect(ctx, wx + 8, ti, SIDE - 14, TAB_H - 2, 4, Draw.a(accent, 0.22f * ga));
        Draw.rrect(ctx, wx + 8, ti + 3, 2, TAB_H - 8, 1, Draw.a(accent, ga));
        int ci = 0;
        for (Category c : Category.values()) {
            int ty0 = tabsTop + ci * TAB_H;
            Smooth hv = tabHover.get(c);
            hv.target = in(tx, ty, wx + 8, ty0, SIDE - 14, TAB_H - 2) ? 1 : 0;
            hv.update(sdt);
            boolean sel = c == tab && search.isEmpty();
            int tc = Draw.mix(GRAY, WHITE, sel ? 1 : hv.cur);
            Draw.text(ctx, textRenderer, c.label, wx + 16 + hv.cur * 2f, ty0 + 7, Draw.a(tc, ga), 1f);
            String cnt = String.valueOf(ModuleManager.in(c).size());
            Draw.text(ctx, textRenderer, cnt, wx + SIDE - 14 - textRenderer.getWidth(cnt) * 0.8f, ty0 + 8, Draw.a(GRAY, ga * 0.8f), 0.8f);
            ci++;
        }
        Draw.text(ctx, textRenderer, "v" + DreamingClient.VERSION, wx + 14, wy + H - 16, Draw.a(0xFF555077, ga), 0.8f);

        // search bar
        int sbx = wx + SIDE + 10, sby = wy + 9, sbw = W - SIDE - 20;
        Draw.rrect(ctx, sbx - 1, sby - 1, sbw + 2, 20, 5, Draw.a(accent, (0.12f + 0.55f * focus.cur) * ga));
        Draw.rrect(ctx, sbx, sby, sbw, 18, 4, Draw.a(0xFF171330, ga));
        boolean caret = searchFocus && ((int) (time * 2.2f) % 2 == 0);
        if (search.isEmpty() && !searchFocus) Draw.text(ctx, textRenderer, "Search modules...", sbx + 8, sby + 5, Draw.a(0xFF5E5A80, ga), 1f);
        else Draw.text(ctx, textRenderer, search + (caret ? "|" : ""), sbx + 8, sby + 5, Draw.a(WHITE, ga), 1f);

        // cards
        List<Card> cards = layout();
        boolean clip = open.cur > 0.97f;
        if (clip) ctx.enableScissor(cx0 - 2, cy0, cx1 + 2, cy1);
        for (Card c : cards) {
            float ca = Draw.clamp01(tabFade.cur * 1.7f - c.index * 0.12f);
            drawCard(ctx, c, ca * ga, Math.round((1 - ca) * 10), tx, ty, sdt, accent, time);
        }
        if (cards.isEmpty()) {
            Draw.text(ctx, textRenderer, "No modules match your search.", cx0 + 4, cy0 + 8, Draw.a(GRAY, ga), 1f);
        }
        if (clip) ctx.disableScissor();

        // scrollbar
        if (maxScroll > 0) {
            int trackH = cy1 - cy0;
            int barH = Math.max(14, (int) (trackH * trackH / (trackH + maxScroll)));
            int barY = cy0 + (int) ((trackH - barH) * (scroll.cur / maxScroll));
            Draw.rrect(ctx, cx1 + 4, barY, 2, barH, 1, Draw.a(accent, 0.55f * ga));
        }

        // footer
        boolean ok = DreamingClient.allowed();
        float dot = 0.55f + 0.45f * (float) Math.sin(time * 3f);
        int dc = ok ? 0xFF4ADE80 : 0xFFF87171;
        Draw.rrect(ctx, cx0, wy + H - 13, 5, 5, 2, Draw.a(dc, dot * ga));
        Draw.text(ctx, textRenderer, ok ? "Singleplayer - modules active" : "Not in singleplayer - modules locked",
                cx0 + 10, wy + H - 14, Draw.a(GRAY, ga), 0.85f);
        int on = 0;
        for (Module m : ModuleManager.all()) if (m.enabled && !m.settingsOnly) on++;
        String right = on + " enabled  |  Right Shift to close";
        Draw.text(ctx, textRenderer, right, cx1 - textRenderer.getWidth(right) * 0.85f, wy + H - 14, Draw.a(0xFF6C678F, ga), 0.85f);

        ms.pop();
    }

    private void drawCard(DrawContext ctx, Card c, float ca, int yOff, double tx, double ty, float dt, int accent, float time) {
        Module m = c.m;
        CardAnim a = c.a;
        int x = c.x, y = c.y + yOff, w = c.w, h = c.h;

        boolean visible = ty >= cy0 && ty < cy1;
        a.hover.target = visible && in(tx, ty, x, y, w, CARD_HEAD) ? 1 : 0;
        a.enable.target = m.enabled ? 1 : 0;
        a.expand.target = (m.expanded || m.settingsOnly) ? 1 : 0;
        a.hover.update(dt); a.enable.update(dt); a.expand.update(dt); a.pulse.update(dt);
        for (Setting s : m.settings) {
            Smooth sm = a.sets.get(s);
            sm.target = s instanceof BoolSetting b ? (b.value ? 1 : 0) : (float) ((NumSetting) s).fraction();
            sm.update(dt);
        }

        float en = a.enable.cur;
        if (en > 0.01f || a.hover.cur > 0.01f)
            Draw.rrect(ctx, x - 1, y - 1, w + 2, h + 2, 5, Draw.a(accent, (0.08f + 0.32f * en + 0.12f * a.hover.cur) * ca));
        Draw.rrect(ctx, x, y, w, h, 4, Draw.a(Draw.mix(CARD, CARD_HOT, a.hover.cur), ca));
        Draw.rrect(ctx, x, y, w, CARD_HEAD, 4, Draw.a(accent, a.pulse.cur * 0.35f * ca));
        if (en > 0.01f && !m.settingsOnly) Draw.rrect(ctx, x, y + 4, 2, CARD_HEAD - 8, 1, Draw.a(accent, en * ca));

        int nameCol = Draw.mix(GRAY, WHITE, m.settingsOnly ? 1 : Math.max(en, a.hover.cur * 0.6f));
        Draw.text(ctx, textRenderer, m.name, x + 9 + a.hover.cur, y + 4, Draw.a(nameCol, ca), 1f);
        String desc = textRenderer.trimToWidth(m.description, (int) ((w - 62) / 0.75f));
        Draw.text(ctx, textRenderer, desc, x + 9, y + 15, Draw.a(0xFF6C678F, ca), 0.75f);

        if (!m.settingsOnly) {
            int sx = x + w - 30, sy = y + 7;
            Draw.rrect(ctx, sx, sy, 22, 10, 5, Draw.a(Draw.mix(TRACK, accent, en), ca));
            Draw.rrect(ctx, sx + 1 + Math.round(en * 12), sy + 1, 8, 8, 4, Draw.a(WHITE, ca));
            if (!m.settings.isEmpty())
                Draw.text(ctx, textRenderer, a.expand.cur > 0.5f ? "-" : "+", x + w - 43, y + 8, Draw.a(GRAY, ca), 1f);
        }

        if (a.expand.cur > 0.02f && !m.settings.isEmpty()) {
            ctx.enableScissor(x, y + CARD_HEAD - 1, x + w, y + h);
            float sa = ca * a.expand.cur;
            Draw.rrect(ctx, x + 5, y + CARD_HEAD - 1, w - 10, 1, 0, Draw.a(TRACK, sa));
            int ry = y + CARD_HEAD + 2;
            for (Setting s : m.settings) {
                float v = a.sets.get(s).cur;
                if (s instanceof BoolSetting b) {
                    Draw.text(ctx, textRenderer, b.name, x + 9, ry + 3, Draw.a(0xFFCCCCDD, sa), 1f);
                    int bx = x + w - 30;
                    Draw.rrect(ctx, bx, ry + 2, 20, 9, 4, Draw.a(Draw.mix(TRACK, accent, v), sa));
                    Draw.rrect(ctx, bx + 1 + Math.round(v * 11), ry + 3, 7, 7, 3, Draw.a(WHITE, sa));
                } else if (s instanceof NumSetting n) {
                    String val = n.step >= 1 ? String.valueOf((int) n.value) : String.format("%.1f", n.value);
                    Draw.text(ctx, textRenderer, n.name, x + 9, ry + 1, Draw.a(0xFFCCCCDD, sa), 1f);
                    Draw.text(ctx, textRenderer, val, x + w - 9 - textRenderer.getWidth(val), ry + 1, Draw.a(accent, sa), 1f);
                    int tw = w - 16, fw = Math.round(tw * v);
                    Draw.rrect(ctx, x + 8, ry + 12, tw, 3, 1, Draw.a(TRACK, sa));
                    Draw.rrect(ctx, x + 8, ry + 12, Math.max(fw, 2), 3, 1, Draw.a(accent, sa));
                    Draw.rrect(ctx, x + 8 + fw - 2, ry + 10, 5, 7, 2, Draw.a(WHITE, sa));
                }
                ry += rowH(s);
            }
            ctx.disableScissor();
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double rawX, double rawY, int button) {
        computeWindow();
        double mx = tmx(rawX), my = tmy(rawY);

        int sbx = wx + SIDE + 10, sby = wy + 9, sbw = W - SIDE - 20;
        searchFocus = in(mx, my, sbx, sby, sbw, 18);
        if (searchFocus) return true;

        int ci = 0;
        for (Category c : Category.values()) {
            if (in(mx, my, wx + 8, tabsTop + ci * TAB_H, SIDE - 14, TAB_H - 2)) { switchTab(c); return true; }
            ci++;
        }

        if (my < cy0 || my >= cy1) return super.mouseClicked(rawX, rawY, button);

        for (Card c : layout()) {
            if (!in(mx, my, c.x, c.y, c.w, c.h)) continue;
            Module m = c.m;
            if (my < c.y + CARD_HEAD) {
                if (m.settingsOnly) return true;
                boolean chevron = !m.settings.isEmpty() && in(mx, my, c.x + c.w - 48, c.y, 16, CARD_HEAD);
                if (button == 0 && !chevron) { m.toggle(); c.a.pulse.snap(1); c.a.pulse.target = 0; }
                else if (button == 1 || chevron) { if (!m.settings.isEmpty()) m.expanded = !m.expanded; }
                return true;
            }
            if (c.a.expand.cur < 0.3f || button != 0) return true;
            int ry = c.y + CARD_HEAD + 2;
            for (Setting s : m.settings) {
                int rh = rowH(s);
                if (my >= ry && my < ry + rh) {
                    if (s instanceof BoolSetting b) b.value = !b.value;
                    else if (s instanceof NumSetting n) {
                        activeSlider = n; sliderX = c.x + 8; sliderW = c.w - 16;
                        n.setFromFraction((mx - sliderX) / sliderW);
                    }
                    return true;
                }
                ry += rh;
            }
            return true;
        }
        return super.mouseClicked(rawX, rawY, button);
    }

    @Override
    public boolean mouseDragged(double rawX, double rawY, int button, double dx, double dy) {
        if (activeSlider != null) {
            computeWindow();
            activeSlider.setFromFraction((tmx(rawX) - sliderX) / sliderW);
            return true;
        }
        return super.mouseDragged(rawX, rawY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double rawX, double rawY, int button) {
        activeSlider = null;
        return super.mouseReleased(rawX, rawY, button);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double hAmount, double vAmount) {
        scrollTarget = MathHelper.clamp(scrollTarget - (float) vAmount * TAB_H, 0, maxScroll);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if ((chr >= 32 && chr != 127 && chr != 167)) {
            search += chr; searchFocus = true;
            scrollTarget = 0; scroll.snap(0);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && searchFocus) { searchFocus = false; return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
