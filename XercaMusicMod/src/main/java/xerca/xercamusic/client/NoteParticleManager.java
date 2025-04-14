package xerca.xercamusic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

public class NoteParticleManager {
    private final List<NoteParticle> particles = new ArrayList<>();
    private final Map<Integer, Long> buttonPressTime = new HashMap<>();
    private final Random random = new Random();
    
    // 跟踪当前已按下的按钮集合，避免长按时重复生成粒子
    private final Set<Integer> activeButtons = new HashSet<>();
    
    // 特殊粒子ID范围
    private static final int SPECIAL_PARTICLE_MIN_ID = 19;
    private static final int SPECIAL_PARTICLE_MAX_ID = 26;
    private boolean hasSpecialParticle = false;
    
    /**
     * 创建按键按下的粒子效果
     */
    public void createParticlesForButtonPress(int buttonId, float centerX, float centerY) {
        // 如果按钮已经被按下，则不创建新的粒子
        if (activeButtons.contains(buttonId)) {
            return;
        }
        
        // 记录按钮为激活状态
        activeButtons.add(buttonId);
        
        // 记录按下时间
        buttonPressTime.put(buttonId, System.currentTimeMillis());
        
        // 生成3-6个粒子
        int particleCount = 3 + random.nextInt(4);
        
        for (int i = 0; i < particleCount; i++) {
            int textureId;
            
            // 为特殊粒子设置条件
            if (!hasSpecialParticle && random.nextFloat() < 0.3f) { // 30%几率生成特殊粒子
                textureId = SPECIAL_PARTICLE_MIN_ID + random.nextInt(SPECIAL_PARTICLE_MAX_ID - SPECIAL_PARTICLE_MIN_ID + 1);
                hasSpecialParticle = true;
            } else {
                // 普通粒子(1-18)
                textureId = 1 + random.nextInt(18);
            }
            
            // 初始化时设置默认生命周期，将在按键释放时更新
            NoteParticle particle = new NoteParticle(buttonId, centerX, centerY, textureId, 2.0f);
            particles.add(particle);
        }
    }
    
    /**
     * 当按键释放时，设置粒子的实际生命周期
     */
    public void handleButtonRelease(int buttonId) {
        // 从激活按钮集合中移除
        activeButtons.remove(buttonId);
        
        Long pressTime = buttonPressTime.remove(buttonId);
        if (pressTime == null) {
            return; // 没有找到对应的按下时间，可能是误调用
        }
        
        // 计算按住时间(秒)
        float heldTime = (System.currentTimeMillis() - pressTime) / 1000.0f;
        
        // 计算剩余生命周期: 0.4 + (按住时间 * 0.1)秒
        float remainingLifetime = 0.4f + (heldTime * 0.1f);
        
        // 更新与该按钮关联的所有粒子
        for (NoteParticle particle : particles) {
            if (particle.getButtonId() == buttonId) {
                particle.setLifetime(remainingLifetime);
            }
        }
    }
    
    /**
     * 更新所有粒子
     */
    public void update(float deltaTime) {
        // 更新所有粒子并移除已经消亡的粒子
        Iterator<NoteParticle> iterator = particles.iterator();
        boolean foundSpecial = false;
        
        while (iterator.hasNext()) {
            NoteParticle particle = iterator.next();
            
            if (!particle.update(deltaTime)) {
                iterator.remove();
                
                // 如果是特殊粒子被移除，重置标志
                if (particle.getTextureId() >= SPECIAL_PARTICLE_MIN_ID && 
                    particle.getTextureId() <= SPECIAL_PARTICLE_MAX_ID) {
                    hasSpecialParticle = false;
                }
            } else {
                // 检查是否存在特殊粒子
                if (particle.getTextureId() >= SPECIAL_PARTICLE_MIN_ID && 
                    particle.getTextureId() <= SPECIAL_PARTICLE_MAX_ID) {
                    foundSpecial = true;
                }
            }
        }
        
        // 更新特殊粒子状态
        hasSpecialParticle = foundSpecial;
    }
    
    /**
     * 渲染所有粒子
     */
    public void render(GuiGraphics guiGraphics) {
        // 启用混合模式以支持透明度
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        for (NoteParticle particle : particles) {
            // 设置透明度
            float alpha = particle.getAlpha();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            
            // 获取粒子位置和大小
            float x = particle.getX() - particle.getSize() / 2;
            float y = particle.getY() - particle.getSize() / 2;
            float size = particle.getSize();
            
            // 渲染粒子
            guiGraphics.blit(particle.getTexture(), (int)x, (int)y, 0, 0, (int)size, (int)size, (int)size, (int)size);
        }
        
        // 重置颜色
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    /**
     * 清除所有激活状态和粒子
     * 在界面模式切换或退出时调用
     */
    public void clearAll() {
        particles.clear();
        activeButtons.clear();
        buttonPressTime.clear();
        hasSpecialParticle = false;
    }
}
