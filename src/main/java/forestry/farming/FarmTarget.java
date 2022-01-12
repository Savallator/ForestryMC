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
package forestry.farming;

import forestry.api.farming.ICrop;
import net.minecraft.block.Block;
import net.minecraft.world.World;

import forestry.api.farming.FarmDirection;
import forestry.core.utils.vect.MutableVect;
import forestry.core.utils.vect.Vect;
import forestry.core.utils.vect.VectUtil;

import java.util.Collection;
import java.util.Stack;

public class FarmTarget {

	private final Vect start;
	private final FarmDirection direction;
	private final int limit;
	private Collection<Vect> cachedCrops;
	private int updateTicks = 0;
	private int yOffset;
	private int extent;

	public FarmTarget(Vect start, FarmDirection direction, int limit) {
		this.start = start;
		this.direction = direction;
		this.limit = limit;
		this.cachedCrops = new Stack<>();
	}
	public void clearCache()
	{
		this.cachedCrops.clear();
	}
	public void addCache(Vect crop)
	{
		this.cachedCrops.add(crop);
	}
	public void setCache (Collection<Vect> cache)
	{
		this.cachedCrops = cache;
	}
	public Collection<Vect> getCache()
	{
		return this.cachedCrops;
	}
	public Vect getStart() {
		return start;
	}

	public void incTick ()
	{
		updateTicks++;
	}
	public int getTick()
	{
		return updateTicks;
	}

	public int getYOffset() {
		return this.yOffset;
	}

	public int getExtent() {
		return extent;
	}

	public FarmDirection getDirection() {
		return direction;
	}

	public void setExtentAndYOffset(World world, Vect platformPosition) {
		if (platformPosition == null) {
			extent = 0;
			return;
		}

		MutableVect position = new MutableVect(platformPosition);
		for (extent = 0; extent < limit; extent++) {
			Block platform = VectUtil.getBlock(world, position);
			if (!FarmHelper.bricks.contains(platform)) {
				break;
			}
			position.add(getDirection().getForgeDirection());
		}

		yOffset = platformPosition.getY() + 1 - getStart().getY();
	}
}
