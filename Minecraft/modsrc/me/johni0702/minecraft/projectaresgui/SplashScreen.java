package me.johni0702.minecraft.projectaresgui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.src.GuiIngame;
import net.minecraft.src.ScaledResolution;

public class SplashScreen 
{
	public static final SplashScreen YOU_WON = new SplashScreen(0,0,0,0);
	public static final SplashScreen YOU_LOST = new SplashScreen(0,0,0,0);
	public static final SplashScreen START = new SplashScreen(0,0,0,0);
	
	private int u,v,w,h;
	
	private SplashScreen(int u, int v, int w, int h)
	{
		this.u = u;
		this.v = v;
		this.w = w;
		this.h = h;
	}
	
	public void draw(GuiIngame guiIngame) 
	{
		Minecraft mc = Minecraft.getMinecraft();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.renderEngine.getTexture("/gui/splashscreens.png"));
        ScaledResolution scaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
		guiIngame.drawTexturedModalRect(width/2-w/2, height/4-h/2, u, v, w, h);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.renderEngine.getTexture("/gui/icons.png"));
	}
}
