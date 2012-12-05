package me.johni0702.minecraft.projectaresgui.map;

import java.util.ArrayList;

import me.johni0702.minecraft.projectaresgui.Player;

public class Team 
{
	private String color;
	private String name;
	private int maxPlayers,currentPlayers;
	private ArrayList<Player> members;
	private int kills,deaths;
	
	public Team(String color, String name, int maxPlayers)
	{
		this.color = color;
		this.name = name;
		this.maxPlayers = maxPlayers;
		this.members = new ArrayList<Player>();
		kills = deaths = 0;
	}
	
	public Team(String name, int maxPlayers)
	{
		this(name.substring(0,2), name.substring(2), maxPlayers);
	}
	
	public String getColor()
	{
		return color;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void addPlayer(Player player)
	{
		if (player == null || player.getTeam() != this) 
			throw new IllegalStateException("The player hasn't left the team yet!");
		members.add(player);
		currentPlayers = members.size();
	}
	
	public void removePlayer(Player player)
	{
		if (player == null || player.getTeam() == this) 
			throw new IllegalStateException("The player hasn't left the team yet!");
		members.remove(player);
		currentPlayers = members.size();
	}

	public void addKill() 
	{
		kills++;
	}

	public void addDeaths() 
	{
		deaths++;
	}
	
	public void setKills(int kills)
	{
		this.kills = kills;
	}
	
	public void setDeaths(int deaths)
	{
		this.deaths = deaths;
	}
	
	public int getKills()
	{
		return kills;
	}
	
	public int getDeaths()
	{
		return deaths;
	}

	public Player getPlayer(String name) 
	{
		name = Player.removeSpecials(name);
		if (name.charAt(0) == '§')
			name = name.substring(2);
		for (Player p : members)
			if (p.getName().equals(name))
				return p;
		return null;
	}

	public void setMaxPlayers(int i) 
	{
		maxPlayers = i;
	}

	public void setPlayerCount(int i) 
	{
		currentPlayers = i;
	}
}
