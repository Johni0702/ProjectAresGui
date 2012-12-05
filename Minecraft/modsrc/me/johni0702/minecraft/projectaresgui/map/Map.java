package me.johni0702.minecraft.projectaresgui.map;

import java.util.ArrayList;

import me.johni0702.minecraft.projectaresgui.Player;

public abstract class Map 
{
	protected ArrayList<Team> teams;

	public Map()
	{
		this.teams = new ArrayList<Team>();
	}
	
	public Map(Team... teams)
	{
		this.teams = new ArrayList<Team>();
		for (Team t : teams)
			this.teams.add(t);
	}
	
	/**
	 * 
	 * @return the number of teams in this map (dynamic)
	 */
	public int getTeamCount()
	{
		return teams.size();
	}

	
	/**
	 * 
	 * @param color - Color of the team (two chars) e.g. <i>§1</i>
	 * @return The team which has that color or <i>null</i> if none exists
	 */
	public Team getTeamByColor(String color)
	{
		for (Team t : teams)
			if (t.getColor().equalsIgnoreCase(color))
				return t;
		return null;
	}
	
	/**
	 * 
	 * @param name - Name of the team (without color)
	 * @return The team which has that name or <i>null</i> if none exists
	 */
	public Team getTeamByName(String name)
	{
		for (Team t : teams)
			if (t.getName().equalsIgnoreCase(name))
				return t;
		return null;
	}
	
	public void onKill(String killer, String killed)
	{
		if (killer != null)
		{
			Player pKiller = getPlayerByName(killer);
			//Check if the player exists if not create him
			if (pKiller == null)
			{
				pKiller = new Player(Player.removeSpecials(killer), getTeamByColor(killer.substring(0, 2)));
			}
			pKiller.addKill();
		}
		Player pKilled = getPlayerByName(killed);
		if (pKilled == null)
		{
			pKilled = new Player(Player.removeSpecials(killer), getTeamByColor(killer.substring(0, 2)));
		}
		pKilled.addDeaths();
	}
	
	/**
	 * 
	 * @return The time in ms to display in the HUD
	 */
	public abstract long getTimeToDisplay();
	
	/**
	 * Returns the team score line in the HUD
	 * @return The line containing the teams score
	 */
	public abstract String getTeamScoreString();
	
	/**
	 * Called when the map is created. You normaly create the Observer team here.
	 */
	public abstract void init();
	
	/**
	 * Returns whether the map is initialized or not. (Time set,teams created,so on)
	 * @return <b>true</b> if the map is initialized; <b>false</b> otherwise
	 */
	public abstract boolean isInitialized();
	
	/**
	 * Proceeds a line of the match info (/match)
	 * @param msg - The line from the chat
	 * @return <b>true</b> if the line was match info; <b>false</b> if it wasn't and should checked for other patterns
	 */
	public abstract boolean chatMatchInfo(String msg);

	/**
	 * Proceeds the score line (every 30s and on <i>/match</i>)
	 * @param msg - The line from the chat
	 */
	public abstract void chatScore(String msg);
	
	public Team getObservers()
	{
		return getTeamByName("Observers");
	}
	
	public Player getPlayerByName(String name)
	{
		name = Player.removeSpecials(name);
		if (name.startsWith("§"))
			return getPlayerByColoredName(name);
		for (Team t : teams)
		{
			Player player = t.getPlayer(name);
			if (player == null)
				continue;
			return player;
		}
		return null;
	}
	
	public Player getPlayerByColoredName(String name)
	{
		name = Player.removeSpecials(name);
		Team team = getTeamByColor(name.substring(0,2));
		return team==null?null:team.getPlayer(name.substring(2));
	}
}
