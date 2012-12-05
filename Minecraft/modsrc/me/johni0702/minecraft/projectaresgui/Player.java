package me.johni0702.minecraft.projectaresgui;

import me.johni0702.minecraft.projectaresgui.map.Team;

public class Player 
{
	public int kills,deaths;
	private Team team;
	private String name;
	
	public Player(String name, Team team)
	{
		name = Player.removeSpecials(name);
		if (name.charAt(0) == '§')
			name = name.substring(2);
		this.name = name;
		if (team != null)
			joinTeam(team);
	}
	
	public void joinTeam(Team team)
	{
		Team oldTeam = this.team;
		this.team = team;
		if (oldTeam != null)
			oldTeam.removePlayer(this);
		if (team != null)
			team.addPlayer(this);
	}

	public Team getTeam()
	{
		return team;
	}
	
	public String getName()
	{
		return name;
	}
	
	public static String removeSpecials(String name)
	{
		while (name != null && name.charAt(0) == '§' && name.charAt(2) == '*')
			name = name.substring(3);
		return name;
	}

	public void addKill() 
	{
		kills++;
		if (team != null)
			team.addKill();
	}

	public void addDeaths() 
	{
		deaths++;
		if (team != null)
			team.addDeaths();
	}
	
	public void resetKD()
	{
		kills = deaths = 0;
	}
}
