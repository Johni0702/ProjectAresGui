package me.johni0702.minecraft.projectaresgui.map;

import java.util.Arrays;

/**
 * A map running <b>T</b>eam<b>D</b>eath<b>M</b>atch mode.<br>
 * Currently these maps: 
 * @author Johni0702
 *
 */
public class MapTDMMostKills extends Map 
{
	private long startTime;

	@Override
	public long getTimeToDisplay() 
	{
		return (System.currentTimeMillis()-startTime)/1000;
	}

	@Override
	public boolean chatMatchInfo(String msg)
	{
		if (msg.startsWith("�5Time: �6"))
		{
			String time = msg.substring(msg.indexOf("�6")+2);
			int timeMin = Integer.parseInt(time.substring(0, time.indexOf(':')));
			int timeSec = Integer.parseInt(time.substring(time.indexOf(':')+1));
			this.startTime = System.currentTimeMillis()-(timeMin*60000+timeSec*1000);
			return true;
		}
		if (msg.startsWith("�3Score:"))
		{
			chatScore(msg);
			return true;
		}
		else if (msg.contains(" Team�7: �f"))
		{
			int maxPlayers = Integer.parseInt(msg.substring(msg.lastIndexOf("�7/")+3));
			Team t = getTeamByColor(msg.substring(0,2));
			if (t == null)
			{
				teams.add(new Team(msg.substring(0, msg.indexOf(" Team�7: �f")), maxPlayers));
			}
			else
			{
				t.setMaxPlayers(maxPlayers);
			}
			return true;	
		} 
		else if (msg.startsWith("�bObservers�7:"))
		{
			getObservers().setPlayerCount(Integer.parseInt(msg.substring(msg.lastIndexOf("�f")+2)));
			return true;
		}
		else
			return false;
	}

	@Override
	public void chatScore(String msg) 
	{
		int currentIndex = 11;
		msg = msg.substring(9);
		String[] parts = msg.split(" ");
		for (int i = 0; i < parts.length-1; i++)
		{
			getTeamByColor(parts[i].substring(0, 2)).setDeaths(Integer.parseInt(parts[i].substring(3)));
		}
		String time = parts[parts.length].substring(2);
		int timeMin = Integer.parseInt(time.substring(0, time.indexOf(':')));
		int timeSec = Integer.parseInt(time.substring(time.indexOf(':')+1));
		this.startTime = System.currentTimeMillis()-(timeMin*60000+timeSec*1000);
	}

	@Override
	public String getTeamScoreString() 
	{
		StringBuilder sb = new StringBuilder();
		for (Team t : teams)
		{
			if (t.getName().equals("Observers"))
				continue;
			sb.append(t.getColor());
			String points = Integer.toString(t.getDeaths());
			for (int i = points.length(); i < 6; i++)
				sb.append(' ');
			sb.append(points);
		}
		return sb.toString();
	}

	@Override
	public void init() 
	{
		teams.add(new Team("�bObservers",-1));
	}

	@Override
	public boolean isInitialized() 
	{
		return startTime != 0 && teams.size() > 0;
	}

}
