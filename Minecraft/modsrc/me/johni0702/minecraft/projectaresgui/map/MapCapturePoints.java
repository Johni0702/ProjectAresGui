package me.johni0702.minecraft.projectaresgui.map;

/**
 * A map running in CaptureTheWool/DestroyTheCore/DestroyTheFlag mode. No time limit!<br>
 * Currently these maps: Block DTC, RFV3, Hot Dam, Soviet Mills, Avalon Funland
 * @author Johni0702
 *
 */
public class MapCapturePoints extends Map
{
	/**
	 * Time in ms when the match started
	 */
	private long startTime;
	private String teamScore;

	@Override
	public long getTimeToDisplay()
	{
		return (System.currentTimeMillis()-startTime)/1000;
	}

	@Override
	public String getTeamScoreString() 
	{
		return teamScore;
	}

	@Override
	public void init() 
	{
		teams.add(new Team("§bObservers",-1));
	}

	@Override
	public boolean isInitialized()
	{
		return startTime != 0 && teams.size() > 1 && teamScore != null;
	}

	@Override
	public boolean chatMatchInfo(String msg) 
	{
		if (msg.startsWith("§5Time: §6"))
		{
			String time = msg.substring(msg.indexOf("§6")+2);
			int timeMin = Integer.parseInt(time.substring(0, time.indexOf(':')));
			int timeSec = Integer.parseInt(time.substring(time.indexOf(':')+1));
			this.startTime = System.currentTimeMillis()-(timeMin*60000+timeSec*1000);
			return true;
		}
		else if (msg.contains(" Team§7: §f"))
		{
			int maxPlayers = Integer.parseInt(msg.substring(msg.lastIndexOf("§7/")+3));
			Team t = getTeamByColor(msg.substring(0,2));
			if (t == null)
			{
				teams.add(new Team(msg.substring(0, msg.indexOf(" Team§7: §f")), maxPlayers));
			}
			else
			{
				t.setMaxPlayers(maxPlayers);
			}
			return true;	
		} 
		else if (msg.startsWith("§bObservers§7:"))
		{
			getObservers().setPlayerCount(Integer.parseInt(msg.substring(msg.lastIndexOf("§f")+2)));
			return true;
		}
		else if (msg.equals("§c---- Goals ----"))
		{
			teamScore = null;
			return true;
		}
		else if (msg.startsWith(" Team§7:  §"))
		{
			String[] goals = msg.substring(msg.indexOf(':')+3).split("  §");
			StringBuilder sb = new StringBuilder(teamScore);
			for (String goal : goals)
			{
				if (goal.charAt(0) != '4')
					continue; //Already captured that point
				sb.append("  ");
				sb.append('§');
				sb.append(goal.substring(0,goal.indexOf(' ')));
			}
			teamScore = sb.toString();
			return true;
		}
		else
			return false;
	}

	/**
	 * This message won't be broadcasted in this gamemode
	 */
	@Override
	public void chatScore(String msg) {}

}
