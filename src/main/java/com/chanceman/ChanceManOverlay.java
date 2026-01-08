package com.chanceman;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Renders and animates the ChanceMan rolling strip overlay.
 * <p>Flow:</p>
 * <ol>
 *   <li>{@link #startRollAnimation(int, int, Supplier)} primes the strip and starts the spin timer.</li>
 *   <li>{@link #render(Graphics2D)} scrolls items, snaps to a slot near the end, then highlights the winner.</li>
 *   <li>{@link #getFinalItem()} returns the centered item after snap/highlight.</li>
 * </ol>
 */
@Singleton
@Slf4j
public class ChanceManOverlay extends Overlay {
    // Snap behavior and timing
    private static final float SNAP_NEXT_THRESHOLD = 0.55f;
    private static final long SNAP_DURATION_MS = 350L;

    // Timing / motion
    private static final long HIGHLIGHT_DURATION_MS = 3000L;
    private static final float INITIAL_SPEED = 975f; // px/s at t=0
    private static final float MIN_SPEED = 120f; // px/s near end
    private static final float MAX_DT = 0.05f;  // clamp large frame gaps

    // Layout
    private static final int ICON_COUNT = 5;
    private static final int DRAW_COUNT = ICON_COUNT + 1;
    private static final int ICON_W = 32;
    private static final int ICON_H = 32;
    private static final int SPACING = 5;
    private static final float STEP = ICON_W + SPACING;

    // Box / visual tweaks
    private static final int EXTRA_WIDTH_BUFFER = 16;
    private static final int OUTER_PAD = 5;
    private static final int OFFSET_TOP = 20;
    private static final int BOX_SHIFT_X = -26;
    private static final int CENTER_NUDGE_PX = 17;
    private static final int FRAME_CONTENT_INSET = 4;
    private static final Color SHADE_BOTTOM = new Color(0, 0, 0, 60);
    private static final Color SHADE_TOP = new Color(255, 255, 255, 25);

    private final Client client;
    private final ItemManager itemManager;

    private final List<Integer> rollingItems = Collections.synchronizedList(new ArrayList<>());

    private static float toDb(int percent) {
        int p = Math.max(0, Math.min(100, percent));
        if (p == 0) {
            return -80.0f; // effectively mute
        }
        double lin = p / 100.0;
        return (float) (20.0 * Math.log10(lin));
    }

    // Textures
    private final BufferedImage rollBoxImage =
            ImageUtil.loadImageResource(getClass(), "/com/chanceman/roll_box.png");
    private final BufferedImage iconFrameImage =
            ImageUtil.loadImageResource(getClass(), "/com/chanceman/icon_slot.png");

    @Inject
    private AudioPlayer audioPlayer;
    @Inject
    private ChanceManConfig config;

    // Animation state
    private volatile boolean isAnimating = false;
    private volatile long rollDurationMs;
    private volatile long rollStartNs = 0L;

    // Motion state
    private float rollOffset = 0f; // cumulative horizontal scroll in px
    private float currentSpeed = INITIAL_SPEED; // px/s
    private Supplier<Integer> randomLockedItemSupplier;
    private volatile long lastUpdateNanos = 0L;

    // Snap state
    private boolean isSnapping = false;
    private long snapStartNs = 0L;
    private float snapBase;
    private float snapResidualStart;
    private float snapTarget;
    private int winnerDelta = 0;

    /**
     * Constructs the overlay (dynamic position, above widgets).
     */
    @Inject
    public ChanceManOverlay(Client client, ItemManager itemManager) {
        this.client = client;
        this.itemManager = itemManager;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    /**
     * Starts the roll animation. Populates the strip and resets timers/state.
     *
     * @param dummy reserved; kept for API compatibility
     * @param rollDurationMs spin duration (ms) before highlight
     * @param randomLockedItemSupplier supplier of random locked item ids
     */
    public void startRollAnimation(int dummy, int rollDurationMs, Supplier<Integer> randomLockedItemSupplier) {
        if (config.enableRollSounds()) {
            Thread t = new Thread(() -> {
                try {
                    float volumeDb = toDb(config.rollSoundVolume());
                    audioPlayer.play(ChanceManOverlay.class, "/com/chanceman/tick.wav", volumeDb);
                }
                catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
                    log.warn("ChanceMan: failed to play tick.wav", ex);
                }
                catch (Throwable t1) {
                    log.warn("ChanceMan: unexpected error while playing tick.wav", t1);
                }
            }, "ChanceMan-Audio");
            t.setDaemon(true);
            t.start();
        }

        this.rollDurationMs = rollDurationMs;
        this.rollStartNs = System.nanoTime();
        this.rollOffset = 0f;
        this.currentSpeed = INITIAL_SPEED;
        this.randomLockedItemSupplier = randomLockedItemSupplier;
        this.isAnimating = true;
        this.lastUpdateNanos = System.nanoTime();

        this.isSnapping = false;
        this.snapStartNs = 0L;
        this.snapBase = 0f;
        this.snapResidualStart = 0f;
        this.snapTarget = 0f;
        this.winnerDelta = 0;

        synchronized (rollingItems) {
            rollingItems.clear();
            for (int i = 0; i < DRAW_COUNT; i++) {
                rollingItems.add(randomLockedItemSupplier.get());
            }
        }
    }

    /**
     * Returns the centered (winning) item after snap/highlight.
     *
     * @return item id, or 0 if unavailable
     */
    public int getFinalItem() {
        synchronized (rollingItems) {
            int centerIndex = ICON_COUNT / 2;
            int idx = Math.min(centerIndex + winnerDelta, rollingItems.size() - 1);
            if (idx >= 0 && idx < rollingItems.size()) {
                return rollingItems.get(idx);
            }
        }
        return 0;
    }

    /**
     * @return highlight duration in ms.
     */
    public int getHighlightDurationMs() {
        return (int) HIGHLIGHT_DURATION_MS;
    }

    /**
     * Advances the animation and draws the strip. Returns null (overlay API).
     */
    @Override
    public Dimension render(Graphics2D g) {
        if (!isAnimating) {
            return null;
        }

        // Rendering hints once per frame
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        final long nowNs = System.nanoTime();
        final long elapsedMs = (nowNs - rollStartNs) / 1_000_000L;
        final boolean inHighlightPhase = (elapsedMs > rollDurationMs);

        // Stop after highlight
        if (elapsedMs > rollDurationMs + HIGHLIGHT_DURATION_MS) {
            isAnimating = false;
            return null;
        }

        // Compute clamped dt and ease the speed (quintic-ish falloff via (1 - t)^3)
        final long nowNanos = System.nanoTime();
        float dt = 0f;
        if (lastUpdateNanos != 0L) {
            dt = (nowNs - lastUpdateNanos) / 1_000_000_000f;
            if (dt > MAX_DT) dt = MAX_DT;
        }
        lastUpdateNanos = nowNs;

        final float t = (rollDurationMs > 0) ? Math.min(1f, elapsedMs / (float) rollDurationMs) : 1f;
        final float eased = (float) Math.pow(1f - t, 3);
        currentSpeed = MIN_SPEED + (INITIAL_SPEED - MIN_SPEED) * eased;

        // Viewport + box geometry
        final int vpX = client.getViewportXOffset();
        final int vpY = client.getViewportYOffset();
        final int vpWidth = client.getViewportWidth();
        final int centerX = vpX + (vpWidth / 2);
        final int boxTopY = vpY + OFFSET_TOP;

        final int totalIconsWidth = ICON_COUNT * ICON_W + (ICON_COUNT - 1) * SPACING;
        final int totalWidthWithBuffer = totalIconsWidth + EXTRA_WIDTH_BUFFER;
        final int boxWidth = totalWidthWithBuffer + OUTER_PAD * 2;
        final int boxHeight = ICON_H + OUTER_PAD * 2;
        final int boxLeftX = centerX - (boxWidth / 2) + BOX_SHIFT_X;

        // Content rect
        final int contentLeftX = boxLeftX + OUTER_PAD;
        final int innerWidth = boxWidth - OUTER_PAD * 2;
        final float contentCenterX = contentLeftX + innerWidth / 2f + CENTER_NUDGE_PX;
        final float middleIndex = ICON_COUNT / 2f;
        final float iconsLeftXF = contentCenterX - middleIndex * STEP - ICON_W / 2f;
        final int iconsY = boxTopY + OUTER_PAD;

        // Background frame + subtle lines
        if (rollBoxImage != null) {
            final Composite prev = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(0.95f));
            g.drawImage(rollBoxImage, boxLeftX, boxTopY, boxWidth, boxHeight, null);
            g.setComposite(prev);

            g.setColor(SHADE_BOTTOM);
            g.fillRect(boxLeftX, boxTopY + boxHeight - 2, boxWidth, 2);
            g.setColor(SHADE_TOP);
            g.drawLine(boxLeftX + 2, boxTopY + 2, boxLeftX + boxWidth - 3, boxTopY + 2);
        }

        // Clip to the strip content
        final Shape oldClip = g.getClip();
        g.setClip(contentLeftX, boxTopY + OUTER_PAD, innerWidth, ICON_H);

        synchronized (rollingItems) {
            // Begin snap near the end of spin (or immediately if we already hit highlight)
            final long remainingMs = rollDurationMs - elapsedMs;
            if (!isSnapping && (remainingMs <= SNAP_DURATION_MS || inHighlightPhase)) {
                startSnap(nowNs);
            }

            // Advance motion
            if (!inHighlightPhase) {
                if (isSnapping) {
                    // Smoothstep to the target slot
                    final long snapElapsedNs = nowNs - snapStartNs;
                    final float u = Math.min(1f, snapElapsedNs / (SNAP_DURATION_MS * 1_000_000f));
                    final float s = u * u * (3f - 2f * u);
                    final float start = rollOffset;
                    final float end = snapTarget;
                    rollOffset = start + (end - start) * s;

                    if (rollOffset >= STEP) {
                        normalizeOnce();
                        winnerDelta = 0;
                        snapBase = 0f;
                        snapTarget = 0f;
                        snapResidualStart = 0f;
                    }
                } else {
                    rollOffset += currentSpeed * dt;
                    while (rollOffset >= STEP) {
                        normalizeOnce();
                    }
                }
            } else {
                // During highlight, ensure exact snap
                if (isSnapping) {
                    rollOffset = snapTarget;
                    if (rollOffset >= STEP) {
                        normalizeOnce();
                        winnerDelta = 0;
                    }
                }
            }

            // Inner content area of the frame
            final int innerBoxXInset = FRAME_CONTENT_INSET;
            final int innerBoxYInset = FRAME_CONTENT_INSET;
            final int innerBoxW = ICON_W - innerBoxXInset * 2;
            final int innerBoxH = ICON_H - innerBoxYInset * 2;

            // Draw items
            final int itemsToDraw = Math.min(rollingItems.size(), DRAW_COUNT);
            for (int i = 0; i < itemsToDraw; i++) {
                final int itemId = rollingItems.get(i);
                final BufferedImage image = itemManager.getImage(itemId, 1, false);
                if (image == null) continue;

                final float drawXF = iconsLeftXF + i * STEP - rollOffset;
                final int drawX = Math.round(drawXF);

                if (iconFrameImage != null) {
                    g.drawImage(iconFrameImage, drawX, iconsY, ICON_W, ICON_H, null);
                }

                final int x = drawX + innerBoxXInset;
                final int y = iconsY + innerBoxYInset;
                g.drawImage(image, x, y, innerBoxW, innerBoxH, null);
            }

            // Highlight winner
            if (inHighlightPhase) {
                final int centerIndex = ICON_COUNT / 2;
                final int winnerIndex = Math.min(centerIndex + winnerDelta, rollingItems.size() - 1);

                final float baseXF = iconsLeftXF + centerIndex * STEP - rollOffset;
                final int baseX = Math.round(baseXF);

                final int glowW = (int) (ICON_W * 2.2);
                final int glowH = (int) (ICON_H * 2.2);
                final float cx = baseX + ICON_W / 2f;
                final float cy = iconsY + ICON_H / 2f;

                // Radial glow
                final RadialGradientPaint glow = new RadialGradientPaint(
                        new Point2D.Float(cx, cy),
                        glowW / 2f,
                        new float[]{0f, 1f},
                        new Color[]{
                                new Color(255, 255, 160, 150),
                                new Color(255, 255, 160, 0)
                        }
                );
                final Composite old = g.getComposite();
                g.setComposite(AlphaComposite.SrcOver.derive(0.85f));
                g.setPaint(glow);
                g.fill(new Ellipse2D.Float(cx - glowW / 2f, cy - glowH / 2f, glowW, glowH));
                g.setComposite(old);

                // Slightly larger winner icon, centered inside the frame
                final float centerScale = 1.12f;
                final int innerBoxX = baseX + innerBoxXInset;
                final int innerBoxY = iconsY + innerBoxYInset;

                final int scaledW = (int) (innerBoxW * centerScale);
                final int scaledH = (int) (innerBoxH * centerScale);
                final int scaledX = innerBoxX + (innerBoxW - scaledW) / 2;
                final int scaledY = innerBoxY + (innerBoxH - scaledH) / 2;

                final int centerItemId = rollingItems.get(winnerIndex);
                final BufferedImage centerImg = itemManager.getImage(centerItemId, 1, false);
                if (centerImg != null) {
                    g.drawImage(centerImg, scaledX, scaledY, scaledW, scaledH, null);
                }
            }
        }

        g.setClip(oldClip);
        return null;
    }

    /**
     * Advances the strip by one slot when a full step is crossed.
     * Removes the left-most item and appends one from the supplier.
     */
    private void normalizeOnce() {
        if (rollOffset >= STEP) {
            rollOffset -= STEP;
            if (!rollingItems.isEmpty()) {
                rollingItems.remove(0);
            }
            if (randomLockedItemSupplier != null) {
                rollingItems.add(randomLockedItemSupplier.get());
            }
        }
    }
    /**
     * Initializes snap state targeting the nearest slot boundary.
     * Ensures roll always aligns to a slot even if the highlight phase began before the snap window ticked.
     */
    private void startSnap(long nowNs) {
        isSnapping = true;
        snapStartNs = nowNs;

        final float k = (float) Math.floor(rollOffset / STEP);
        snapBase = k * STEP;
        snapResidualStart = rollOffset - snapBase; // [0, STEP)
        final boolean goNext = (snapResidualStart / STEP) >= SNAP_NEXT_THRESHOLD;
        winnerDelta = goNext ? 1 : 0;
        snapTarget = goNext ? (snapBase + STEP) : snapBase;
    }
}
