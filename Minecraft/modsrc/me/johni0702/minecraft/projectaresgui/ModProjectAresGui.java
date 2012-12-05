package me.johni0702.minecraft.projectaresgui;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.TickRegistry;

import me.johni0702.minecraft.projectaresgui.map.Map;
import me.johni0702.minecraft.projectaresgui.map.MapCapturePoints;
import me.johni0702.minecraft.projectaresgui.map.MapTDMMostKills;
import me.johni0702.minecraft.projectaresgui.map.MapTimedTDMLessDeaths;
import me.johni0702.minecraft.projectaresgui.map.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.src.ChatLine;
import net.minecraft.src.EntityClientPlayerMP;
import net.minecraft.src.FontRenderer;
import net.minecraft.src.GuiChat;
import net.minecraft.src.GuiIngame;
import net.minecraft.src.GuiNewChat;
import net.minecraft.src.ModLoader;
import net.minecraft.src.Packet3Chat;
import net.minecraft.src.ScaledResolution;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.Event.Result;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.IEventListener;
import net.minecraftforge.event.ListenerList;

@Mod(modid = "ProjectAresGui", name = "Project Ares Gui", version = "0.0.1dev")
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
public class ModProjectAresGui implements ITickHandler
{
	@Init
	public void load(FMLInitializationEvent event)
	{
		mc = ModLoader.getMinecraftInstance();
		kills = new ArrayList<String[]>();
		myPlayer = new Player(Minecraft.getMinecraft().session.username, null);
		
		maps = new HashMap<Pattern, Class<? extends Map>>();
		maps.put(Pattern.compile(".*\\W[Mm]ost\\W.*\\W[Kk]ills\\W.*"), MapTDMMostKills.class);
		maps.put(Pattern.compile(".*\\W[Ll]east\\W.*\\W[Dd]eaths\\W.*"), MapTimedTDMLessDeaths.class);
		maps.put(Pattern.compile(".*(\\W[Ww]ool\\W|\\W[Mm]onument\\W|\\W[Cc]ore\\W).*"), MapCapturePoints.class);
		
		TickRegistry.registerTickHandler(this, Side.CLIENT);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@Instance("ProjectAresGui")
	private static ModProjectAresGui inst;
	public static ModProjectAresGui getInstance()
	{
		if (inst == null)
			inst = new ModProjectAresGui();
		return inst;
	}
	
	private Minecraft mc;
	
	/**
	 * Time until switch to a new map or until the map starts.
	 * The time while the match is running is handled by the map itself.
	 * <i>0</i> While a map handles the time.
	 */
	private long timeEnd;
	/**
	 * Array of kills. String list contains: {killer,killed,weapon}
	 */
	private ArrayList<String[]> kills;
	/**
	 * Address of the last server we were on. Used to check if we switched server.
	 */
	private String lastServer = "";
	/**
	 * The current Map
	 */
	private Map map;
	/**
	 * The next map which will be played. This is only not <i>null</i> when the last map ended and the new one didn't load.
	 */
	private Map nextMap;
	/**
	 * HashMap of pattern of objectives of maps and the associated class.
	 */
	private HashMap<Pattern,Class<? extends Map>> maps;
	/**
	 * The <i>Player</i> instance which is the player
	 */
	private Player myPlayer;
	
	/**
	 * Returns a new map instance by objective. If none were found it returns the "Default" map.
	 * @param objective - Objective of the map
	 * @return A map with the requested objective or a default map.
	 */
	private Map getNewMapInstFor(String objective)
	{
		Class<? extends Map> cls = MapTDMMostKills.class;
		for (Entry<Pattern, Class<? extends Map>> e : maps.entrySet())
		{
			if (e.getKey().matcher(objective).matches())
			{
				cls = e.getValue();
				break;
			}
		}
		try {
			return cls.newInstance();
		} catch (Exception e1) 
		{
			e1.printStackTrace();
			DebugLogger.logException(e1);
		} 
		MinecraftForge.EVENT_BUS.unregister(this);
		DebugLogger.log("Default map wasn't found!");
		return null;
	}

	/**
	 * <b>true</b> if the mod requested the match informations by sending a "/match"
	 */
	private boolean requestedMatchInfo;
	/**
	 * When this is <b>true</b> the server sent the head line of the match informations. The mod will then check every following line for match info until it does contain other info.
	 */
	private boolean capturingMatchInfo;
	/**
	 * <b>true</b> if the mod requested the map informations by sending a "/map"
	 */
	private boolean requestedMapInfo;
	/**
	 * When this is <b>true</b> the server sent the head line of the map informations. The mod will then check every following line for map info until it does contain other info.
	 */
	private boolean capturingMapInfo;
	
	/**
	 * Stores the name of the map while capturing map info
	 */
	private String mapName;
	
	/**
	 * ---WIP---
	 * The current splash screen. (e.g. "You won!","You joined RED team")
	 */
	private SplashScreen currentSplashScreen;
	private Pattern pattern_kill_pvp = Pattern.compile("§[0-9a-fA-F].+§7 was slain by §[0-9a-fA-F].+'s .+");
	private Pattern pattern_kill_shot = Pattern.compile("§[0-9a-fA-F].+§7 was shot by §[0-9a-fA-F].+");
	private Pattern pattern_kill_pearl = Pattern.compile("§[0-9a-fA-F].+§7 took §[0-9a-fA-F].+'s ender pearl to the face");
	private Pattern pattern_snowball = Pattern.compile("§[0-9a-fA-F].+§7 was pelted by §[0-9a-fA-F].+'s snowball");
	private Pattern pattern_kill_tnt = Pattern.compile("§[0-9a-fA-F].+§7 was blown up by §[0-9a-fA-F].+'s TNT");
	private Pattern pattern_kill_fall = Pattern.compile("§[0-9a-fA-F].+§7 hit the ground too hard \\(\\d+ blocks\\)");
	private Pattern pattern_kill_suicide = Pattern.compile("§[0-9a-fA-F].+§7 (suffocated|'sploded|blew up|fell out of the world|starved to death|forgot to breathe|burned to death|went up in flames)");
	
	private Pattern pattern_score = Pattern.compile("§3Score: §[0-9a-fA-F]\\d+ §[0-9a-fA-F]\\d+  §c\\d{1,2}:\\d{2}");
	private Pattern pattern_match = Pattern.compile("§c(§m-)+ §3 Match Info §7\\(\\d+\\) §c(§m-)+");
	private Pattern pattern_map = Pattern.compile("§c(§m-)+ §3 .+ §c(§m-)+");
	private Pattern pattern_objective = Pattern.compile("§5§lObjective: §6.+");
	private Pattern pattern_join_team = Pattern.compile("§7You joined the §[0-9a-fA-F].+ Team");
	private Pattern pattern_now_playing = Pattern.compile("§5Now playing §6.+§5 by §c.+§5");
	private Pattern pattern_win = Pattern.compile("§5# # §[0-9a-fA-F].+ Team wins!§5 # #");
	private Pattern pattern_next_match = Pattern.compile("§3Cycling to §b.+§3 in §4\\d+§3 seconds!");
	private Pattern pattern_match_starting = Pattern.compile("§aMatch starting in §4\\d+§a seconds!");
	private Pattern pattern_chat_team = Pattern.compile("§[0-9a-fA-F]\\[Team\\] §[0-9a-fA-F].+§f: .+");
	private Pattern pattern_chat_global = Pattern.compile("\\<§[0-9a-fA-F].+§f\\>: .+");
	
	private ConcurrentLinkedQueue<String> chatQueue = new ConcurrentLinkedQueue<String>();
	
	/**
	 * This tries to identify a chat message using a lot of patterns
	 * @param msg - The chat message sent by the server
	 * @return <b>true</b> If the message should show up in the player's chat; <b>false</b> otherwise
	 */
	public boolean proceedChatMessage(String msg)
	{
//		System.out.println(map);
//		return true;
		try
		{
			checkServer();
			if (pattern_map.matcher(msg).matches() && !pattern_match.matcher(msg).matches())
			{
				mapName = msg.substring(msg.indexOf("§3"), msg.indexOf("§7"));
				capturingMapInfo = true;
				return !requestedMapInfo;
			}
			if (capturingMapInfo)
			{
				if (pattern_objective.matcher(msg).matches())
				{
					String objective = msg.substring(msg.indexOf("§6"));
					map = getNewMapInstFor(objective);
					if (map != null)
					{
						map.init();
						return !requestedMapInfo;
					}
				}
				if (msg.contains("§lMax players"))
				{
					boolean showMsg = !requestedMapInfo;
					requestedMapInfo = false;
					capturingMapInfo = false;
					return showMsg;
				}
				return !requestedMapInfo;
			}
			if (map == null)
			{
				chatQueue.offer(msg);
				if (requestedMapInfo)
				{
					return false;
				}
				else
				{
					requestedMapInfo = true;
					if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null)
						sendChatMessageToServer("/map");
					return false;
				}
			}
			else if (map.isInitialized())
			{
				while (!chatQueue.isEmpty())
				{
					String message = chatQueue.poll();
					if (proceedChatMessage(message))
						ModLoader.getMinecraftInstance().ingameGUI.getChatGUI().printChatMessage(message);
				}
			}
			if (capturingMatchInfo)
			{
				if (map.chatMatchInfo(msg))
					return !requestedMatchInfo;
				else
					capturingMatchInfo = false;
			}
			else if (map != null && !map.isInitialized() && !requestedMatchInfo)
			{
				requestedMatchInfo = true;
				if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null)
					sendChatMessageToServer("/match");
			}
			if (pattern_match.matcher(msg).matches())
			{
				capturingMatchInfo = true;
				return !requestedMatchInfo;
			}
			if (!map.isInitialized())
			{
					chatQueue.offer(msg);
					return false;
			}
			System.out.println("Blup");
			if (msg.equals("Your game mode has been updated"))
				return false;
			if (pattern_chat_global.matcher(msg).matches() ||
					pattern_chat_team.matcher(msg).matches() ||
					msg.startsWith("§7§o[§b§l§oTip§7§o]§7"))
				return true;
			if (pattern_kill_pvp.matcher(msg).matches())
			{
				int currentIndex = 0;
				String killed = msg.substring(0, currentIndex=msg.indexOf('§', 2));
				String killer = msg.substring(currentIndex=msg.indexOf('§', currentIndex+1), currentIndex=msg.indexOf("'s ", currentIndex)-2);
				String weapon = msg.substring(currentIndex+5);

				addKill(killer, killed, weapon);
				return false;
			}
			if (pattern_kill_pearl.matcher(msg).matches())
			{
				int currentIndex = 0;
				String killed = msg.substring(0, currentIndex=msg.indexOf('§', 2));
				String killer = msg.substring(currentIndex=msg.indexOf('§', currentIndex+1), currentIndex=msg.indexOf("'s ender pearl to the face", currentIndex)-2);


				addKill(killer, killed, "enderpearl");
				return false;
			}
			if (pattern_snowball.matcher(msg).matches())
			{
				int currentIndex = 0;
				String killed = msg.substring(0, currentIndex=msg.indexOf('§', 2));
				String killer = msg.substring(currentIndex=msg.indexOf('§', currentIndex+1), currentIndex=msg.indexOf("'s ender pearl to the face", currentIndex)-2);


				addKill(killer, killed, "snowball");
				return false;
			}
			if (pattern_kill_tnt.matcher(msg).matches())
			{
				int currentIndex = 0;
				String killed = msg.substring(0, currentIndex=msg.indexOf('§', 2));
				String killer = msg.substring(currentIndex=msg.indexOf('§', currentIndex+1), currentIndex=msg.indexOf("'s TNT", currentIndex)-2);


				addKill(killer, killed, "tnt");
				return false;
			}
			if (pattern_kill_shot.matcher(msg).matches())
			{
				int currentIndex = 0;
				String killed = msg.substring(0, currentIndex=msg.indexOf('§', 2));
				String killer = msg.substring(currentIndex=msg.indexOf('§', currentIndex+1));

				addKill(killer, killed, "shoot");
				return false;
			}
			if (pattern_kill_fall.matcher(msg).matches())
			{
				int currentIndex = 0;
				String killed = msg.substring(0, currentIndex=msg.indexOf('§', 2));
				int distance = Integer.parseInt(msg.substring(currentIndex+28, msg.indexOf(' ',currentIndex+29)));

				addKill(null, killed, "fall (" + distance + " blocks)");
				return false;
			}
			if (pattern_kill_suicide.matcher(msg).matches())
			{
				int currentIndex = 0;
				String killed = msg.substring(0, currentIndex=msg.indexOf('§', 2));
				String reason = msg.substring(msg.lastIndexOf("§7")+3);
				if (reason.equals("'sploded"))
					reason = "tnt";
				else if (reason.equals("blew up"))
					reason = "tnt";
				else if (reason.equals("suffocated"))
					reason = "suffocated";
				else if (reason.equals("fell out of the world"))
					reason = "void";
				else if (reason.equals("starved to death"))
					reason = "starved";
				else if (reason.equals("forgot to breathe"))
					reason = "drowned";
				else if (reason.equals("burned to death"))
					reason = "fire";
				else if (reason.equals("went up in flames"))
					reason = "fire";
//				(suffocated|'sploded|blew up|fell out of the world|starved to death|forgot to breathe|burned to death|went up in flames)
				
				addKill(null, killed, reason);
				return false;
			}
			if (pattern_score.matcher(msg).matches())
			{
				if (map != null)
					map.chatScore(msg);
				return false;
			}
			if (pattern_join_team.matcher(msg).matches())
			{
				String teamColor = msg.substring(17, 19);
				myPlayer.joinTeam(map.getTeamByColor(teamColor));
				return false;
			}
			if (msg.equals("§7You joined the §bObservers"))
			{
				myPlayer.joinTeam(map.getObservers());
				return false;
			}
			if (msg.startsWith("§5# # # # # # # # # # # #") || msg.startsWith("§5# # # # # # # # # # # # # # # #"))
				return false;
			if (msg.equals("§5# # §6   Game Over!   §5 # #"))
			{
				return false;
			}
			if (pattern_win.matcher(msg).matches()) //TODO
			{
//				if (lastMatchTeam != 0)
//					if (msg.substring(6, 8).equals(teams[lastMatchTeam]))
//						currentSplashScreen = SplashScreen.YOU_WON;
//					else
//						currentSplashScreen = SplashScreen.YOU_LOST;
//				else
//				{
////					EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
					return true;
//				}
//				return false;
			}
			if (pattern_now_playing.matcher(msg).matches())
			{
				return false;
			}
			if (pattern_next_match.matcher(msg).matches())
			{
				timeEnd = Integer.parseInt(msg.substring(msg.lastIndexOf("§4")+2, msg.lastIndexOf("§3")))*1000+System.currentTimeMillis();
				if (nextMap == null)
				{
					String mapName = msg.substring(15,msg.indexOf('§', 16));
					sendChatMessageToServer("/map " + mapName.replaceAll(" +", ""));
					requestedMapInfo = true;
				}
			}
			if (pattern_match_starting.matcher(msg).matches())
			{
				timeEnd = Integer.parseInt(msg.substring(msg.lastIndexOf("§4")+2, msg.lastIndexOf("§a")))*1000+System.currentTimeMillis();
			}
			if (msg.equals("§5# # §6The match has started!§5 # #"))
			{
				timeEnd = 0;
				map = nextMap;
				nextMap = null;
				resetAll();
				if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null)
				{
					requestedMatchInfo = true;
					sendChatMessageToServer("/match");
				}
				return false;
			}
			DebugLogger.log("Couldn't proceed message: " + msg);
			return true;
		} catch (Exception e)
		{
			e.printStackTrace();
			DebugLogger.logException(e);
			return true;
		}
	}
	
	/**
	 * Adds a kill to the HUD and to the teams
	 * @param killer
	 * @param killed
	 * @param weapon
	 */
	private void addKill(String killer, String killed, String weapon)
	{
		System.out.println(killer + " " );
		kills.add(new String[]{killer,killed,weapon});
		if (map != null && map.isInitialized())
			map.onKill(killer, killed);
	}
	
	/**
	 * Resets the mod (e.g. when changing server)
	 */
	public void resetAll()
	{
		kills.clear();
		map = null;
		myPlayer.joinTeam(null);
		requestedMatchInfo = false;
		capturingMatchInfo = false;
		requestedMapInfo = false;
		capturingMapInfo = false;
		currentSplashScreen = null;
		chatQueue.clear();
		sendMessages = 0;
	}
	
	/**
	 * Check if we are still on the same server or if the IP changed
	 */
	public void checkServer()
	{
		Minecraft mc = Minecraft.getMinecraft();
		if (lastServer != null && mc.getServerData() != null && !lastServer.equals(mc.getServerData().serverIP))
		{
			resetAll();
			lastServer = mc.getServerData().serverIP;
		}
	}
	
	/**
	 * This renders the In-Game HUD on top of the normal In-Game HUD
	 * @param guiIngame
	 */
	public void renderIngameHUD(GuiIngame guiIngame)
	{
		checkServer();
		Minecraft mc = Minecraft.getMinecraft();
		ScaledResolution scaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
        FontRenderer font = mc.fontRenderer;
        
        
        String timeString;
        if (map == null || !map.isInitialized())
        	timeString = "??:??";
        else
        {
        	long sec;
        	if (timeEnd != 0)
        		sec = timeEnd-System.currentTimeMillis();
        	else
        		sec = map.getTimeToDisplay();
        	timeString = String.format("%02d", sec/60) + ":" + String.format("%02d", Math.abs(sec%60));
        }
        guiIngame.drawString(font, timeString, width-font.getStringWidth(timeString)-2, 2, 0xffff5050);

        
        if (map != null && map.isInitialized())
        {
        	String scoreString = map.getTeamScoreString();
        	guiIngame.drawString(font, scoreString, width-font.getStringWidth(scoreString)-font.getStringWidth(timeString)-4, 2, 0xffffffff);
        }
        
//        if (myPlayer.getTeam() != null && !myPlayer.getTeam().getName().equals("Observers"))
        {
        	guiIngame.drawString(font, "§4" + myPlayer.deaths, width-50, 12, 0xffff5050);
	        guiIngame.drawString(font, "§2" + myPlayer.kills, width-75, 12, 0xffff5050);
        }
        
        
    	for (int i = kills.size()-5<0?0:kills.size()-5; i < kills.size(); i++)
    	{
    		String text;
    		if (kills.get(i)[0] == null)
    			text = kills.get(i)[1] + " §f-> " + kills.get(i)[2];
    		else
    			text = kills.get(i)[0] + " §f-> " + kills.get(i)[2] + " §f-> " + kills.get(i)[1];
    		text.replaceAll("§[0-9a-fA-F]" + mc.session.username, "§2§l"+mc.session.username);
			guiIngame.drawString(font, text, 2, 52-10*(kills.size()-i), 0xffffffff);
    	}
	}

	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) 
	{
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) 
	{
//		System.out.println(tickData);
		for (TickType tickType : type)
		{
			if (tickType.equals(TickType.RENDER) && mc.currentScreen == null || mc.currentScreen instanceof GuiChat)
			{
				try {
					renderIngameHUD(ModLoader.getMinecraftInstance().ingameGUI);
				} catch (Exception e)
				{
					e.printStackTrace();
					DebugLogger.logException(e);
					MinecraftForge.EVENT_BUS.unregister(this);
				}
			}
		}
	}

	@Override
	public EnumSet<TickType> ticks() 
	{
		return EnumSet.of(TickType.RENDER);
	}

	@Override
	public String getLabel() {
		return null;
	}
	
	@ForgeSubscribe
	public void onClientChatReceived(ClientChatReceivedEvent event)
	{
		System.out.println("[Chat] " + event.message);
//		resetAll();
//		DebugLogger.log(event.message + "\n");
		if (!proceedChatMessage(event.message))
			event.setCanceled(true);
//			event.message = "(" + event.message + ")";
	}
	
	/**
	 * This helps the sendChatMessageToServer method to keep track of the sent messages
	 */
	private int sendMessages = 0;
	
	/**
	 * ---Debug Method---
	 * This method makes sure I don't send too many messages to the server by accident. I don't spam to spam the server.
	 * @param msg
	 */
	private void sendChatMessageToServer(String msg)
	{
		if (sendMessages < 10)
		{
			Minecraft.getMinecraft().thePlayer.sendChatMessage(msg);
			System.out.println("Send to server: " + msg);
			sendMessages++;
		}
		else
			System.out.println("Didn't send (spam): " + msg);
	}
}
