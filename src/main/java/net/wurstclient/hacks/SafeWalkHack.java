/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"safe walk", "SneakSafety", "sneak safety", "SpeedBridgeHelper",
	"speed bridge helper"})
public final class SafeWalkHack extends Hack
{
	private final CheckboxSetting sneak =
		new CheckboxSetting("Sneak at edges", "Visibly sneak at edges.", false);
	
	private final SliderSetting edgeDistance = new SliderSetting(
		"Sneak edge distance",
		"How close SafeWalk will let you get to the edge before sneaking.\n\n"
			+ "This setting is only used when \"Sneak at edges\" is enabled.",
		0.3, 0.05, 0.3, 0.001, ValueDisplay.DECIMAL.withSuffix("m"));

	private final SliderSetting permittedHeight = new SliderSetting(
			"Permitted height",
			"Permitted height",
			5, 0, 100, 1, ValueDisplay.DECIMAL);

	private final SliderSetting altPermittedHeight = new SliderSetting(
			"Alt Permitted height",
			"Permitted height for walking backward or sideways",
			1, 0, 100, 1, ValueDisplay.DECIMAL);
	
	private boolean sneaking;

	public SafeWalkHack()
	{
		super("SafeWalk");
		setCategory(Category.MOVEMENT);
		addSetting(sneak);
		addSetting(edgeDistance);
		addSetting(permittedHeight);
		addSetting(altPermittedHeight);
	}

	@Override
	protected void onEnable()
	{
		WURST.getHax().parkourHack.setEnabled(false);
		sneaking = false;
	}
	
	@Override
	protected void onDisable()
	{
		if(sneaking)
			setSneaking(false);
	}

	public void onClipAtLedge(boolean clipping)
	{
		if(!isEnabled() || !sneak.isChecked() || !MC.player.isOnGround())
		{
			if(sneaking)
				setSneaking(false);
			return;
		}

		if(clip())
			clipping = true;

		setSneaking(clipping);
	}

	public boolean clip()
	{
		if (!isEnabled())
			return false;

		ClientPlayerEntity player = MC.player;
		if (player == null)
			return false;

		float input = player.input == null ? 0f : player.input.movementForward;

		double adjustedPermittedHeight = (input == 1)
				? permittedHeight.getValue()
				: altPermittedHeight.getValue();

		Box adjustedBox = MC.player.getBoundingBox().stretch(0, -MC.player.stepHeight, 0)
				.expand(-edgeDistance.getValue(), 0, -edgeDistance.getValue())
				.stretch(0, -adjustedPermittedHeight, 0);

		return !intersectsAnyCollision(adjustedBox);
	}

	private boolean intersectsAnyCollision(Box box) {
		for (BlockPos pos : BlockPos.iterate(
				BlockPos.ofFloored(box.minX, box.minY, box.minZ),
				BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ))) {

			BlockState state = MC.world.getBlockState(pos);
			VoxelShape shape = state.getCollisionShape(MC.world, pos);

			if (!shape.isEmpty() && shape.getBoundingBox().offset(pos).intersects(box))
				return true;
		}

		return false;
	}

	private void setSneaking(boolean sneaking)
	{
		IKeyBinding sneakKey = IKeyBinding.get(MC.options.sneakKey);
		
		if(sneaking)
			sneakKey.setPressed(true);
		else
			sneakKey.resetPressedState();
		
		this.sneaking = sneaking;
	}

	// See ClientPlayerEntityMixin
}
