/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

import java.util.HashMap;
import java.util.Map;

@SearchTags({"auto tool", "auto tool drop"})
public final class AutoToolDropHack extends Hack implements UpdateListener, PacketOutputListener
{
	private final CheckboxSetting swapWhileMoving = new CheckboxSetting(
			"Swap while moving",
			"Whether or not to swap while the player is moving.\n\n"
					+ "\u00a7c\u00a7lWARNING:\u00a7r This would not be possible without cheats. It may raise suspicion.",
			false);

	private final SliderSetting delay = new SliderSetting("Delay",
			"Amount of ticks to wait before swapping.",
			2, 1, 20, 1,
			ValueDisplay.INTEGER.withSuffix(" ticks").withLabel(1, "1 tick"));

	private int timer;

	private final Map<MCTool, Float> bestToolsValue = new HashMap<>();
	private final Map<MCTool, Integer> bestToolsSlots = new HashMap<>();

	public AutoToolDropHack()
	{
		super("AutoToolDrop");
		setCategory(Category.COMBAT);
		addSetting(swapWhileMoving);
		addSetting(delay);
	}

	@Override
	protected void onEnable()
	{
		timer = 0;
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}

	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
	}

	@Override
	public void onUpdate() {
		if (timer > 0) {
			timer--;
			return;
		}

		if (MC.currentScreen instanceof HandledScreen && !(MC.currentScreen instanceof InventoryScreen))
			return;

		ClientPlayerEntity player = MC.player;
		if (player == null)
			return;

		PlayerInventory inventory = player.getInventory();

		if (!swapWhileMoving.isChecked() && (player.input.movementForward != 0 || player.input.movementSideways != 0))
			return;

		// get best tools values
		for (int slot = 0; slot < 36; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isEmpty()) continue;
			Item item = stack.getItem();
			MCTool toolType = getMCTool(item);
			float value = getToolValue(toolType, stack, item);

			if (value > bestToolsValue.getOrDefault(toolType, -1f)) {
				bestToolsValue.put(toolType, value);
				bestToolsSlots.put(toolType, slot);
			}
		}

		// Throw away the worst tools
		for (int slot = 9; slot < 45; slot++) {
			int adjusted = slot >= 36 ? slot - 36 : slot;
			ItemStack stack = inventory.getStack(adjusted);

			if (bestToolsSlots.containsValue(adjusted))
				continue;

			if (!stack.isEmpty() && isWorseOrSameTool(stack)) {
				IMC.getInteractionManager().windowClick_THROW(slot);
			}
		}
	}

	public boolean isWorseOrSameTool(ItemStack candidate)
	{
		MCTool toolType = getMCTool(candidate.getItem());

		// If candidate is not tool
		if(toolType == MCTool.Null)
			return false;

		Item candidateItem = candidate.getItem();

		var player = MinecraftClient.getInstance().player;
		if(player == null)
			return false;

		ItemStack equipped;
		if (bestToolsSlots.containsKey(toolType))
			equipped = player.getInventory().getStack(bestToolsSlots.get(toolType));
		else
			return false;

		// If nothing is equipped
		if(equipped.isEmpty())
			return false;

		float candidateValue = getToolValue(toolType, candidate, candidateItem);
		float equippedValue = getToolValue(toolType, equipped, equipped.getItem());
		return candidateValue <= equippedValue;
	}

	public static float getToolValue(MCTool mcTool, ItemStack stack, Item item) {
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

	@Override
	public void onSentPacket(PacketOutputListener.PacketOutputEvent event)
	{
		if(event.getPacket() instanceof ClickSlotC2SPacket)
			timer = delay.getValueI();
	}

	public enum MCTool {
		Shovel, Pickaxe, Axe, Hoe, Null
	}

	private MCTool getMCTool(Item item)
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
