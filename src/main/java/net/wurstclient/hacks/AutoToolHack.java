/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.BlockBreakingProgressListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"auto tool", "AutoSwitch", "auto switch"})
public final class AutoToolHack extends Hack
	implements BlockBreakingProgressListener, UpdateListener
{
	private final CheckboxSetting useSwords = new CheckboxSetting("Use swords",
		"Uses swords to break leaves, cobwebs, etc.", false);
	
	private final CheckboxSetting useHands = new CheckboxSetting("Use hands",
		"Uses an empty hand or a non-damageable item when no applicable tool is"
			+ " found.",
		true);
	
	private final SliderSetting repairMode = new SliderSetting("Repair mode",
		"Prevents tools from being used when their durability reaches the given"
			+ " threshold, so you can repair them before they break.\n"
			+ "Can be adjusted from 0 (off) to 100 remaining uses.",
		0, 0, 100, 1, ValueDisplay.INTEGER.withLabel(0, "off"));
	
	private final CheckboxSetting switchBack = new CheckboxSetting(
		"Switch back", "After using a tool, automatically switches back to the"
			+ " previously selected slot.",
		false);
	
	private int prevSelectedSlot;
	
	public AutoToolHack()
	{
		super("AutoTool");
		
		setCategory(Category.BLOCKS);
		addSetting(useSwords);
		addSetting(useHands);
		addSetting(repairMode);
		addSetting(switchBack);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(BlockBreakingProgressListener.class, this);
		EVENTS.add(UpdateListener.class, this);
		prevSelectedSlot = -1;
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(BlockBreakingProgressListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onBlockBreakingProgress(BlockBreakingProgressEvent event)
	{
		BlockPos pos = event.getBlockPos();
		if(!BlockUtils.canBeClicked(pos))
			return;
		
		if(prevSelectedSlot == -1)
			prevSelectedSlot = MC.player.getInventory().selectedSlot;
		
		equipBestTool(pos, useSwords.isChecked(), useHands.isChecked(),
			repairMode.getValueI());
	}
	
	@Override
	public void onUpdate()
	{
		if(prevSelectedSlot == -1 || MC.interactionManager.isBreakingBlock())
			return;
		
		HitResult hitResult = MC.crosshairTarget;
		if(hitResult != null && hitResult.getType() == HitResult.Type.BLOCK)
			return;
		
		if(switchBack.isChecked())
			MC.player.getInventory().selectedSlot = prevSelectedSlot;
		
		prevSelectedSlot = -1;
	}
	
	public void equipIfEnabled(BlockPos pos)
	{
		if(!isEnabled())
			return;
		
		equipBestTool(pos, useSwords.isChecked(), useHands.isChecked(),
			repairMode.getValueI());
	}
	
	public void equipBestTool(BlockPos pos, boolean useSwords, boolean useHands,
		int repairMode)
	{
		ClientPlayerEntity player = MC.player;
		if(player.getAbilities().creativeMode)
			return;
		
		ItemStack heldItem = player.getMainHandStack();
		boolean heldItemDamageable = isDamageable(heldItem);
		if(heldItemDamageable && isTooDamaged(heldItem, repairMode))
			putAwayDamagedTool(repairMode);
		
		BlockState state = BlockUtils.getState(pos);
		int bestSlot = getBestSlot(state, useSwords, repairMode);
		if(bestSlot == -1)
		{
			if(useHands && heldItemDamageable && isWrongTool(heldItem, state))
				selectFallbackSlot();
			
			return;
		}
		
		player.getInventory().selectedSlot = bestSlot;
	}
	
	private int getBestSlot(BlockState state, boolean useSwords, int repairMode)
	{
		ClientPlayerEntity player = MC.player;
		PlayerInventory inventory = player.getInventory();
		ItemStack heldItem = MC.player.getMainHandStack();
		
		float bestSpeed = getMiningSpeed(heldItem, state);
		if(isTooDamaged(heldItem, repairMode))
			bestSpeed = 1;
		int bestSlot = -1;
		
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == inventory.selectedSlot)
				continue;
			
			ItemStack stack = inventory.getStack(slot);
			
			float speed = getMiningSpeed(stack, state);
			if(speed <= bestSpeed)
				continue;
			
			if(!useSwords && stack.getItem() instanceof SwordItem)
				continue;
			
			if(isTooDamaged(stack, repairMode))
				continue;
			
			bestSpeed = speed;
			bestSlot = slot;
		}
		
		return bestSlot;
	}
	
	private float getMiningSpeed(ItemStack stack, BlockState state)
	{
		float speed = stack.getMiningSpeedMultiplier(state);
		
		if(speed > 1)
		{
			int efficiency =
				EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
			if(efficiency > 0 && !stack.isEmpty())
				speed += efficiency * efficiency + 1;
		}
		
		return speed;
	}
	
	private boolean isDamageable(ItemStack stack)
	{
		return !stack.isEmpty() && stack.getItem().isDamageable();
	}
	
	private boolean isTooDamaged(ItemStack stack, int repairMode)
	{
		return stack.getMaxDamage() - stack.getDamage() <= repairMode;
	}
	
	private void putAwayDamagedTool(int repairMode)
	{
		PlayerInventory inv = MC.player.getInventory();
		int selectedSlot = inv.selectedSlot;
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		
		// If there's an empty slot in the main inventory,
		// shift-click the damaged item out of the hotbar
		OptionalInt emptySlot = IntStream.range(9, 36)
			.filter(i -> !inv.getStack(i).isEmpty()).findFirst();
		if(emptySlot.isPresent())
		{
			im.windowClick_QUICK_MOVE(
				InventoryUtils.toNetworkSlot(selectedSlot));
			return;
		}
		
		// Failing that, swap with a non-damageable item
		OptionalInt nonDamageableSlot = IntStream.range(9, 36)
			.filter(i -> !isDamageable(inv.getStack(i))).findFirst();
		if(nonDamageableSlot.isPresent())
		{
			im.windowClick_SWAP(nonDamageableSlot.getAsInt(), selectedSlot);
			return;
		}
		
		// Failing that, swap with a less damaged item
		OptionalInt notTooDamagedSlot = IntStream.range(9, 36)
			.filter(i -> !isTooDamaged(inv.getStack(i), repairMode))
			.findFirst();
		if(notTooDamagedSlot.isPresent())
		{
			im.windowClick_SWAP(notTooDamagedSlot.getAsInt(), selectedSlot);
			return;
		}
		
		// Failing all of the above (whole inventory full of damaged tools),
		// just swap with the top-left slot
		im.windowClick_SWAP(0, selectedSlot);
	}
	
	private boolean isWrongTool(ItemStack heldItem, BlockState state)
	{
		return getMiningSpeed(heldItem, state) <= 1;
	}
	
	private void selectFallbackSlot()
	{
		int fallbackSlot = getFallbackSlot();
		PlayerInventory inventory = MC.player.getInventory();
		
		if(fallbackSlot == -1)
		{
			if(inventory.selectedSlot == 8)
				inventory.selectedSlot = 0;
			else
				inventory.selectedSlot++;
			
			return;
		}
		
		inventory.selectedSlot = fallbackSlot;
	}
	
	private int getFallbackSlot()
	{
		PlayerInventory inventory = MC.player.getInventory();
		
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == inventory.selectedSlot)
				continue;
			
			ItemStack stack = inventory.getStack(slot);
			
			if(!isDamageable(stack))
				return slot;
		}
		
		return -1;
	}

	public float getToolValue(MCTool mcTool, ItemStack stack, Item item) {
		float speed;
		if (mcTool == MCTool.Shovel && item instanceof ShovelItem shovel)
			speed = shovel.getMaterial().getMiningSpeedMultiplier();
		else if (mcTool == MCTool.Pickaxe && item instanceof PickaxeItem pickaxe)
			speed = pickaxe.getMaterial().getMiningSpeedMultiplier();
		else if (mcTool == MCTool.Axe && item instanceof AxeItem axe)
			speed = axe.getMaterial().getMiningSpeedMultiplier();
		else if (mcTool == MCTool.Hoe && item instanceof HoeItem hoe)
			speed = hoe.getMaterial().getMiningSpeedMultiplier();
		else
			return -1;

		int enchantmentBonus = 0;

		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack) * 3;
		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.FORTUNE, stack);
		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.MENDING, stack);
		enchantmentBonus += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);

		return speed * 5 + enchantmentBonus;
	}

	public AbstractMap.SimpleEntry<Map<MCTool, Integer>, Map<MCTool, Float>> getBestTools() {
		Map<AutoToolHack.MCTool, Integer> bestToolsSlots = new HashMap<>();
		Map<AutoToolHack.MCTool, Float> bestToolsValues = new HashMap<>();

		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = MC.player.getInventory().getStack(slot);
			if (stack.isEmpty()) continue;
			Item item = stack.getItem();
			AutoToolHack.MCTool toolType = getMCTool(item);
			float value = getToolValue(toolType, stack, item);

			if (value > bestToolsValues.getOrDefault(toolType, -1f)) {
				bestToolsValues.put(toolType, value);
				bestToolsSlots.put(toolType, slot);
			}
		}

		return new AbstractMap.SimpleEntry<>(bestToolsSlots, bestToolsValues);
	}

	public enum MCTool {
		Shovel, Pickaxe, Axe, Hoe, Null
	}

	public MCTool getMCTool(Item item)
	{
		MCTool toolType = MCTool.Null;
		if (item instanceof ShovelItem) {
			toolType = MCTool.Shovel;
		} else if (item instanceof PickaxeItem) {
			toolType = MCTool.Pickaxe;
		} else if (item instanceof AxeItem) {
			toolType = MCTool.Axe;
		} else if (item instanceof HoeItem) {
			toolType = MCTool.Hoe;
		}

		return toolType;
	}

}
