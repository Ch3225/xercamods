package xerca.xercamusic.client;  // 修正包名，原本是 main.java.xerca.xercamusic.client

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xerca.xercamusic.common.XercaMusic;

/**
 * 处理背景动画的实用类
 * 如果GIF背景无法直接播放，可以将GIF分解为多个帧，然后使用此类模拟动画
 */
@OnlyIn(Dist.CLIENT)
public class AnimatedBackground {
    private final ResourceLocation[] frames;
    private final int frameTime; // 每帧持续时间(毫秒)
    private final int frameCount;
    private long lastFrameTime;
    private int currentFrameIndex = 0;
    
    /**
     * 创建一个帧分割的动画背景
     * @param basePath 帧文件的基本路径，例如 "textures/gui/sky/background/"
     * @param extension 帧文件扩展名，例如 ".png" 
     * @param frameCount 总帧数
     * @param frameTime 每帧持续时间(毫秒)
     */
    public AnimatedBackground(String basePath, String extension, int frameCount, int frameTime) {
        this.frameCount = frameCount;
        this.frameTime = frameTime;
        this.frames = new ResourceLocation[frameCount];
        
        // 加载所有帧 - 修正为直接使用数字命名，不添加下划线
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new ResourceLocation(XercaMusic.MODID, basePath + i + extension);
        }
        
        this.lastFrameTime = System.currentTimeMillis();
    }
    
    /**
     * 获取当前应该显示的帧纹理
     * @return 当前帧的ResourceLocation
     */
    public ResourceLocation getCurrentFrame() {
        // 检查是否应该更新帧
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > frameTime) {
            currentFrameIndex = (currentFrameIndex + 1) % frameCount;
            lastFrameTime = currentTime;
        }
        
        return frames[currentFrameIndex];
    }
    
    /**
     * 重置动画到第一帧
     */
    public void resetAnimation() {
        currentFrameIndex = 0;
        lastFrameTime = System.currentTimeMillis();
    }
}
