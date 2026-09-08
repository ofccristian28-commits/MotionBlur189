package br.motionblur;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;

@Mod(
        modid = MotionBlurMod.MODID,
        name = MotionBlurMod.NAME,
        version = MotionBlurMod.VERSION,
        clientSideOnly = true
)
public class MotionBlurMod {
    public static final String MODID = "motionblur189";
    public static final String NAME = "Motion Blur 1.8.9";
    public static final String VERSION = "1.2";

    private static final int HISTORY_COUNT = 3;
    private final Minecraft mc = Minecraft.getMinecraft();

    private KeyBinding toggleKey;
    private boolean enabled = true;
    private int strength = 65;

    private final int[] historyTextures = new int[HISTORY_COUNT];
    private int historyWidth = -1;
    private int historyHeight = -1;
    private int historyIndex = 0;
    private boolean historyReady = false;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        toggleKey = new KeyBinding(
                "Motion Blur: Toggle",
                Keyboard.KEY_F6,
                "Motion Blur"
        );

        ClientRegistry.registerKeyBinding(toggleKey);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (toggleKey != null && toggleKey.isPressed()) {
            enabled = !enabled;
            historyReady = false;
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        if (!enabled || mc.gameSettings.showDebugInfo) {
            return;
        }

        if (mc.currentScreen != null || mc.theWorld == null) {
            return;
        }

        int width = mc.displayWidth;
        int height = mc.displayHeight;

        if (width <= 0 || height <= 0) {
            return;
        }

        ensureTextures(width, height);
        captureCurrentFrame(width, height);

        if (historyReady) {
            drawHistory(width, height);
        } else {
            historyReady = true;
        }

        historyIndex = (historyIndex + 1) % HISTORY_COUNT;
    }

    private void ensureTextures(int width, int height) {
        if (width == historyWidth && height == historyHeight && historyTextures[0] != 0) {
            return;
        }

        deleteTextures();

        historyWidth = width;
        historyHeight = height;
        historyIndex = 0;
        historyReady = false;

        for (int i = 0; i < HISTORY_COUNT; i++) {
            historyTextures[i] = GL11.glGenTextures();

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, historyTextures[i]);

            GL11.glTexParameteri(
                    GL11.GL_TEXTURE_2D,
                    GL11.GL_TEXTURE_MIN_FILTER,
                    GL11.GL_LINEAR
            );

            GL11.glTexParameteri(
                    GL11.GL_TEXTURE_2D,
                    GL11.GL_TEXTURE_MAG_FILTER,
                    GL11.GL_LINEAR
            );

            // Forge 1.8.9/LWJGL 2 exposes this constant through GL12.
            GL11.glTexParameteri(
                    GL11.GL_TEXTURE_2D,
                    GL11.GL_TEXTURE_WRAP_S,
                    GL12.GL_CLAMP_TO_EDGE
            );

            GL11.glTexParameteri(
                    GL11.GL_TEXTURE_2D,
                    GL11.GL_TEXTURE_WRAP_T,
                    GL12.GL_CLAMP_TO_EDGE
            );

            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    width,
                    height,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    (ByteBuffer) null
            );
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void captureCurrentFrame(int width, int height) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, historyTextures[historyIndex]);

        GL11.glCopyTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                0,
                0,
                width,
                height
        );

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void drawHistory(int width, int height) {
        GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT |
                GL11.GL_COLOR_BUFFER_BIT |
                GL11.GL_TEXTURE_BIT |
                GL11.GL_DEPTH_BUFFER_BIT
        );

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, -1, 1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        float baseAlpha = strength / 100.0F;

        for (int sample = 1; sample <= HISTORY_COUNT; sample++) {
            int index = (historyIndex - sample + HISTORY_COUNT) % HISTORY_COUNT;

            if (historyTextures[index] == 0) {
                continue;
            }

            float alpha =
                    baseAlpha *
                    (1.0F - ((sample - 1) / (float) HISTORY_COUNT)) *
                    0.55F;

            if (alpha <= 0.0F) {
                continue;
            }

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, historyTextures[index]);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);

            GL11.glBegin(GL11.GL_QUADS);

            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex2f(0.0F, 0.0F);

            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex2f(width, 0.0F);

            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex2f(width, height);

            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex2f(0.0F, height);

            GL11.glEnd();
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glPopAttrib();
    }

    private void deleteTextures() {
        for (int i = 0; i < historyTextures.length; i++) {
            if (historyTextures[i] != 0) {
                GL11.glDeleteTextures(historyTextures[i]);
                historyTextures[i] = 0;
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getStrength() {
        return strength;
    }
}
