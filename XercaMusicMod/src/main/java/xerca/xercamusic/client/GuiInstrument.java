package xerca.xercamusic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import xerca.xercamusic.common.XercaMusic;
import xerca.xercamusic.common.block.BlockInstrument;
import xerca.xercamusic.common.item.IItemInstrument;
import xerca.xercamusic.common.packets.serverbound.SingleNotePacket;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static xerca.xercamusic.client.ClientStuff.sendToServer;

public class GuiInstrument extends Screen {
    private static final ResourceLocation INS_GUI_TEXTURES = XercaMusic.id("textures/gui/instrument_gui.png");
    private static final int GUI_HEIGHT = 201;
    private static final int GUI_WIDTH = 401;
    private static final int GUI_MARGIN_WIDTH = 7;
    private static final int GUI_NOTE_WIDTH = 8;
    private static final int GUI_OCTAVE_WIDTH = GUI_NOTE_WIDTH * 12 + 1;
    private static final int GUI_OCTAVE_HIGHLIGHT_Y = 212;
    private static final int GUI_OCTAVE_HIGHLIGHT_WIDTH = 98;
    private static final int GUI_OCTAVE_HIGHLIGHT_HEIGHT = 92;
    private static final int GUI_TOP_KEYBOARD_BOTTOM = 94;
    private static final int GUI_BOTTOM_KEYBOARD_TOP = 105;
    private static final int GUI_OCTAVE_BLOCK_X = 99;
    private static final int GUI_OCTAVE_BLOCK_Y = 212;
    private static final int GUI_OCTAVE_BLOCK_WIDTH = 95;
    private static final int GUI_OCTAVE_BLOCK_HEIGHT = 82;
    private static final int OCTAVE_BUTTON_Y = 30;
    private static int currentKeyboardOctave;

    // ---- Sky mode ----
    private static final String SKY_BUTTON_PATH = "textures/gui/sky/button/";
    private static final String SKY_BACKGROUND_PATH = "textures/gui/sky/background/";
    private static final int SKY_BACKGROUND_FRAME_COUNT = 18;
    private static final int SKY_BACKGROUND_FRAME_TIME = 100;
    private static final int SKY_BACKGROUND_WIDTH = 520;
    private static final int SKY_BACKGROUND_HEIGHT = 260;
    private static final ResourceLocation[] NOTE_BUTTON_NORMAL = new ResourceLocation[7];
    private static final ResourceLocation[] NOTE_BUTTON_PRESSED = new ResourceLocation[7];
    private static final ResourceLocation CHORD_BUTTON_NORMAL = XercaMusic.id(SKY_BUTTON_PATH + "gray_normal.png");
    private static final ResourceLocation CHORD_BUTTON_PRESSED = XercaMusic.id(SKY_BUTTON_PATH + "gray_pressed.png");
    private static final int SKY_BUTTON_SIZE = 40;
    private static final int SKY_BUTTON_SPACING = 10;
    private static final int SKY_BUTTON_MARGIN = 3;
    private static final float SKY_BUTTON_ALPHA = 0.8f;
    private static final int SKY_GRID_ROWS = 3;
    private static final int SKY_GRID_COLS = 5;
    private static final int SKY_NOTE_COUNT = 15;
    private static final int CHORD_GRID_ROWS = 3;
    private static final int CHORD_GRID_COLS = 2;
    private static final int CHORD_COUNT = 6;
    private static final int SEPARATOR_GAP = 12;
    /** Particle ids for chord buttons are offset so they never clash with note button ids. */
    private static final int CHORD_PARTICLE_ID_OFFSET = 100;
    /** Highest offset used by {@link #SKY_NOTE_OFFSETS}, in octaves. */
    private static final int SKY_NOTE_SPAN_OCTAVES = 2;
    /** Chords are played this many octaves below the note area. */
    private static final int CHORD_OCTAVE_SHIFT = 2;

    /** Button id (0-14) to semitone offset from the current sky octave: 1-7, 1-7 an octave up, then the top 1. */
    private static final int[] SKY_NOTE_OFFSETS = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24};
    private static final int[] SKY_KEYS = {
            GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P,
            GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_SEMICOLON,
            GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH
    };
    private static final String[] SKY_KEY_NAMES = {"Y", "U", "I", "O", "P", "H", "J", "K", "L", ";", "N", "M", ",", ".", "/"};
    private static final String[] NOTE_NAMES = {"1", "2", "3", "4", "5", "6", "7"};

    /** Triads I ii iii IV V vi (135 246 357 461 572 613) as semitone offsets. */
    private static final int[][] CHORD_NOTE_OFFSETS = {
            {0, 4, 7}, {2, 5, 9}, {4, 7, 11}, {5, 9, 12}, {7, 11, 14}, {9, 12, 16}
    };
    private static final int[] CHORD_KEYS = {
            GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T
    };
    private static final String[] CHORD_KEY_NAMES = {"V", "B", "F", "G", "R", "T"};
    private static final String[] CHORD_DEGREE_NAMES = {"135", "246", "357", "461", "572", "613"};
    /** Chord button ids laid out by row/column: R T on top, F G in the middle, V B at the bottom. */
    private static final int[][] CHORD_LAYOUT = {{4, 5}, {2, 3}, {0, 1}};

    private static final Map<Integer, Integer> KEY_TO_SKY_NOTE = new HashMap<>();
    private static final Map<Integer, Integer> KEY_TO_CHORD = new HashMap<>();

    /** Remembered for the rest of the game session, like the keyboard octave. */
    private static boolean skyModeActive;
    private static int skyCurrentOctave = 3;

    static {
        String[] colors = {"red", "orange", "yellow", "green", "cyan", "blue", "purple"};
        for (int i = 0; i < colors.length; i++) {
            NOTE_BUTTON_NORMAL[i] = XercaMusic.id(SKY_BUTTON_PATH + colors[i] + "_normal.png");
            NOTE_BUTTON_PRESSED[i] = XercaMusic.id(SKY_BUTTON_PATH + colors[i] + "_pressed.png");
        }
        for (int i = 0; i < SKY_KEYS.length; i++) {
            KEY_TO_SKY_NOTE.put(SKY_KEYS[i], i);
        }
        for (int i = 0; i < CHORD_KEYS.length; i++) {
            KEY_TO_CHORD.put(CHORD_KEYS[i], i);
        }
    }

    private final boolean[] buttonPushStates;
    private final NoteSound[] noteSounds;
    /** How many sky buttons currently hold each note, so chords and single notes sharing a pitch don't cut each other off. */
    private final int[] skyNoteHolds;
    private final boolean[] skyButtonStates = new boolean[SKY_NOTE_COUNT];
    private final boolean[] chordButtonStates = new boolean[CHORD_COUNT];
    /** Notes actually started by each pressed sky button / chord, so a release stops exactly what the press started. */
    private final int[] skyButtonNote = new int[SKY_NOTE_COUNT];
    private final int[][] chordButtonNotes = new int[CHORD_COUNT][];
    private final Player player;
    private final IItemInstrument instrument;
    @Nullable
    private final BlockPos blockInsPos;
    private final MidiHandler midiHandler;
    private final AnimatedBackground animatedBackground;
    private final NoteParticleManager particleManager = new NoteParticleManager();
    private int guiBaseX = 45;
    private int guiBaseY = 80;
    private int octaveButtonX;
    private Button toggleInterfaceButton;
    private long lastRenderTime;
    /** Sky button held by the mouse: 0-14 note, 100+ chord, -1 none. */
    private int mouseHeldSkyButton = -1;
    // Sky layout, computed in init() so it follows window resizes
    private int skyChordStartX;
    private int skyNoteStartX;
    private int skyStartY;
    private int skySeparatorX;
    private int skyAreaHeight;

    GuiInstrument(Player player, IItemInstrument instrument, Component title, @Nullable BlockPos blockInsPos) {
        super(title);
        this.player = player;
        this.instrument = instrument;
        this.buttonPushStates = new boolean[IItemInstrument.TOTAL_NOTES];
        this.noteSounds = new NoteSound[IItemInstrument.TOTAL_NOTES];
        this.skyNoteHolds = new int[IItemInstrument.TOTAL_NOTES];
        this.midiHandler = new MidiHandler(this::playSound, this::stopSound);
        this.blockInsPos = blockInsPos;
        this.animatedBackground = new AnimatedBackground(SKY_BACKGROUND_PATH, ".png", SKY_BACKGROUND_FRAME_COUNT, SKY_BACKGROUND_FRAME_TIME);
        Arrays.fill(skyButtonNote, -1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init() {
        guiBaseX = (this.width - GUI_WIDTH) / 2;
        guiBaseY = (this.height - GUI_HEIGHT) / 2;
        octaveButtonX = guiBaseX - 10;

        if (currentKeyboardOctave < instrument.getMinOctave()) {
            currentKeyboardOctave = instrument.getMinOctave();
        } else if (currentKeyboardOctave > instrument.getMaxOctave()) {
            currentKeyboardOctave = instrument.getMaxOctave();
        }
        midiHandler.setCurrentOctave(currentKeyboardOctave);
        skyCurrentOctave = clampSkyOctave(skyCurrentOctave);
        computeSkyLayout();
        lastRenderTime = System.currentTimeMillis();

        this.addRenderableWidget(Button.builder(Component.translatable("note.upButton"), button -> increaseOctave()).
                bounds(octaveButtonX, OCTAVE_BUTTON_Y, 10, 10).
                tooltip(Tooltip.create(Component.translatable("ins.octaveTooltip"))).build());

        this.addRenderableWidget(Button.builder(Component.translatable("note.downButton"), button -> decreaseOctave()).
                bounds(octaveButtonX, OCTAVE_BUTTON_Y + 25, 10, 10).
                tooltip(Tooltip.create(Component.translatable("ins.octaveTooltip"))).build());

        toggleInterfaceButton = this.addRenderableWidget(Button.builder(toggleButtonText(), button -> toggleInterface())
                .bounds(this.width - 100, 10, 80, 20)
                .tooltip(toggleButtonTooltip())
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft client = minecraft;
        if (blockInsPos != null && client != null) {
            if (player.level().getBlockState(blockInsPos).getBlock() instanceof BlockInstrument blockIns) {
                if (!Objects.equals(blockIns.getItemInstrument(), instrument)) {
                    client.setScreen(null);
                }
            } else {
                client.setScreen(null);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // All rendering is handled in render()
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        particleManager.update((now - lastRenderTime) / 1000.0f);
        lastRenderTime = now;

        if (skyModeActive) {
            renderSkyInterface(guiGraphics);
        } else {
            renderOriginalInterface(guiGraphics);
        }
        particleManager.render(guiGraphics);
        RenderSystem.disableBlend();

        int shownOctave = skyModeActive ? skyCurrentOctave : currentKeyboardOctave;
        guiGraphics.drawCenteredString(this.font, Integer.toString(shownOctave), octaveButtonX + 4, OCTAVE_BUTTON_Y + 14, 0xFFFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderOriginalInterface(GuiGraphics guiGraphics) {
        RenderSystem.setShaderTexture(0, INS_GUI_TEXTURES);

        guiGraphics.blit(INS_GUI_TEXTURES, guiBaseX, guiBaseY, 0, 0, 0, GUI_WIDTH, GUI_HEIGHT, 512, 512);

        for (int i = 0; i < buttonPushStates.length; i++) {
            if (buttonPushStates[i]) {
                int pushedOctave = i / 12;
                int x = guiBaseX + GUI_MARGIN_WIDTH + i * GUI_NOTE_WIDTH + pushedOctave;
                int y = guiBaseY + 11;
                if (pushedOctave > 3) {
                    x -= 4 + 48 * GUI_NOTE_WIDTH;
                    y = guiBaseY + GUI_BOTTOM_KEYBOARD_TOP + 2;
                }
                guiGraphics.blit(INS_GUI_TEXTURES, x, y, 0, 402, 11, 7, 82, 512, 512);
            }
        }

        int currentKeyboardOctaveDraw = Math.max(0, currentKeyboardOctave);
        int octaveHighlightX = guiBaseX + GUI_MARGIN_WIDTH + currentKeyboardOctaveDraw * GUI_OCTAVE_WIDTH - 1;
        int octaveHighlightY = guiBaseY + 3;
        if (currentKeyboardOctave > 3) {
            octaveHighlightX -= 4 * GUI_OCTAVE_WIDTH;
            octaveHighlightY = guiBaseY + GUI_BOTTOM_KEYBOARD_TOP - 6;
        }
        guiGraphics.blit(INS_GUI_TEXTURES, octaveHighlightX, octaveHighlightY, 0, 0, 0, GUI_OCTAVE_HIGHLIGHT_Y, GUI_OCTAVE_HIGHLIGHT_WIDTH, GUI_OCTAVE_HIGHLIGHT_HEIGHT, 512, 512);

        for (int i = 0; i < 8; i++) {
            if (i < instrument.getMinOctave() || i > instrument.getMaxOctave()) {
                int x = guiBaseX + GUI_MARGIN_WIDTH + i * GUI_OCTAVE_WIDTH;
                int y = guiBaseY + 11;
                if (i > 3) {
                    x -= 4 * GUI_OCTAVE_WIDTH;
                    y = guiBaseY + GUI_BOTTOM_KEYBOARD_TOP + 2;
                }
                guiGraphics.blit(INS_GUI_TEXTURES, x, y, 0, 0, GUI_OCTAVE_BLOCK_X, GUI_OCTAVE_BLOCK_Y, GUI_OCTAVE_BLOCK_WIDTH, GUI_OCTAVE_BLOCK_HEIGHT, 512, 512);
            }
        }
    }

    // ---------------------------------------------------------------- Sky mode: layout & rendering

    private void computeSkyLayout() {
        int chordWidth = CHORD_GRID_COLS * SKY_BUTTON_SIZE + (CHORD_GRID_COLS - 1) * SKY_BUTTON_SPACING;
        int noteWidth = SKY_GRID_COLS * SKY_BUTTON_SIZE + (SKY_GRID_COLS - 1) * SKY_BUTTON_SPACING;
        int totalWidth = chordWidth + SEPARATOR_GAP * 2 + noteWidth;
        skyAreaHeight = SKY_GRID_ROWS * SKY_BUTTON_SIZE + (SKY_GRID_ROWS - 1) * SKY_BUTTON_SPACING;
        skyChordStartX = (this.width - totalWidth) / 2;
        skyStartY = (this.height - skyAreaHeight) / 2;
        skySeparatorX = skyChordStartX + chordWidth + SEPARATOR_GAP - 1;
        skyNoteStartX = skyChordStartX + chordWidth + SEPARATOR_GAP * 2;
    }

    private int skyNoteX(int buttonId) {
        return skyNoteStartX + (buttonId % SKY_GRID_COLS) * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
    }

    private int skyNoteY(int buttonId) {
        return skyStartY + (buttonId / SKY_GRID_COLS) * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
    }

    private int chordX(int chordId) {
        for (int[] row : CHORD_LAYOUT) {
            for (int col = 0; col < row.length; col++) {
                if (row[col] == chordId) {
                    return skyChordStartX + col * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
                }
            }
        }
        return skyChordStartX;
    }

    private int chordY(int chordId) {
        for (int row = 0; row < CHORD_LAYOUT.length; row++) {
            for (int id : CHORD_LAYOUT[row]) {
                if (id == chordId) {
                    return skyStartY + row * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
                }
            }
        }
        return skyStartY;
    }

    private void renderSkyInterface(GuiGraphics guiGraphics) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // Shrink the background on small screens instead of letting it overflow
        float scale = Math.min(1f, Math.min((float) this.width / SKY_BACKGROUND_WIDTH, (float) this.height / SKY_BACKGROUND_HEIGHT));
        int bgWidth = Math.round(SKY_BACKGROUND_WIDTH * scale);
        int bgHeight = Math.round(SKY_BACKGROUND_HEIGHT * scale);
        guiGraphics.blit(animatedBackground.getCurrentFrame(), (this.width - bgWidth) / 2, (this.height - bgHeight) / 2,
                bgWidth, bgHeight, 0, 0, SKY_BACKGROUND_WIDTH, SKY_BACKGROUND_HEIGHT, SKY_BACKGROUND_WIDTH, SKY_BACKGROUND_HEIGHT);

        for (int chordId = 0; chordId < CHORD_COUNT; chordId++) {
            ResourceLocation texture = chordButtonStates[chordId] ? CHORD_BUTTON_PRESSED : CHORD_BUTTON_NORMAL;
            renderSkyButton(guiGraphics, texture, chordX(chordId), chordY(chordId), CHORD_DEGREE_NAMES[chordId], 0xFF77CCFF, CHORD_KEY_NAMES[chordId]);
        }

        guiGraphics.fill(skySeparatorX, skyStartY, skySeparatorX + 2, skyStartY + skyAreaHeight, 0xFFAAAAAA);

        for (int buttonId = 0; buttonId < SKY_NOTE_COUNT; buttonId++) {
            int noteIndex = buttonId % 7;
            ResourceLocation texture = skyButtonStates[buttonId] ? NOTE_BUTTON_PRESSED[noteIndex] : NOTE_BUTTON_NORMAL[noteIndex];
            renderSkyButton(guiGraphics, texture, skyNoteX(buttonId), skyNoteY(buttonId), NOTE_NAMES[noteIndex], 0xFFFFFFFF, SKY_KEY_NAMES[buttonId]);
        }

        guiGraphics.drawCenteredString(font, Component.translatable("gui.xercamusic.sky.title"), this.width / 2, 20, 0xFFFFFFFF);
        guiGraphics.drawString(font, Component.translatable("gui.xercamusic.sky.hint_octave"), 10, this.height - 30, 0xFFFFFF00);
        guiGraphics.drawString(font, Component.translatable("gui.xercamusic.sky.hint_chords"), 10, this.height - 15, 0xFFFFFF00);
    }

    private void renderSkyButton(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, String label, int labelColor, String keyName) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, SKY_BUTTON_ALPHA);
        int size = SKY_BUTTON_SIZE - SKY_BUTTON_MARGIN * 2;
        guiGraphics.blit(texture, x + SKY_BUTTON_MARGIN, y + SKY_BUTTON_MARGIN, size, size, 0, 0, SKY_BUTTON_SIZE, SKY_BUTTON_SIZE, SKY_BUTTON_SIZE, SKY_BUTTON_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        String keyText = " (" + keyName + ")";
        int textX = x + (SKY_BUTTON_SIZE - font.width(label) - font.width(keyText)) / 2;
        int textY = y + SKY_BUTTON_SIZE + 2;
        guiGraphics.drawString(font, label, textX, textY, labelColor);
        guiGraphics.drawString(font, keyText, textX + font.width(label), textY, 0xFFFFFF00);
    }

    /** Returns 0-14 for a note button, CHORD_PARTICLE_ID_OFFSET + id for a chord button, -1 otherwise. */
    private int skyButtonAt(double mouseX, double mouseY) {
        for (int chordId = 0; chordId < CHORD_COUNT; chordId++) {
            int x = chordX(chordId);
            int y = chordY(chordId);
            if (mouseX >= x && mouseX < x + SKY_BUTTON_SIZE && mouseY >= y && mouseY < y + SKY_BUTTON_SIZE) {
                return CHORD_PARTICLE_ID_OFFSET + chordId;
            }
        }
        for (int buttonId = 0; buttonId < SKY_NOTE_COUNT; buttonId++) {
            int x = skyNoteX(buttonId);
            int y = skyNoteY(buttonId);
            if (mouseX >= x && mouseX < x + SKY_BUTTON_SIZE && mouseY >= y && mouseY < y + SKY_BUTTON_SIZE) {
                return buttonId;
            }
        }
        return -1;
    }

    private void pressSkyButton(int id) {
        if (id >= CHORD_PARTICLE_ID_OFFSET) {
            playChord(id - CHORD_PARTICLE_ID_OFFSET);
        } else if (id >= 0) {
            playSkyNote(id);
        }
    }

    private void releaseSkyButton(int id) {
        if (id >= CHORD_PARTICLE_ID_OFFSET) {
            stopChord(id - CHORD_PARTICLE_ID_OFFSET);
        } else if (id >= 0) {
            stopSkyNote(id);
        }
    }

    // ---------------------------------------------------------------- Sky mode: playing

    private int clampSkyOctave(int octave) {
        int max = Math.max(instrument.getMinOctave(), instrument.getMaxOctave() - SKY_NOTE_SPAN_OCTAVES);
        return Math.max(instrument.getMinOctave(), Math.min(max, octave));
    }

    private void holdNote(int noteId) {
        if (noteId < 0 || noteId >= skyNoteHolds.length) {
            return;
        }
        if (skyNoteHolds[noteId]++ == 0) {
            playSound(noteId);
        }
    }

    private void releaseNote(int noteId) {
        if (noteId < 0 || noteId >= skyNoteHolds.length || skyNoteHolds[noteId] == 0) {
            return;
        }
        if (--skyNoteHolds[noteId] == 0) {
            stopSound(noteId);
        }
    }

    private void playSkyNote(int buttonId) {
        if (buttonId < 0 || buttonId >= SKY_NOTE_COUNT || skyButtonStates[buttonId]) {
            return; // ignore key repeat
        }
        skyButtonStates[buttonId] = true;
        int noteId = SKY_NOTE_OFFSETS[buttonId] + 12 * skyCurrentOctave;
        skyButtonNote[buttonId] = noteId;
        holdNote(noteId);
        particleManager.createParticlesForButtonPress(buttonId, skyNoteX(buttonId) + SKY_BUTTON_SIZE / 2f, skyNoteY(buttonId) + SKY_BUTTON_SIZE / 2f);
    }

    private void stopSkyNote(int buttonId) {
        if (buttonId < 0 || buttonId >= SKY_NOTE_COUNT || !skyButtonStates[buttonId]) {
            return;
        }
        skyButtonStates[buttonId] = false;
        releaseNote(skyButtonNote[buttonId]);
        skyButtonNote[buttonId] = -1;
        particleManager.handleButtonRelease(buttonId);
    }

    private void playChord(int chordId) {
        if (chordId < 0 || chordId >= CHORD_COUNT || chordButtonStates[chordId]) {
            return;
        }
        chordButtonStates[chordId] = true;
        int chordOctave = Math.max(instrument.getMinOctave(), skyCurrentOctave - CHORD_OCTAVE_SHIFT);
        int[] offsets = CHORD_NOTE_OFFSETS[chordId];
        int[] notes = new int[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            notes[i] = offsets[i] + 12 * chordOctave;
            holdNote(notes[i]);
        }
        chordButtonNotes[chordId] = notes;
        particleManager.createParticlesForButtonPress(CHORD_PARTICLE_ID_OFFSET + chordId, chordX(chordId) + SKY_BUTTON_SIZE / 2f, chordY(chordId) + SKY_BUTTON_SIZE / 2f);
    }

    private void stopChord(int chordId) {
        if (chordId < 0 || chordId >= CHORD_COUNT || !chordButtonStates[chordId]) {
            return;
        }
        chordButtonStates[chordId] = false;
        if (chordButtonNotes[chordId] != null) {
            for (int noteId : chordButtonNotes[chordId]) {
                releaseNote(noteId);
            }
            chordButtonNotes[chordId] = null;
        }
        particleManager.handleButtonRelease(CHORD_PARTICLE_ID_OFFSET + chordId);
    }

    private void releaseAllSkyButtons() {
        for (int i = 0; i < SKY_NOTE_COUNT; i++) {
            stopSkyNote(i);
        }
        for (int i = 0; i < CHORD_COUNT; i++) {
            stopChord(i);
        }
        mouseHeldSkyButton = -1;
        Arrays.fill(skyNoteHolds, 0);
    }

    private void toggleInterface() {
        releaseAllSkyButtons();
        stopAllSounds();
        particleManager.clearAll();
        skyModeActive = !skyModeActive;
        toggleInterfaceButton.setMessage(toggleButtonText());
        toggleInterfaceButton.setTooltip(toggleButtonTooltip());
    }

    private static Component toggleButtonText() {
        return Component.translatable(skyModeActive ? "gui.xercamusic.toggle_original_interface" : "gui.xercamusic.toggle_sky_interface");
    }

    private static Tooltip toggleButtonTooltip() {
        return Tooltip.create(Component.translatable(skyModeActive ? "gui.xercamusic.toggle_original_interface.tooltip" : "gui.xercamusic.toggle_sky_interface.tooltip"));
    }

    // ---------------------------------------------------------------- Original mode & shared sound handling

    private int noteIdFromPos(int mouseX, int mouseY) {
        int buttonBaseX = guiBaseX + GUI_MARGIN_WIDTH;
        if (mouseX >= buttonBaseX && mouseX <= buttonBaseX + GUI_WIDTH - 14
                && mouseY >= guiBaseY + 9 && mouseY <= guiBaseY + GUI_HEIGHT - 10
                && (mouseY < guiBaseY + GUI_TOP_KEYBOARD_BOTTOM || mouseY > guiBaseY + GUI_BOTTOM_KEYBOARD_TOP)) {
            int octavePlus = (mouseY < guiBaseY + GUI_TOP_KEYBOARD_BOTTOM) ? 0 : 4;
            int octave = octavePlus + (mouseX - buttonBaseX) / GUI_OCTAVE_WIDTH;
            int note = ((mouseX - buttonBaseX) % GUI_OCTAVE_WIDTH) / GUI_NOTE_WIDTH;
            if (note < 12) {
                return octave * 12 + note;
            }
        }
        return -1;
    }

    private void playSound(int noteId) {
        playSound(new MidiHandler.MidiData(noteId, 0.8f));
    }

    private void playSound(MidiHandler.MidiData data) {
        int noteId = data.noteId();

        if (noteId >= 0 && noteId < buttonPushStates.length && !buttonPushStates[noteId]) {
            int note = IItemInstrument.idToNote(noteId);

            IItemInstrument.InsSound noteSound = instrument.getSound(note);
            if (noteSound == null) {
                return;
            }
            noteSounds[noteId] = ClientStuff.playNote(noteSound.sound(), player.getX(), player.getY(), player.getZ(), data.volume(), noteSound.pitch());
            player.level().addParticle(ParticleTypes.NOTE, player.getX(), player.getY() + 2.2D, player.getZ(), note / 24.0D, 0.0D, 0.0D);
            buttonPushStates[noteId] = true;

            SingleNotePacket pack = new SingleNotePacket(note, instrument, false, data.volume());
            sendToServer(pack);
        }
    }

    private void stopSound(int noteId) {
        if (noteId >= 0 && noteId < buttonPushStates.length && buttonPushStates[noteId] && noteSounds[noteId] != null) {
            noteSounds[noteId].stopSound();
            noteSounds[noteId] = null;
            buttonPushStates[noteId] = false;

            int note = IItemInstrument.idToNote(noteId);
            SingleNotePacket pack = new SingleNotePacket(note, instrument, true, 1f);
            sendToServer(pack);
        }
    }

    private void stopAllSounds() {
        for (int noteId = 0; noteId < buttonPushStates.length; noteId++) {
            stopSound(noteId);
        }
    }

    @Override
    public boolean mouseClicked(double dmouseX, double dmouseY, int mouseButton) {
        if (super.mouseClicked(dmouseX, dmouseY, mouseButton)) {
            return true;
        }
        if (skyModeActive) {
            int id = skyButtonAt(dmouseX, dmouseY);
            if (id >= 0) {
                releaseSkyButton(mouseHeldSkyButton);
                mouseHeldSkyButton = id;
                pressSkyButton(id);
            }
            return true;
        }
        int mouseX = (int) Math.round(dmouseX);
        int mouseY = (int) Math.round(dmouseY);

        int noteId = noteIdFromPos(mouseX, mouseY);
        playSound(noteId);
        return true;
    }

    @Override
    public boolean mouseReleased(double dmouseX, double dmouseY, int mouseButton) {
        super.mouseReleased(dmouseX, dmouseY, mouseButton);
        if (skyModeActive) {
            releaseSkyButton(mouseHeldSkyButton);
            mouseHeldSkyButton = -1;
            return true;
        }
        int mouseX = (int) Math.round(dmouseX);
        int mouseY = (int) Math.round(dmouseY);

        int noteId = noteIdFromPos(mouseX, mouseY);
        stopSound(noteId);
        return true;
    }

    @Override
    public boolean mouseDragged(double posX, double posY, int mouseButton, double deltaX, double deltaY) {
        if (skyModeActive) {
            // Slide across buttons like on a touch screen, but only while a sky button is held
            if (mouseHeldSkyButton >= 0) {
                int id = skyButtonAt(posX, posY);
                if (id >= 0 && id != mouseHeldSkyButton) {
                    releaseSkyButton(mouseHeldSkyButton);
                    mouseHeldSkyButton = id;
                    pressSkyButton(id);
                }
            }
            return super.mouseDragged(posX, posY, mouseButton, deltaX, deltaY);
        }
        int mouseX = (int) Math.round(posX);
        int mouseY = (int) Math.round(posY);
        int prevMouseX = (int) Math.round(posX - deltaX);
        int prevMouseY = (int) Math.round(posY - deltaY);

        int prevNoteId = noteIdFromPos(prevMouseX, prevMouseY);
        int currentNoteId = noteIdFromPos(mouseX, mouseY);
        if (prevNoteId != currentNoteId) {
            stopSound(prevNoteId);
            playSound(currentNoteId);
        }

        return super.mouseDragged(posX, posY, mouseButton, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        setFocused(null);
        super.keyPressed(keyCode, scanCode, modifiers);

        if (skyModeActive) {
            Integer chordId = KEY_TO_CHORD.get(keyCode);
            Integer buttonId = KEY_TO_SKY_NOTE.get(keyCode);
            if (chordId != null) {
                playChord(chordId);
            } else if (buttonId != null) {
                playSkyNote(buttonId);
            } else if (keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_LEFT_BRACKET) {
                decreaseOctave();
            } else if (keyCode == GLFW.GLFW_KEY_S || keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) {
                increaseOctave();
            }
            return true;
        }

        if (scanCode >= 16 && scanCode <= 27) {
            int noteId = scanCode - 16 + 12 * Math.max(0, currentKeyboardOctave);
            playSound(noteId);
        }

        if (keyCode == GLFW.GLFW_KEY_A) {
            decreaseOctave();
        } else if (keyCode == GLFW.GLFW_KEY_S) {
            increaseOctave();
        }
        return true;
    }

    private void decreaseOctave() {
        if (skyModeActive) {
            changeSkyOctave(-1);
        } else if (currentKeyboardOctave > -3) {
            currentKeyboardOctave--;
            midiHandler.setCurrentOctave(currentKeyboardOctave);
            stopAllSounds();
        }
    }

    private void increaseOctave() {
        if (skyModeActive) {
            changeSkyOctave(1);
        } else if (currentKeyboardOctave < instrument.getMaxOctave()) {
            currentKeyboardOctave++;
            midiHandler.setCurrentOctave(currentKeyboardOctave);
            stopAllSounds();
        }
    }

    private void changeSkyOctave(int delta) {
        int newOctave = clampSkyOctave(skyCurrentOctave + delta);
        if (newOctave != skyCurrentOctave) {
            releaseAllSkyButtons();
            stopAllSounds();
            skyCurrentOctave = newOctave;
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (skyModeActive) {
            Integer chordId = KEY_TO_CHORD.get(keyCode);
            if (chordId != null) {
                stopChord(chordId);
            }
            Integer buttonId = KEY_TO_SKY_NOTE.get(keyCode);
            if (buttonId != null) {
                stopSkyNote(buttonId);
            }
            return true;
        }
        if (scanCode >= 16 && scanCode <= 27) {
            int noteId = scanCode - 16 + 12 * Math.max(0, currentKeyboardOctave);
            stopSound(noteId);
        }
        return true;
    }

    @Override
    public void removed() {
        releaseAllSkyButtons();
        stopAllSounds();
        particleManager.clearAll();
        midiHandler.closeDevices();
    }
}
