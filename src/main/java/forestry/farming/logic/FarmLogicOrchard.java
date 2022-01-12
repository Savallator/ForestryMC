/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.farming.logic;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import forestry.farming.FarmTarget;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import forestry.api.farming.FarmDirection;
import forestry.api.farming.Farmables;
import forestry.api.farming.ICrop;
import forestry.api.farming.IFarmHousing;
import forestry.api.farming.IFarmable;
import forestry.api.genetics.IFruitBearer;
import forestry.core.utils.vect.Vect;
import forestry.core.utils.vect.VectUtil;
import forestry.plugins.PluginCore;
import forestry.plugins.PluginManager;
import scala.Console;

public class FarmLogicOrchard extends FarmLogic {

	protected Collection<IFarmable> farmables;
	protected final HashMap<Vect, Integer> lastExtents = new HashMap<>();
	protected final ImmutableList<Block> traversalBlocks;

	public FarmLogicOrchard(IFarmHousing housing) {
		super(housing);
		this.farmables = Farmables.farmables.get(FarmableReference.Orchard.get());

		ImmutableList.Builder<Block> traversalBlocksBuilder = ImmutableList.builder();
		if (PluginManager.Module.AGRICRAFT.isEnabled() || PluginManager.Module.INDUSTRIALCRAFT.isEnabled()) {
			traversalBlocksBuilder.add(Blocks.farmland);
		}
		if (PluginManager.Module.INDUSTRIALCRAFT.isEnabled()) {
			traversalBlocksBuilder.add(Blocks.dirt);
		}
		if (PluginManager.Module.PLANTMEGAPACK.isEnabled()) {
			traversalBlocksBuilder.add(Blocks.water);
		}

		{
			Block grapeVine = GameRegistry.findBlock("Growthcraft|Grapes", "grc.grapeVine1");
			if (grapeVine != null) {
				traversalBlocksBuilder.add(grapeVine);
			}
		}

		traversalBlocksBuilder.build();
		this.traversalBlocks = traversalBlocksBuilder.build();
	}

	@Override
	public int getFertilizerConsumption() {
		return 10;
	}

	@Override
	public int getWaterConsumption(float hydrationModifier) {
		return (int) (40 * hydrationModifier);
	}

	@Override
	public boolean isAcceptedResource(ItemStack itemstack) {
		return false;
	}

	@Override
	public boolean isAcceptedGermling(ItemStack itemstack) {
		return false;
	}

	@Override
	public boolean isAcceptedWindfall(ItemStack stack) {
		return false;
	}

	@Override
	public Collection<ItemStack> collect() {
		return null;
	}

	@Override
	public boolean cultivate(FarmTarget target, int x, int y, int z, FarmDirection direction, int extent) {
		return false;
	}

	@Override
	public Collection<ICrop> harvest(FarmTarget target, int x, int y, int z, FarmDirection direction, int extent) {

		Vect start = new Vect(x, y, z);
		if (!lastExtents.containsKey(start)) {
			lastExtents.put(start, 0);
		}

		int lastExtent = lastExtents.get(start);
		if (lastExtent > extent) {
			lastExtent = 0;
		}
		Stack<ICrop> crops = new Stack<>();

		Vect position = translateWithOffset(x, y + 1, z, direction, lastExtent);
		target.incTick();
		if (target.getTick() % 200 == 0)
		{
			target.setCache(getHarvestBlocks(position));
			System.out.println("building cache for "+position.x+" "+position.y+" "+position.z);
		}
		for (Vect element :target.getCache()) {
			ICrop crop = getCrop(getWorld(), element);
			if (crop != null) {
				crops.push(crop);
			}
		}
		lastExtent++;
		lastExtents.put(start, lastExtent);

		return crops;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon() {
		return PluginCore.items.fruits.getIconFromDamage(0);
	}

	@Override
	public String getName() {
		return "Orchard";
	}

	private Collection<Vect> getHarvestBlocks(Vect position) {

		Set<Vect> seen = new HashSet<>();
		Stack<Vect> cropList = new Stack<>();
		World world = getWorld();

		// Determine what type we want to harvest.
		if (!VectUtil.isWoodBlock(world, position) && !isBlockTraversable(world, null, position, traversalBlocks) && !isFruitBearer(world, position)) {
			return cropList;
		}

		List<Vect> candidates = processHarvestBlock(cropList, seen, position, position);
		List<Vect> temp = new ArrayList<>();
		while (!candidates.isEmpty() && cropList.size() < 20) {
			for (Vect candidate : candidates) {
				temp.addAll(processHarvestBlock(cropList, seen, position, candidate));
			}
			candidates.clear();
			candidates.addAll(temp);
			temp.clear();
		}
		return cropList;
	}

	private List<Vect> processHarvestBlock(Stack<Vect> crops, Set<Vect> seen, Vect start, Vect position) {
		World world = getWorld();

		List<Vect> candidates = new ArrayList<>();

		// Get additional candidates to return
		for (int i = -2; i < 3; i++) {
			for (int j = 0; j < 2; j++) {
				for (int k = -1; k < 2; k++) {
					Vect candidate = position.add(i, j, k);
					if (Math.abs(candidate.x - start.x) > 5) {
						continue;
					}
					if (Math.abs(candidate.z - start.z) > 5) {
						continue;
					}
					// See whether the given position has already been processed
					if (seen.contains(candidate)) {
						continue;
					}
					Block target = VectUtil.getBlock(world, candidate);
					if (target.isAir(world, candidate.x, candidate.y, candidate.z)) {
						continue;
					}
					if (target.isWood(world, candidate.x, candidate.y, candidate.z) || isBlockTraversable(world, target, candidate, traversalBlocks)) {
						candidates.add(candidate);
						seen.add(candidate);
					} else if (isFruitBearer(world, candidate)) {
						candidates.add(candidate);
						seen.add(candidate);
						crops.push(candidate);
					}
				}
			}
		}

		return candidates;
	}

	private boolean isFruitBearer(World world, Vect position) {

		TileEntity tile = world.getTileEntity(position.x, position.y, position.z);
		if (tile instanceof IFruitBearer) {
			return true;
		}

		for (IFarmable farmable : farmables) {
			if (farmable.isSaplingAt(world, position.x, position.y, position.z)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isBlockTraversable(World world, Block candidate, Vect position, ImmutableList<Block> traversalBlocks) {

		for (Block block : traversalBlocks) {
			if (block == (candidate)) {
				return true;
			}
		}
		return false;
	}

	private ICrop getCrop(World world, Vect position) {

		TileEntity tile = world.getTileEntity(position.x, position.y, position.z);

		if (tile instanceof IFruitBearer) {
			IFruitBearer fruitBearer = (IFruitBearer) tile;
			if (fruitBearer.hasFruit() && fruitBearer.getRipeness() >= 0.9f) {
				return new CropFruit(world, position);
			}
		} else {
			for (IFarmable seed : farmables) {
				ICrop crop = seed.getCropAt(world, position.x, position.y, position.z);
				if (crop != null) {
					return crop;
				}
			}
		}
		return null;
	}

}
