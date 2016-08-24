package org.tilegames.hexicube.bukkit.isle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import net.minecraft.server.v1_9_R2.Block;
import net.minecraft.server.v1_9_R2.ChunkSection;
import net.minecraft.server.v1_9_R2.PacketPlayOutMapChunk;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_9_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;

public class IslePopulator extends BlockPopulator
{
	private ArrayList<net.minecraft.server.v1_9_R2.Chunk> chunksToReload;
	private UsedSections lastUsedSections;
	
	private int[][][] genIslandDataPair(int size, Random rand)
	{
		int tileSize = 6+rand.nextInt(5);
		int[][][] tileData = new int[2][size][size];
		int subsize = size*tileSize;
		int radiusSteps = Math.min(subsize, subsize)/15;
		int[][][] data = new int[2][subsize][subsize];
		
		ArrayList<int[]> steps = new ArrayList<int[]>();
		steps.add(new int[]{(int)(subsize*0.5), (int)(subsize*0.5), radiusSteps*5});
		
		while(steps.size() > 0)
		{
			int[] step = steps.remove(0);
			if(step[2] > radiusSteps/1.3)
			{
				double mult = 0.85+rand.nextDouble()*0.25;
				mult *= 1-((double)step[2]/(double)(radiusSteps*5))/4;
				double mult2 = 1.2+rand.nextDouble()*0.8;
				if(rand.nextInt(7) == 1) mult *= step[2]/radiusSteps;
				int stepSqrd = step[2]*step[2];
				for(int x = 0; x < step[2]; x++)
				{
					for(int y = 0; y < step[2]; y++)
					{
						double distSqrd = (step[2]*0.5-x)*(step[2]*0.5-x)+(step[2]*0.5-y)*(step[2]*0.5-y);
						if(distSqrd < stepSqrd*0.25)
						{
							double strength = (1.0-distSqrd/stepSqrd*4)*((step[2]==radiusSteps*5)?0.1:0.065)*mult;
							int xPos = (int)(x+step[0]-step[2]*0.5);
							int yPos = (int)(y+step[1]-step[2]*0.5);
							int val = data[0][xPos][yPos];
							val += strength*255;
							if(val > 2500) val = 2500;
							data[0][xPos][yPos] = val;
							strength = (1.0-distSqrd/stepSqrd*4)*((step[2]==radiusSteps*5)?0.1:0.065)*mult2;
							val = data[1][xPos][yPos];
							val += strength*255;
							if(val > 3500) val = 3500;
							data[1][xPos][yPos] = val;
						}
					}
				}
				
				int factor = 4+rand.nextInt(4);
				for(int a = 0; a < factor; a++)
				{
					double angle = (double)rand.nextInt(360)/180*Math.PI;
					steps.add(new int[]{(int)((double)step[0]+Math.cos(angle)*(double)step[2]*0.5), (int)((double)step[1]+Math.sin(angle)*(double)step[2]*0.5), step[2]-(int)(radiusSteps*(1+rand.nextDouble()*0.2))});
				}
			}
		}
		for(int x = 0; x < size; x++)
		{
			for(int y = 0; y < size; y++)
			{
				float strength = 0;
				for(int x2 = 0; x2 < tileSize; x2++)
				{
					for(int y2 = 0; y2 < tileSize; y2++)
					{
						int val = data[0][x*tileSize+x2][y*tileSize+y2];
						strength += (float)val/(float)(tileSize*tileSize);
					}
				}
				
				int value = (int)strength;
				tileData[0][x][y] = value;
				strength = 0;
				for(int x2 = 0; x2 < tileSize; x2++)
				{
					for(int y2 = 0; y2 < tileSize; y2++)
					{
						int val = data[1][x*tileSize+x2][y*tileSize+y2];
						strength += (float)val/(float)(tileSize*tileSize);
					}
				}
				value = (int)strength;
				tileData[1][x][y] = value;
			}
		}
		
		return tileData;
	}
	
	private ChunkSection getChunkSection(World world, int x, int y, int z)
	{
		Chunk c = world.getChunkAt(x>>4, z>>4);
		UsedSections usedSections = null;
		if(lastUsedSections != null &&
		   lastUsedSections.chunkX == x>>4 &&
		   lastUsedSections.chunkZ == z>>4) usedSections = lastUsedSections;
		if(usedSections == null)
		{
			net.minecraft.server.v1_9_R2.Chunk chunk = ((CraftChunk)c).getHandle();
			chunksToReload.add(chunk);
			Field f = null;
			try
			{
				f = chunk.getClass().getDeclaredField("sections");
			}
			catch(NoSuchFieldException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			catch(SecurityException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			
			f.setAccessible(true);
			ChunkSection[] sections = null;
			try
			{
				sections = (ChunkSection[]) f.get(chunk);
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			
			usedSections = new UsedSections();
			usedSections.chunkX = x>>4;
			usedSections.chunkZ = z>>4;
			usedSections.sections = sections;
		}
		
		lastUsedSections = usedSections;
		ChunkSection[] section = usedSections.sections;
		
		try
		{
			ChunkSection chunksection = section[y >> 4];
			if(chunksection == null)
			{
				chunksection = section[y >> 4] = new ChunkSection(y >> 4 << 4, true); //TODO: feed proper flag
			}
			return chunksection;
		}
		catch(IndexOutOfBoundsException e)
		{
			/* Added by NL_Tim for debugging purposes */
			e.printStackTrace();
			/*end*/
			
			return new ChunkSection(y >> 4 << 4, true); //TODO: feed proper flag
		}
	}
	
	private void setBlock(World world, int x, int y, int z, int id)
	{
		setBlockWithData(world, x, y, z, id, 0);
	}
	
	public void setBlockWithData(World world, int x, int y, int z, int id, int data)
	{
		ChunkSection chunksection = getChunkSection(world, x, y, z);
		chunksection.setType(x & 15, y & 15, z & 15, Block.getById(id).fromLegacyData(data));
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void populate(World world, Random rand, Chunk chunk)
	{
		if((chunk.getX()%(IslandWorldGeneration.islandSpacing*2) != 0 || chunk.getZ()%(IslandWorldGeneration.islandSpacing*2) != 0) &&
		   (Math.abs(chunk.getX())%(IslandWorldGeneration.islandSpacing*2) != IslandWorldGeneration.islandSpacing || Math.abs(chunk.getZ())%(IslandWorldGeneration.islandSpacing*2) != IslandWorldGeneration.islandSpacing)) return;
		
		chunksToReload = new ArrayList<net.minecraft.server.v1_9_R2.Chunk>();

		boolean flatIsland = true;
		boolean islandValidSpawn = true;
		Biome islandType = Biome.PLAINS;
		
		//70-180
		int size = IslandWorldGeneration.minIslandSize+rand.nextInt(IslandWorldGeneration.maxIslandSize-IslandWorldGeneration.minIslandSize+1);
		float heightMult = rand.nextFloat()*0.5f+0.75f*(float)IslandWorldGeneration.islandHeightScalar;
		
		int[][][] tileData = genIslandDataPair(size, rand);
		int startX = chunk.getX()*16+8 - size/2;
		int startY = IslandWorldGeneration.minIslandHeight+rand.nextInt(IslandWorldGeneration.maxIslandHeight-IslandWorldGeneration.minIslandHeight+1);
		int startZ = chunk.getZ()*16+8 - size/2;
		
		for(int x = 0; x < size; x++)
		{
			for(int z = 0; z < size; z++)
			{
				if(tileData[0][x][z] > 10 || tileData[1][x][z] > 25)
				{
					world.setBiome(startX+x, startZ+z, islandType);
					
					int upAmount = (int)((tileData[0][x][z]-(tileData[0][x-1][z]+tileData[0][x+1][z]+tileData[0][x][z-1]+tileData[0][x][z+1])/16)*size*heightMult/(flatIsland?12000:2000));
					int total = 0;
					for(int x2 = -4; x2 <= 4; x2++)
					{
						for(int z2 = -4; z2 <= 4; z2++)
						{
							try
							{
								total += tileData[1][x+x2][z+z2];
							}
							catch(IndexOutOfBoundsException e){
								/* Added by NL_Tim for debugging purposes */
								e.printStackTrace();
							}
						}
					}
					
					total /= 49;
					int downAmount = (int) (total*size*heightMult*IslandWorldGeneration.islandUnderbellyScalar/3000+1);
					int blockX = startX+x, blockZ = startZ+z;
					for(int y = -downAmount; y <= upAmount; y++)
					{
						int blockY = startY+y;
						int distFromTop = upAmount-y;
						
						if(islandType == Biome.PLAINS)
						{
							if(distFromTop < 4)
							{
								setBlock(world, blockX, blockY, blockZ, Material.DIRT.getId());
							}
							else 
							{
								setBlock(world, blockX, blockY, blockZ, Material.STONE.getId());
							}
						}
						else
						{
							System.out.println("Unknown island type: "+islandType.toString());
							@SuppressWarnings("unused")
							int a = 0/0; // Ugh...
						}
					}
				}
			}
		}
		
		// Send chunks to client when needed to reload them
		while(chunksToReload.size() > 0)
		{
			net.minecraft.server.v1_9_R2.Chunk c = chunksToReload.remove(0);
			c.initLighting();
			Iterator<Player> players = world.getPlayers().iterator();
			while(players.hasNext())
			{
				/* Experimental code by Microsenix and NL_Tim */
				((CraftPlayer) players.next()).getHandle().playerConnection.sendPacket(new PacketPlayOutMapChunk(c, 20));
				Bukkit.broadcastMessage("ChunkUpdate Called L->2119: " + c.toString());
				Bukkit.broadcastMessage("X: " + c.locX * 16 + " | Z: " + c.locZ * 16);
				/* End expirmental code */
			}
		}
		
		// Set spawnpoint
		// Not working great, make sure you have OP and/or can fly to prevent falling into the void
		if(!IslandWorldGeneration.spawnVerified)
		{
			if(islandValidSpawn)
			{				
				Location loc = world.getSpawnLocation();
				Chunk chu = world.getChunkAt(loc);
				if(!world.isChunkLoaded(chu)) 
				{ 
					world.loadChunk(chu);
				}
				
				int spawnY = world.getHighestBlockYAt(loc);
				if(Math.abs(spawnY-loc.getBlockY()) > 3 || loc.getBlockY() < IslandWorldGeneration.waterLevel)
				{
					world.setSpawnLocation(startX+size/2, world.getHighestBlockYAt(startX+size/2, startZ+size/2)+1, startZ+size/2);
				}
				
				IslandWorldGeneration.spawnVerified = true;
			}
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o == null) return false;
		return o instanceof IslePopulator;
	}
}