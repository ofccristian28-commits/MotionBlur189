package br.motionblur;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class MotionBlurGui extends GuiScreen {

    private final MotionBlurMod mod;
    private GuiButton toggleButton;

    public MotionBlurGui(MotionBlurMod mod) {
        this.mod = mod;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        String label = "Motion Blur: " + (mod.isEnabled() ? "ON" : "OFF");
        toggleButton = new GuiButton(0, this.width / 2 - 75, this.height / 2 - 10, 150, 20, label);
        this.buttonList.add(toggleButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mod.setEnabled(!mod.isEnabled());
            button.displayString = "Motion Blur: " + (mod.isEnabled() ? "ON" : "OFF");
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "Menu Motion Blur", this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
