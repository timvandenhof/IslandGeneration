package org.tilegames.hexicube.bukkit.isle;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;

public final class IslandWorldGeneration extends JavaPlugin implements Listener
{
	public static int islandSpacing = 8,
					  minIslandHeight = 150, 
					  maxIslandHeight = 210,
					  minIslandSize = 70, 
					  maxIslandSize = 180;
	
	public static double islandHeightScalar = 1, 
			  			 islandUnderbellyScalar = 1;
	
	public static int waterLevel = 0, 
			          waterBlock = 0; //TODO: improve
	
	
	public static boolean enabled;
	
	public static boolean spawnVerified = false;
	
	@Override
	public void onEnable()
	{
		enabled = true;
	}
	
	@Override
	public void onDisable()
	{
		enabled = false;
	}
	
	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		if(!enabled) 
		{ 
			getServer().getPluginManager().enablePlugin(this);
		}
		
		return new ChunkGen();
	}
}