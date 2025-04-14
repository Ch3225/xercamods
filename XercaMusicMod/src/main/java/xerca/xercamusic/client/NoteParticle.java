package xerca.xercamusic.client;

import net.minecraft.resources.ResourceLocation;
import xerca.xercamusic.common.XercaMusic;

import java.util.Random;

public class NoteParticle {
    private static final Random random = new Random();
    
    // 粒子的位置
    private float x;
    private float y;
    
    // 粒子的移动速度
    private float velocityX;
    private float velocityY;
    
    // 粒子的生命周期相关
    private float maxLifetime; // 总生命周期(秒)
    private float lifetime; // 当前剩余生命(秒)
    private float initialLifetime; // 初始生命周期(记录用于alpha计算)
    
    // 粒子的中心和移动范围
    private final float centerX;
    private final float centerY;
    private final float maxDistance; // 最大移动距离
    
    // 关联的按钮ID
    private final int buttonId;
    
    // 粒子的贴图ID
    private final int textureId;
    
    // 创建粒子的纹理路径
    private final ResourceLocation textureLocation;
    
    // 粒子大小
    private final float size;
    
    /**
     * 创建一个新的音符粒子
     */
    public NoteParticle(int buttonId, float centerX, float centerY, int textureId, float lifetime) {
        this.buttonId = buttonId;
        this.centerX = centerX;
        this.centerY = centerY;
        this.textureId = textureId;
        this.maxLifetime = lifetime;
        this.lifetime = lifetime;
        this.initialLifetime = lifetime;
        
        // 生成随机大小 (5-10像素)
        this.size = 5 + random.nextFloat() * 5;
        
        // 使用新的文件命名格式：简单的数字.png
        this.textureLocation = new ResourceLocation(XercaMusic.MODID, 
                "textures/gui/sky/partical/" + textureId + ".png");
        
        // 在中心点周围随机位置生成
        float offset = 5 + random.nextFloat() * 15; // 5-20像素范围
        float angle = random.nextFloat() * (float)(Math.PI * 2); // 随机角度
        
        this.x = centerX + (float)Math.cos(angle) * offset;
        this.y = centerY + (float)Math.sin(angle) * offset;
        
        // 设置随机但较慢的移动速度
        this.velocityX = (random.nextFloat() - 0.5f) * 2f; // -1到1的随机速度
        this.velocityY = (random.nextFloat() - 0.5f) * 2f;
        
        // 最大移动距离
        this.maxDistance = 20f;
    }
    
    /**
     * 更新粒子状态
     */
    public boolean update(float deltaTime) {
        // 更新生命周期
        lifetime -= deltaTime;
        if (lifetime <= 0) {
            return false; // 粒子已消亡
        }
        
        // 更新位置
        x += velocityX * deltaTime * 10; // 调整速度系数
        y += velocityY * deltaTime * 10;
        
        // 检查是否超出最大范围，如果是则反弹或调整方向
        float distanceFromCenter = (float)Math.sqrt(
                Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
        
        if (distanceFromCenter > maxDistance) {
            // 计算从中心指向粒子的向量
            float dx = x - centerX;
            float dy = y - centerY;
            
            // 标准化向量
            float length = (float)Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
            
            // 将粒子拉回范围内
            x = centerX + dx * maxDistance;
            y = centerY + dy * maxDistance;
            
            // 反转速度方向(加一点随机性)
            velocityX = -velocityX * (0.8f + random.nextFloat() * 0.2f);
            velocityY = -velocityY * (0.8f + random.nextFloat() * 0.2f);
        }
        
        return true;
    }
    
    /**
     * 获取粒子的透明度
     */
    public float getAlpha() {
        // 生命周期末尾时渐变消失
        return Math.min(1.0f, lifetime / initialLifetime);
    }
    
    /**
     * 设置粒子的剩余生命周期
     */
    public void setLifetime(float newLifetime) {
        this.lifetime = newLifetime;
        this.maxLifetime = newLifetime;
        this.initialLifetime = newLifetime;
    }
    
    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public float getSize() { return size; }
    public ResourceLocation getTexture() { return textureLocation; }
    public int getTextureId() { return textureId; }
    public int getButtonId() { return buttonId; }
}
