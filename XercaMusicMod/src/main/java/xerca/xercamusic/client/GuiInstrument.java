package xerca.xercamusic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import xerca.xercamusic.common.XercaMusic;
import xerca.xercamusic.common.block.BlockInstrument;
import xerca.xercamusic.common.item.IItemInstrument;
import xerca.xercamusic.common.packets.SingleNotePacket;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class GuiInstrument extends Screen {
    private static final ResourceLocation insGuiTextures = new ResourceLocation(XercaMusic.MODID, "textures/gui/instrument_gui.png");
    private static final ResourceLocation skyGuiTextures = new ResourceLocation(XercaMusic.MODID, "textures/gui/sky_instrument_gui.png");
    private static final ResourceLocation greenBlockTexture = new ResourceLocation(XercaMusic.MODID, "textures/gui/tmp/g40.png");
    private static final ResourceLocation redBlockTexture = new ResourceLocation(XercaMusic.MODID, "textures/gui/tmp/r40.png");
    // 添加背景图资源
    private static final ResourceLocation skyBackgroundTexture = new ResourceLocation(XercaMusic.MODID, "textures/gui/sky/background/background.gif");
    
    // 定义按钮纹理资源路径 - 修改为全小写
    private static final String BUTTON_PATH = "textures/gui/sky/button/";
    private static final ResourceLocation[] NOTE_BUTTON_NORMAL = new ResourceLocation[7];
    private static final ResourceLocation[] NOTE_BUTTON_PRESSED = new ResourceLocation[7];
    private static final ResourceLocation CHORD_BUTTON_NORMAL;
    private static final ResourceLocation CHORD_BUTTON_PRESSED;
    
    // 按钮相关常量
    private static final int BUTTON_MARGIN = 3; // 按钮边缘的margin
    private static final float BUTTON_ALPHA = 0.8f; // 略微调低透明度

    private int guiBaseX = 45;
    private int guiBaseY = 80;
    private final boolean[] buttonPushStates;
    private final NoteSound[] noteSounds;
    private static int currentKeyboardOctave = 0;
    
    // 添加标志来跟踪当前界面模式 - 默认为原始界面
    private boolean isSkyInterfaceActive = false;
    // 添加界面切换按钮
    private Button toggleInterfaceButton;
    
    // Sky模式相关
    private static final int SKY_BUTTON_SIZE = 40;
    private static final int SKY_BUTTON_SPACING = 10;
    private static final int SKY_GRID_ROWS = 3;
    private static final int SKY_GRID_COLS = 5;
    
    // 存储Sky界面按钮状态，键为按钮ID (0-14)，值为是否被按下
    private final boolean[] skyButtonStates = new boolean[15];
    
    // 存储键盘按键与按钮ID的映射关系
    private final Map<Integer, Integer> keyToButtonIdMap = new HashMap<>();
    
    // Sky模式下音符映射，按钮ID (0-14) 到音符ID的映射
    private static final int[] skyNoteMapping = {
        0, 2, 4, 5, 7, 9, 11,  // 1-2-3-4-5-6-7 (低八度)
        12, 14, 16, 17, 19, 21, 23, // 1-2-3-4-5-6-7 (高八度，直接加12表示高一个八度)
        24  // 1 (最高音，比低八度高2个八度)
    };

    private static final int guiHeight = 201;
    private static final int guiWidth = 401;
    private static final int guiMarginWidth = 7;
    private static final int guiNoteWidth = 8;
    private static final int guiOctaveWidth = guiNoteWidth * 12 + 1;
    private static final int guiOctaveHighlightY = 212;
    private static final int guiOctaveHighlightWidth = 98;
    private static final int guiOctaveHighlightHeight = 92;
    private static final int guiTopKeyboardBottom = 94;
    private static final int guiBottomKeyboardTop = 105;
    private static final int guiOctaveBlockX = 99;
    private static final int guiOctaveBlockY = 212;
    private static final int guiOctaveBlockWidth = 95;
    private static final int guiOctaveBlockHeight = 82;
    private int octaveButtonX;
    private final int octaveButtonY = 30;

    private final Player player;
    private final IItemInstrument instrument;
    private final BlockPos blockInsPos;
    private final MidiHandler midiHandler;
    
    // 增加Sky模式的八度控制
    private static int skyCurrentOctave = 3; // 默认从第三个八度开始

    // 和弦胶囊相关常量
    private static final int CHORD_BUTTON_WIDTH = 40; // 修改为40，与SKY_BUTTON_SIZE一致，解决"胖"问题
    private static final int CHORD_BUTTON_HEIGHT = 40;
    private static final int CHORD_BUTTON_SPACING = 10;
    private static final int CHORD_GRID_ROWS = 3;
    private static final int CHORD_GRID_COLS = 2;
    
    // 存储和弦胶囊按钮状态
    private final boolean[] chordButtonStates = new boolean[6];
    
    // 和弦按键映射
    private final Map<Integer, Integer> keyToChordButtonIdMap = new HashMap<>();
    
    // 修正和弦定义为传统三和弦: 根音、三度音、五度音
    // 对应音乐中的 135、246、357、461、572、613
    private static final int[][] chordNoteOffsets = {
        {0, 4, 7},   // 1和弦（C大三和弦): C-E-G（对应135）
        {2, 5, 9},   // 2和弦（D小三和弦): D-F-A（对应246）
        {4, 7, 11},  // 3和弦（E小三和弦): E-G-B（对应357）
        {5, 9, 12},  // 4和弦（F大三和弦): F-A-C（对应461）
        {7, 11, 14}, // 5和弦（G大三和弦): G-B-D（对应572）
        {9, 12, 16}  // 6和弦（A小三和弦): A-C-E（对应613）
    };

    static {
        // 初始化音符按钮纹理资源 - 修改文件名为全小写
        String[] colors = {"red", "orange", "yellow", "green", "cyan", "blue", "purple"};
        for (int i = 0; i < 7; i++) {
            NOTE_BUTTON_NORMAL[i] = new ResourceLocation(XercaMusic.MODID, BUTTON_PATH + colors[i] + "_normal.png");
            NOTE_BUTTON_PRESSED[i] = new ResourceLocation(XercaMusic.MODID, BUTTON_PATH + colors[i] + "_pressed.png");
        }
        
        // 初始化和弦按钮纹理资源 - 修改文件名为全小写
        CHORD_BUTTON_NORMAL = new ResourceLocation(XercaMusic.MODID, BUTTON_PATH + "gray_normal.png");
        CHORD_BUTTON_PRESSED = new ResourceLocation(XercaMusic.MODID, BUTTON_PATH + "gray_pressed.png");
    }

    // 添加背景图资源 - 改用静态PNG帧
    private static final String BACKGROUND_PATH = "textures/gui/sky/background/";
    private static final int BACKGROUND_FRAME_COUNT = 18;  // 0.png 到 17.png
    private static final int BACKGROUND_FRAME_TIME = 100;  // 每帧切换时间(毫秒)
    private AnimatedBackground animatedBackground;
    
    // 粒子系统
    private NoteParticleManager particleManager;
    
    // 保存上一帧渲染时间，用于计算deltaTime
    private long lastRenderTime;

    GuiInstrument(Player player, IItemInstrument instrument, Component title, @Nullable BlockPos blockInsPos) {
        super(title);
        this.player = player;
        this.instrument = instrument;
        this.buttonPushStates = new boolean[IItemInstrument.totalNotes];
        this.noteSounds = new NoteSound[IItemInstrument.totalNotes];
        this.midiHandler = new MidiHandler(this::playSound, this::stopSound);
        this.blockInsPos = blockInsPos;
        if(currentKeyboardOctave < instrument.getMinOctave()) {
            currentKeyboardOctave = instrument.getMinOctave();
        }
        else if(currentKeyboardOctave > instrument.getMaxOctave()) {
            currentKeyboardOctave = instrument.getMaxOctave();
        }
        midiHandler.currentOctave = currentKeyboardOctave;
        
        // 初始化Sky模式的八度，使用与原界面相同的八度值
        skyCurrentOctave = currentKeyboardOctave;
        
        // 初始化键盘映射
        initKeyMapping();
        
        // 初始化动画背景
        animatedBackground = new AnimatedBackground(BACKGROUND_PATH, ".png", BACKGROUND_FRAME_COUNT, BACKGROUND_FRAME_TIME);
        
        // 初始化粒子管理器
        particleManager = new NoteParticleManager();
        lastRenderTime = System.currentTimeMillis();
    }

    /**
     * 初始化键盘到按钮的映射关系
     */
    private void initKeyMapping() {
        // 第一行: y u i o p - 对应低八度的1-2-3-4-5
        keyToButtonIdMap.put(GLFW.GLFW_KEY_Y, 0);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_U, 1);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_I, 2);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_O, 3);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_P, 4);
        
        // 第二行: h j k l ; - 对应低八度的6-7和高八度的1-2-3
        keyToButtonIdMap.put(GLFW.GLFW_KEY_H, 5);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_J, 6);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_K, 7);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_L, 8);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_SEMICOLON, 9);
        
        // 第三行: n m , . / - 对应高八度的4-5-6-7和最高音的1
        keyToButtonIdMap.put(GLFW.GLFW_KEY_N, 10);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_M, 11);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_COMMA, 12);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_PERIOD, 13);
        keyToButtonIdMap.put(GLFW.GLFW_KEY_SLASH, 14);

        // 初始化和弦按键映射
        keyToChordButtonIdMap.put(GLFW.GLFW_KEY_V, 0); // 1和弦
        keyToChordButtonIdMap.put(GLFW.GLFW_KEY_B, 1); // 2和弦
        keyToChordButtonIdMap.put(GLFW.GLFW_KEY_F, 2); // 3和弦
        keyToChordButtonIdMap.put(GLFW.GLFW_KEY_G, 3); // 4和弦
        keyToChordButtonIdMap.put(GLFW.GLFW_KEY_R, 4); // 5和弦
        keyToChordButtonIdMap.put(GLFW.GLFW_KEY_T, 5); // 6和弦
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init() {
        guiBaseX = (this.width - guiWidth) / 2;
        guiBaseY = (this.height - guiHeight) / 2;
        octaveButtonX = guiBaseX - 10;

        this.addRenderableWidget(Button.builder(Component.translatable("note.upButton"), button -> increaseOctave()).
                bounds(octaveButtonX, octaveButtonY, 10, 10).
                tooltip(Tooltip.create(Component.translatable("ins.octaveTooltip"))).build());

        this.addRenderableWidget(Button.builder(Component.translatable("note.downButton"), button -> decreaseOctave()).
                bounds(octaveButtonX, octaveButtonY + 25, 10, 10).
                tooltip(Tooltip.create(Component.translatable("ins.octaveTooltip"))).build());
        
        // 添加界面切换按钮
        this.toggleInterfaceButton = Button.builder(
                Component.translatable("gui.xercamusic.toggle_sky_interface"), 
                button -> toggleInterface())
                .bounds(this.width - 100, 10, 80, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.xercamusic.toggle_sky_interface.tooltip")))
                .build();
        
        this.addRenderableWidget(this.toggleInterfaceButton);
    }

    @Override
    public void tick() {
        super.tick();
        if(blockInsPos != null && minecraft != null ){
            if(player.level().getBlockState(blockInsPos).getBlock() instanceof BlockInstrument blockIns){
                if(blockIns.getItemInstrument() != instrument){
                    minecraft.setScreen(null);
                }
            }
            else{
                minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 计算deltaTime
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRenderTime) / 1000.0f;
        lastRenderTime = currentTime;
        
        // 更新粒子
        particleManager.update(deltaTime);
        
        // 正常渲染界面
        this.renderBackground(guiGraphics);
        
        // 根据当前模式渲染相应界面
        if (isSkyInterfaceActive) {
            renderSkyInterface(guiGraphics, mouseX, mouseY, partialTicks);
        } else {
            renderOriginalInterface(guiGraphics, mouseX, mouseY, partialTicks);
        }
        
        // 渲染粒子效果
        particleManager.render(guiGraphics);
        
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    /**
     * 渲染原始界面
     */
    private void renderOriginalInterface(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 原始渲染代码
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, insGuiTextures);

        guiGraphics.blit(insGuiTextures, guiBaseX, guiBaseY, 0, 0, 0, guiWidth, guiHeight, 512, 512);

        for(int i=0; i<buttonPushStates.length; i++){
            if(buttonPushStates[i]){
                int pushedOctave = i / 12;
                int x = guiBaseX + guiMarginWidth + i*guiNoteWidth + pushedOctave;
                int y = guiBaseY + 11;
                if(pushedOctave > 3){
                    x -= 4 + 48*guiNoteWidth;
                    y = guiBaseY + guiBottomKeyboardTop + 2;
                }
                guiGraphics.blit(insGuiTextures, x, y, 0, 402, 11, 7, 82, 512, 512);
            }
        }

        int currentKeyboardOctaveDraw = Math.max(0, currentKeyboardOctave);
        int octaveHighlightX = guiBaseX + guiMarginWidth + currentKeyboardOctaveDraw * guiOctaveWidth - 1;
        int octaveHighlightY = guiBaseY + 3;
        if(currentKeyboardOctave > 3){
            octaveHighlightX -= 4 * guiOctaveWidth;
            octaveHighlightY = guiBaseY + guiBottomKeyboardTop - 6;
        }
        guiGraphics.blit(insGuiTextures, octaveHighlightX, octaveHighlightY, 0, 0, 0, guiOctaveHighlightY, guiOctaveHighlightWidth, guiOctaveHighlightHeight, 512, 512);

        for(int i=0; i<8; i++){
            if(i < instrument.getMinOctave() || i > instrument.getMaxOctave()){
                int x = guiBaseX + guiMarginWidth + i*guiOctaveWidth;
                int y = guiBaseY + 11;
                if(i > 3){
                    x -= 4 * guiOctaveWidth;
                    y = guiBaseY + guiBottomKeyboardTop + 2;
                }
                guiGraphics.blit(insGuiTextures, x, y, 0, 0, guiOctaveBlockX, guiOctaveBlockY, guiOctaveBlockWidth, guiOctaveBlockHeight, 512, 512);
            }
        }

        guiGraphics.drawCenteredString(this.font, "" + (currentKeyboardOctave), octaveButtonX + 4, octaveButtonY + 14, 0xFFFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    /**
     * 渲染Sky风格界面
     */
    private void renderSkyInterface(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 绘制动画背景
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 使用AnimatedBackground获取当前帧
        ResourceLocation currentFrame = animatedBackground.getCurrentFrame();
        
        // 背景图中心对齐
        int bgWidth = 520;
        int bgHeight = 260;
        int bgX = (this.width - bgWidth) / 2;
        int bgY = (this.height - bgHeight) / 2;
        
        guiGraphics.blit(currentFrame, bgX, bgY, 0, 0, bgWidth, bgHeight, bgWidth, bgHeight);
        
        // 使用原有的八度控制按钮和显示
        guiGraphics.drawCenteredString(this.font, "" + (skyCurrentOctave), octaveButtonX + 4, octaveButtonY + 14, 0xFFFFFFFF);
        
        // 计算布局尺寸
        int totalWidth = calcualteSkySectionWidth();
        int gridHeight = SKY_GRID_ROWS * SKY_BUTTON_SIZE + (SKY_GRID_ROWS - 1) * SKY_BUTTON_SPACING;
        int chordGridHeight = CHORD_GRID_ROWS * CHORD_BUTTON_HEIGHT + (CHORD_GRID_ROWS - 1) * CHORD_BUTTON_SPACING;
        
        // 计算起始位置，使整体居中
        int startX = (this.width - totalWidth) / 2;
        int startY = Math.max((this.height - gridHeight) / 2, (this.height - chordGridHeight) / 2);
        
        // 绘制和弦胶囊按钮
        int chordStartX = startX;
        renderChordButtons(guiGraphics, chordStartX, startY);
        
        // 绘制分隔线
        int separatorX = chordStartX + CHORD_BUTTON_WIDTH * CHORD_GRID_COLS + CHORD_BUTTON_SPACING * 2;
        guiGraphics.fill(separatorX, startY, separatorX + 2, startY + Math.max(gridHeight, chordGridHeight), 0xFFAAAAAA);
        
        // 绘制音符按钮网格
        int noteStartX = separatorX + 10; // 分隔线后留一些间距
        renderNoteButtons(guiGraphics, noteStartX, startY);
        
        // 绘制提示信息和八度信息
        guiGraphics.drawCenteredString(font, "Sky-Style Interface", this.width / 2, 20, 0xFFFFFFFF);
        guiGraphics.drawString(font, "Use [ ] or A/S keys to change octave", 10, this.height - 30, 0xFFFFFF00);
        guiGraphics.drawString(font, "Use V B F G R T keys for chords", 10, this.height - 15, 0xFFFFFF00);
    }
    
    /**
     * 计算Sky界面的总宽度
     */
    private int calcualteSkySectionWidth() {
        int chordSectionWidth = CHORD_GRID_COLS * CHORD_BUTTON_WIDTH + (CHORD_GRID_COLS - 1) * CHORD_BUTTON_SPACING;
        int noteSectionWidth = SKY_GRID_COLS * SKY_BUTTON_SIZE + (SKY_GRID_COLS - 1) * SKY_BUTTON_SPACING;
        int separatorWidth = 12; // 分隔线和间距
        
        return chordSectionWidth + separatorWidth + noteSectionWidth;
    }
    
    /**
     * 渲染和弦按钮区域 - 调整排列顺序，使R和T在最上面
     */
    private void renderChordButtons(GuiGraphics guiGraphics, int startX, int startY) {
        // 使用自定义顺序对应按钮ID: R(4),T(5)在第一行，F(2),G(3)在第二行，V(0),B(1)在第三行
        int[][] buttonIdByRowCol = {
            {4, 5}, // 第一行: R和T
            {2, 3}, // 第二行: F和G
            {0, 1}  // 第三行: V和B
        };
        
        for (int row = 0; row < CHORD_GRID_ROWS; row++) {
            for (int col = 0; col < CHORD_GRID_COLS; col++) {
                int buttonId = buttonIdByRowCol[row][col];
                if (buttonId < 6) { // 总共有6个和弦按钮
                    int x = startX + col * (CHORD_BUTTON_WIDTH + CHORD_BUTTON_SPACING);
                    int y = startY + row * (CHORD_BUTTON_HEIGHT + CHORD_BUTTON_SPACING);
                    
                    // 根据按钮状态选择纹理
                    ResourceLocation texture = chordButtonStates[buttonId] ? CHORD_BUTTON_PRESSED : CHORD_BUTTON_NORMAL;
                    
                    // 设置半透明渲染 - 确保透明度正确应用
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, BUTTON_ALPHA);
                    
                    // 应用margin和调整绘制区域以避免缝隙
                    int drawX = x + BUTTON_MARGIN;
                    int drawY = y + BUTTON_MARGIN;
                    int drawWidth = CHORD_BUTTON_WIDTH - (BUTTON_MARGIN * 2);
                    int drawHeight = CHORD_BUTTON_HEIGHT - (BUTTON_MARGIN * 2);
                    
                    // 绘制按钮 - 使用标准纹理渲染方法，避免缝隙
                    RenderSystem.setShaderTexture(0, texture);
                    guiGraphics.blit(texture, drawX, drawY, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
                    
                    // 重置透明度为不透明，以便渲染文本
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    
                    // 获取和弦信息
                    String keyName = getChordKeyName(buttonId);
                    String[] degreeNames = {"135", "246", "357", "461", "572", "613"};
                    String degreeName = degreeNames[buttonId];
                    
                    // 直接渲染和弦度数(浅蓝色)而不是根音(白色)
                    guiGraphics.drawCenteredString(this.font, degreeName, 
                            x + CHORD_BUTTON_WIDTH / 2 - this.font.width(" (" + keyName + ")") / 2, 
                            y + CHORD_BUTTON_HEIGHT + 2, 0xFF77CCFF);
                    
                    // 渲染按键名称(黄色)
                    guiGraphics.drawString(this.font, " (" + keyName + ")", 
                            x + CHORD_BUTTON_WIDTH / 2 - this.font.width(" (" + keyName + ")") / 2 + this.font.width(degreeName), 
                            y + CHORD_BUTTON_HEIGHT + 2, 0xFFFFFF00);
                }
            }
        }
    }
    
    /**
     * 渲染音符按钮区域
     */
    private void renderNoteButtons(GuiGraphics guiGraphics, int startX, int startY) {
        for (int row = 0; row < SKY_GRID_ROWS; row++) {
            for (int col = 0; col < SKY_GRID_COLS; col++) {
                int buttonId = row * SKY_GRID_COLS + col;
                if (buttonId < 15) { // 总共有15个按钮
                    int x = startX + col * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
                    int y = startY + row * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
                    
                    // 确定音符索引和对应的纹理
                    int noteIndex;
                    if (buttonId < 7) { // 低八度 1-7
                        noteIndex = buttonId;
                    } else if (buttonId < 14) { // 高八度 1-7
                        noteIndex = buttonId - 7;
                    } else { // 最高音 1
                        noteIndex = 0;
                    }
                    
                    // 根据按钮状态选择纹理
                    ResourceLocation texture = skyButtonStates[buttonId] ? 
                            NOTE_BUTTON_PRESSED[noteIndex] : 
                            NOTE_BUTTON_NORMAL[noteIndex];
                    
                    // 设置半透明渲染
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, BUTTON_ALPHA);
                    
                    // 应用margin和调整绘制区域以避免缝隙
                    int drawX = x + BUTTON_MARGIN;
                    int drawY = y + BUTTON_MARGIN;
                    int drawWidth = SKY_BUTTON_SIZE - (BUTTON_MARGIN * 2);
                    int drawHeight = SKY_BUTTON_SIZE - (BUTTON_MARGIN * 2);
                    
                    // 绘制按钮 - 使用标准纹理渲染方法，避免缝隙
                    RenderSystem.setShaderTexture(0, texture);
                    guiGraphics.blit(texture, drawX, drawY, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
                    
                    // 重置透明度为不透明，以便渲染文本
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    
                    // 音符和按键合并一行显示 - 音符白色，按键黄色
                    String[] noteNames = {"1", "2", "3", "4", "5", "6", "7"};
                    String noteName = noteNames[noteIndex];
                    String keyName = getKeyNameForButtonId(buttonId);
                    
                    // 先渲染音符(白色)
                    guiGraphics.drawCenteredString(this.font, noteName, 
                            x + SKY_BUTTON_SIZE / 2 - this.font.width(" (" + keyName + ")") / 2, 
                            y + SKY_BUTTON_SIZE + 2, 0xFFFFFFFF);
                    
                    // 再渲染按键(黄色)
                    guiGraphics.drawString(this.font, " (" + keyName + ")", 
                            x + SKY_BUTTON_SIZE / 2 - this.font.width(" (" + keyName + ")") / 2 + this.font.width(noteName), 
                            y + SKY_BUTTON_SIZE + 2, 0xFFFFFF00);
                }
            }
        }
    }

    /**
     * 处理Sky界面的点击事件 - 调整点击检测以匹配新的排列顺序
     */
    private boolean handleSkyInterfaceClick(double mouseX, double mouseY) {
        // 计算整体布局
        int totalWidth = calcualteSkySectionWidth();
        int gridHeight = SKY_GRID_ROWS * SKY_BUTTON_SIZE + (SKY_GRID_ROWS - 1) * SKY_BUTTON_SPACING;
        int chordGridHeight = CHORD_GRID_ROWS * CHORD_BUTTON_HEIGHT + (CHORD_GRID_ROWS - 1) * CHORD_BUTTON_SPACING;
        
        int startX = (this.width - totalWidth) / 2;
        int startY = Math.max((this.height - gridHeight) / 2, (this.height - chordGridHeight) / 2);
        
        // 检查和弦区域点击
        int chordStartX = startX;
        int[][] buttonIdByRowCol = {
            {4, 5}, // 第一行: R和T
            {2, 3}, // 第二行: F和G
            {0, 1}  // 第三行: V和B
        };
        
        for (int row = 0; row < CHORD_GRID_ROWS; row++) {
            for (int col = 0; col < CHORD_GRID_COLS; col++) {
                int buttonId = buttonIdByRowCol[row][col];
                if (buttonId < 6) {
                    int x = chordStartX + col * (CHORD_BUTTON_WIDTH + CHORD_BUTTON_SPACING);
                    int y = startY + row * (CHORD_BUTTON_HEIGHT + CHORD_BUTTON_SPACING);
                    
                    if (mouseX >= x && mouseX < x + CHORD_BUTTON_WIDTH && 
                        mouseY >= y && mouseY < y + CHORD_BUTTON_HEIGHT) {
                        // 点击了和弦按钮
                        playChord(buttonId);
                        return true;
                    }
                }
            }
        }
        
        // 检查音符区域点击
        int separatorX = chordStartX + CHORD_BUTTON_WIDTH * CHORD_GRID_COLS + CHORD_BUTTON_SPACING * 2;
        int noteStartX = separatorX + 10;
        
        for (int row = 0; row < SKY_GRID_ROWS; row++) {
            for (int col = 0; col < SKY_GRID_COLS; col++) {
                int buttonId = row * SKY_GRID_COLS + col;
                if (buttonId < 15) {
                    int x = noteStartX + col * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
                    int y = startY + row * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
                    
                    if (mouseX >= x && mouseX < x + SKY_BUTTON_SIZE && 
                        mouseY >= y && mouseY < y + SKY_BUTTON_SIZE) {
                        // 点击了按钮
                        playSkyNote(buttonId);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 处理Sky界面的释放事件 - 调整释放检测以匹配新的排列顺序
     */
    private boolean handleSkyInterfaceRelease(double mouseX, double mouseY) {
        // 计算整体布局
        int totalWidth = calcualteSkySectionWidth();
        int gridHeight = SKY_GRID_ROWS * SKY_BUTTON_SIZE + (SKY_GRID_ROWS - 1) * SKY_BUTTON_SPACING;
        int chordGridHeight = CHORD_GRID_ROWS * CHORD_BUTTON_HEIGHT + (CHORD_GRID_ROWS - 1) * CHORD_BUTTON_SPACING;
        
        int startX = (this.width - totalWidth) / 2;
        int startY = Math.max((this.height - gridHeight) / 2, (this.height - chordGridHeight) / 2);
        
        // 检查和弦区域释放
        int chordStartX = startX;
        int[][] buttonIdByRowCol = {
            {4, 5}, // 第一行: R和T
            {2, 3}, // 第二行: F和G
            {0, 1}  // 第三行: V和B
        };
        
        for (int row = 0; row < CHORD_GRID_ROWS; row++) {
            for (int col = 0; col < CHORD_GRID_COLS; col++) {
                int buttonId = buttonIdByRowCol[row][col];
                if (buttonId < 6) {
                    int x = chordStartX + col * (CHORD_BUTTON_WIDTH + CHORD_BUTTON_SPACING);
                    int y = startY + row * (CHORD_BUTTON_HEIGHT + CHORD_BUTTON_SPACING);
                    
                    if (mouseX >= x && mouseX < x + CHORD_BUTTON_WIDTH && 
                        mouseY >= y && mouseY < y + CHORD_BUTTON_HEIGHT) {
                        // 释放了和弦按钮
                        stopChord(buttonId);
                        return true;
                    }
                }
            }
        }
        
        // 检查音符区域释放
        int separatorX = chordStartX + CHORD_BUTTON_WIDTH * CHORD_GRID_COLS + CHORD_BUTTON_SPACING * 2;
        int noteStartX = separatorX + 10;
        
        for (int row = 0; row < SKY_GRID_ROWS; row++) {
            for (int col = 0; col < SKY_GRID_COLS; col++) {
                int buttonId = row * SKY_GRID_COLS + col;
                if (buttonId < 15) {
                    int x = noteStartX + col * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
                    int y = startY + row * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING);
                    
                    if (mouseX >= x && mouseX < x + SKY_BUTTON_SIZE && 
                        mouseY >= y && mouseY < y + SKY_BUTTON_SIZE) {
                        // 释放了按钮
                        stopSkyNote(buttonId);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 播放和弦
     */
    private void playChord(int chordId) {
        if (chordId >= 0 && chordId < 6) {
            chordButtonStates[chordId] = true;
            
            // 和弦八度，比单音区域低两个八度
            int chordOctave = Math.max(instrument.getMinOctave(), skyCurrentOctave - 2);
            
            // 获取该和弦的所有音符偏移量
            int[] offsets = chordNoteOffsets[chordId];
            
            // 播放和弦中的每个音符
            for (int offset : offsets) {
                int noteId = offset + 12 * chordOctave;
                if (noteId >= 0 && noteId < buttonPushStates.length) {
                    playSound(noteId);
                }
            }
            
            // 创建粒子效果 - 为和弦按钮添加特殊ID (100+chordId)
            int particleButtonId = 100 + chordId;
            Point center = getChordCenterPosition(chordId);
            particleManager.createParticlesForButtonPress(particleButtonId, center.x, center.y);
        }
    }
    
    /**
     * 停止和弦
     */
    private void stopChord(int chordId) {
        if (chordId >= 0 && chordId < 6) {
            chordButtonStates[chordId] = false;
            
            // 和弦八度，比单音区域低两个八度
            int chordOctave = Math.max(instrument.getMinOctave(), skyCurrentOctave - 2);
            
            // 获取该和弦的所有音符偏移量
            int[] offsets = chordNoteOffsets[chordId];
            
            // 停止和弦中的每个音符
            for (int offset : offsets) {
                int noteId = offset + 12 * chordOctave;
                if (noteId >= 0 && noteId < buttonPushStates.length) {
                    stopSound(noteId);
                }
            }
            
            // 处理粒子效果消失
            int particleButtonId = 100 + chordId;
            particleManager.handleButtonRelease(particleButtonId);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        // 先尝试处理按钮点击事件
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        
        // 如果Sky界面激活，使用Sky界面的点击逻辑
        if (isSkyInterfaceActive) {
            return handleSkyInterfaceClick(mouseX, mouseY);
        }
        
        // 否则使用原有逻辑
        int noteId = noteIdFromPos((int)Math.round(mouseX), (int)Math.round(mouseY));
        playSound(noteId);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        // 先尝试处理按钮释放事件
        super.mouseReleased(mouseX, mouseY, mouseButton);
        
        // 如果Sky界面激活，使用Sky界面的释放逻辑
        if (isSkyInterfaceActive) {
            return handleSkyInterfaceRelease(mouseX, mouseY);
        }
        
        // 否则使用原有逻辑
        int noteId = noteIdFromPos((int)Math.round(mouseX), (int)Math.round(mouseY));
        stopSound(noteId);
        return true;
    }

    @Override
    public boolean mouseDragged(double posX, double posY, int mouseButton, double deltaX, double deltaY) {
//        XercaMusic.LOGGER.info("Drag pos: " + posX + " del: " + deltaX);
        int mouseX = (int)Math.round(posX);
        int mouseY = (int)Math.round(posY);
        int prevMouseX = (int)Math.round(posX - deltaX);
        int prevMouseY = (int)Math.round(posY - deltaY);

        int prevNoteId = noteIdFromPos(prevMouseX, prevMouseY);
        int currentNoteId = noteIdFromPos(mouseX, mouseY);
        if(prevNoteId != currentNoteId){
            stopSound(prevNoteId);
            playSound(currentNoteId);
        }

        return super.mouseDragged(posX, posY, mouseButton, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        // 先处理通用按键
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(null);
            return true;
        }
        
        if (isSkyInterfaceActive) {
            // 处理和弦按键
            Integer chordId = keyToChordButtonIdMap.get(keyCode);
            if (chordId != null) {
                playChord(chordId);
                return true;
            }
            
            // 处理Sky模式的八度变化快捷键
            if (keyCode == GLFW.GLFW_KEY_LEFT_BRACKET) {
                decreaseOctave(); // 使用通用的八度减少方法
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) {
                increaseOctave(); // 使用通用的八度增加方法
                return true;
            }
            
            // Sky界面的键盘事件处理
            Integer buttonId = keyToButtonIdMap.get(keyCode);
            if (buttonId != null) {
                playSkyNote(buttonId);
                return true;
            }
            
            // 处理通用按键
            if (keyCode == GLFW.GLFW_KEY_A) {
                decreaseOctave();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_S) {
                increaseOctave();
                return true;
            }
        } else {
            // 原有键盘事件处理逻辑
            setFocused(null);
            super.keyPressed(keyCode, scanCode, modifiers);

            int firstScanCode = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_Q);
            int lastScanCode = firstScanCode + 11;
            if (scanCode >= firstScanCode && scanCode <= lastScanCode) {
                int noteId = scanCode - firstScanCode + 12 * Math.max(0, currentKeyboardOctave);
                playSound(noteId);
            }

            if(keyCode == GLFW.GLFW_KEY_A){
                decreaseOctave();
            }
            else if(keyCode == GLFW.GLFW_KEY_S){
                increaseOctave();
            }
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers){
        if (isSkyInterfaceActive) {
            // 处理和弦按键释放
            Integer chordId = keyToChordButtonIdMap.get(keyCode);
            if (chordId != null) {
                stopChord(chordId);
                return true;
            }
            
            // Sky界面的键盘释放事件处理
            Integer buttonId = keyToButtonIdMap.get(keyCode);
            if (buttonId != null) {
                stopSkyNote(buttonId);
                return true;
            }
        } else {
            // 原有逻辑
            int firstScanCode = GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_Q);
            int lastScanCode = firstScanCode + 11;
            if (scanCode >= firstScanCode && scanCode <= lastScanCode) {
                int noteId = scanCode - firstScanCode + 12 * Math.max(0, currentKeyboardOctave);
                stopSound(noteId);
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        midiHandler.closeDevices();
        // 清除粒子效果
        particleManager.clearAll();
    }
    
    /**
     * 切换演奏界面模式
     */
    private void toggleInterface() {
        // 切换界面模式
        isSkyInterfaceActive = !isSkyInterfaceActive;
        
        // 停止所有声音，防止状态混乱
        stopAllSounds();
        
        // 重置所有按钮状态
        for (int i = 0; i < skyButtonStates.length; i++) {
            skyButtonStates[i] = false;
        }
        
        for (int i = 0; i < chordButtonStates.length; i++) {
            chordButtonStates[i] = false;
        }
        
        // 清除所有粒子效果
        particleManager.clearAll();
        
        // 更新按钮文本
        toggleInterfaceButton.setMessage(
                Component.translatable(isSkyInterfaceActive ? 
                        "gui.xercamusic.toggle_original_interface" : 
                        "gui.xercamusic.toggle_sky_interface"));
    }

    private int noteIdFromPos(int mouseX, int mouseY) {
        int buttonBaseX = guiBaseX + guiMarginWidth;
        if(mouseX >= buttonBaseX && mouseX <= buttonBaseX + guiWidth - 14
                && mouseY >= guiBaseY + 9 && mouseY <= guiBaseY + guiHeight - 10
                && (mouseY < guiBaseY + guiTopKeyboardBottom || mouseY > guiBaseY + guiBottomKeyboardTop)) {
            int octavePlus = (mouseY < guiBaseY + guiTopKeyboardBottom) ? 0 : 4;
            int octave = octavePlus + (mouseX - buttonBaseX) / (guiOctaveWidth);
            int note = ((mouseX - buttonBaseX) % guiOctaveWidth) / guiNoteWidth;
            if (note < 12) {
                return octave * 12 + note;
            }
        }
        return -1;
    }

    private void playSound(int noteId){
        playSound(new MidiHandler.MidiData(noteId, 0.8f));
    }

    private void playSound(MidiHandler.MidiData data){
        int noteId = data.noteId();

        if(noteId >= 0 && noteId < buttonPushStates.length && !buttonPushStates[noteId]) {
            int note = IItemInstrument.idToNote(noteId);

            IItemInstrument.InsSound noteSound = instrument.getSound(note);
            if (noteSound == null) {
                return;
            }
            noteSounds[noteId] = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ClientStuff.playNote(noteSound.sound, player.getX(), player.getY(), player.getZ(), data.volume(), noteSound.pitch));
            player.level().addParticle(ParticleTypes.NOTE, player.getX(), player.getY() + 2.2D, player.getZ(), note / 24.0D, 0.0D, 0.0D);
            buttonPushStates[noteId] = true;

            SingleNotePacket pack = new SingleNotePacket(note, instrument, false, data.volume());
            XercaMusic.NETWORK_HANDLER.sendToServer(pack);
        }
    }

    private void stopSound(int noteId){
        if(noteId >= 0 && noteId < buttonPushStates.length && buttonPushStates[noteId]) {
            if (noteSounds[noteId] != null) {
                noteSounds[noteId].stopSound();
                noteSounds[noteId] = null;
                buttonPushStates[noteId] = false;

                int note = IItemInstrument.idToNote(noteId);
                SingleNotePacket pack = new SingleNotePacket(note, instrument, true);
                XercaMusic.NETWORK_HANDLER.sendToServer(pack);
            }
        }
    }

    private void stopAllSounds(){
        for(int noteId=0; noteId<buttonPushStates.length; noteId++) {
            stopSound(noteId);
        }
    }

    private void decreaseOctave() {
        if (isSkyInterfaceActive) {
            if(skyCurrentOctave > instrument.getMinOctave()){
                skyCurrentOctave--;
                stopAllSounds();
                // 重置所有Sky按钮状态
                for (int i = 0; i < skyButtonStates.length; i++) {
                    skyButtonStates[i] = false;
                }
            }
        } else {
            if(currentKeyboardOctave > instrument.getMinOctave()){
                currentKeyboardOctave--;
                midiHandler.currentOctave = currentKeyboardOctave;
                stopAllSounds();
            }
        }
    }

    private void increaseOctave() {
        if (isSkyInterfaceActive) {
            // 考虑到最高音符映射已经包含了2个八度的偏移，需要限制最高八度
            if(skyCurrentOctave < instrument.getMaxOctave() - 2){
                skyCurrentOctave++;
                stopAllSounds();
                // 重置所有Sky按钮状态
                for (int i = 0; i < skyButtonStates.length; i++) {
                    skyButtonStates[i] = false;
                }
            }
        } else {
            if(currentKeyboardOctave < instrument.getMaxOctave()){
                currentKeyboardOctave++;
                midiHandler.currentOctave = currentKeyboardOctave;
                stopAllSounds();
            }
        }
    }
    
    /**
     * 根据按钮ID获取对应的键名
     */
    private String getKeyNameForButtonId(int buttonId) {
        switch (buttonId) {
            case 0: return "Y";
            case 1: return "U";
            case 2: return "I";
            case 3: return "O";
            case 4: return "P";
            case 5: return "H";
            case 6: return "J";
            case 7: return "K";
            case 8: return "L";
            case 9: return ";";
            case 10: return "N";
            case 11: return "M";
            case 12: return ",";
            case 13: return ".";
            case 14: return "/";
            default: return "";
        }
    }
    
    /**
     * 播放Sky界面的音符
     */
    private void playSkyNote(int buttonId) {
        if (buttonId >= 0 && buttonId < 15) {
            skyButtonStates[buttonId] = true;
            
            // 计算实际音符ID
            int baseNoteId = skyNoteMapping[buttonId];
            int actualNoteId = baseNoteId + 12 * skyCurrentOctave;
            
            if (actualNoteId >= 0 && actualNoteId < buttonPushStates.length) {
                // 播放音符
                playSound(actualNoteId);
                
                // 创建粒子效果
                Point center = getNoteCenterPosition(buttonId);
                particleManager.createParticlesForButtonPress(buttonId, center.x, center.y);
            }
        }
    }
    
    private void stopSkyNote(int buttonId) {
        if (buttonId >= 0 && buttonId < 15) {
            skyButtonStates[buttonId] = false;
            
            // 计算实际音符ID
            int baseNoteId = skyNoteMapping[buttonId];
            int actualNoteId = baseNoteId + 12 * skyCurrentOctave;
            
            if (actualNoteId >= 0 && actualNoteId < buttonPushStates.length) {
                // 停止音符
                stopSound(actualNoteId);
                
                // 处理粒子效果消失
                particleManager.handleButtonRelease(buttonId);
            }
        }
    }
    
    /**
     * 获取和弦名称 - 添加和弦度数信息
     */
    private String getChordName(int buttonId) {
        String[] rootNames = {"1", "2", "3", "4", "5", "6"};
        String[] degreeNames = {"(135)", "(246)", "(357)", "(461)", "(572)", "(613)"};
        if (buttonId >= 0 && buttonId < rootNames.length) {
            return rootNames[buttonId] + " " + degreeNames[buttonId];
        }
        return "";
    }
    
    /**
     * 获取和弦的键名
     */
    private String getChordKeyName(int buttonId) {
        switch (buttonId) {
            case 0: return "V";
            case 1: return "B";
            case 2: return "F";
            case 3: return "G";
            case 4: return "R";
            case 5: return "T";
            default: return "";
        }
    }
    
    // 添加以下辅助方法来获取按钮中心位置
    private Point getNoteCenterPosition(int buttonId) {
        if (isSkyInterfaceActive) {
            // 计算Sky模式下音符按钮的位置
            int totalWidth = calcualteSkySectionWidth();
            int gridHeight = SKY_GRID_ROWS * SKY_BUTTON_SIZE + (SKY_GRID_ROWS - 1) * SKY_BUTTON_SPACING;
            int chordGridHeight = CHORD_GRID_ROWS * CHORD_BUTTON_HEIGHT + (CHORD_GRID_ROWS - 1) * CHORD_BUTTON_SPACING;
            
            int startX = (this.width - totalWidth) / 2;
            int startY = Math.max((this.height - gridHeight) / 2, (this.height - chordGridHeight) / 2);
            int separatorX = startX + CHORD_BUTTON_WIDTH * CHORD_GRID_COLS + CHORD_BUTTON_SPACING * 2;
            int noteStartX = separatorX + 10;
            
            int row = buttonId / SKY_GRID_COLS;
            int col = buttonId % SKY_GRID_COLS;
            
            int x = noteStartX + col * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING) + SKY_BUTTON_SIZE / 2;
            int y = startY + row * (SKY_BUTTON_SIZE + SKY_BUTTON_SPACING) + SKY_BUTTON_SIZE / 2;
            
            return new Point(x, y);
        } else {
            // 原始界面下的按键位置 - 简化，返回屏幕中央
            return new Point(width / 2, height / 2);
        }
    }
    
    private Point getChordCenterPosition(int chordButtonId) {
        if (isSkyInterfaceActive) {
            // 计算Sky模式下和弦按钮的位置
            int totalWidth = calcualteSkySectionWidth();
            int gridHeight = SKY_GRID_ROWS * SKY_BUTTON_SIZE + (SKY_GRID_ROWS - 1) * SKY_BUTTON_SPACING;
            int chordGridHeight = CHORD_GRID_ROWS * CHORD_BUTTON_HEIGHT + (CHORD_GRID_ROWS - 1) * CHORD_BUTTON_SPACING;
            
            int startX = (this.width - totalWidth) / 2;
            int startY = Math.max((this.height - gridHeight) / 2, (this.height - chordGridHeight) / 2);
            
            // 找出chordButtonId对应的行和列
            int row = -1, col = -1;
            int[][] buttonIdByRowCol = {
                {4, 5}, // 第一行: R和T
                {2, 3}, // 第二行: F和G
                {0, 1}  // 第三行: V和B
            };
            
            for (int r = 0; r < CHORD_GRID_ROWS; r++) {
                for (int c = 0; c < CHORD_GRID_COLS; c++) {
                    if (buttonIdByRowCol[r][c] == chordButtonId) {
                        row = r;
                        col = c;
                        break;
                    }
                }
                if (row >= 0) break;
            }
            
            if (row >= 0 && col >= 0) {
                int x = startX + col * (CHORD_BUTTON_WIDTH + CHORD_BUTTON_SPACING) + CHORD_BUTTON_WIDTH / 2;
                int y = startY + row * (CHORD_BUTTON_HEIGHT + CHORD_BUTTON_SPACING) + CHORD_BUTTON_HEIGHT / 2;
                return new Point(x, y);
            }
        }
        
        // 默认位置
        return new Point(width / 2, height / 2);
    }
    
    // 添加辅助类Point
    private static class Point {
        public final int x;
        public final int y;
        
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
