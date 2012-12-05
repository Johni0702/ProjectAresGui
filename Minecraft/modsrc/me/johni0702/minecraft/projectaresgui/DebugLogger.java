package me.johni0702.minecraft.projectaresgui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import net.minecraft.client.Minecraft;
import net.minecraft.src.ModLoader;
import net.minecraftforge.common.MinecraftForge;

public class DebugLogger
{
	private static File logFile = new File(Minecraft.getMinecraftDir(),"ProjectAresMod.log");
	public static void log(String str)
	{
		try 
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(logFile,true));
			out.write(str);
			out.close();
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public static void logException(Exception e)
	{
		try 
		{
			PrintWriter out = new PrintWriter(new FileWriter(logFile,true));
			e.printStackTrace(out);
			out.close();
		} catch (IOException e1) 
		{
			e1.printStackTrace();
		}
	}
}
