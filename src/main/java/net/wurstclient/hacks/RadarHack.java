/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Window;
import net.wurstclient.clickgui.components.RadarComponent;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.settings.filters.*;
import net.wurstclient.util.FakePlayerEntity;
import org.jetbrains.annotations.NotNull;

@SearchTags({"MiniMap", "mini map"})
public final class RadarHack extends Hack implements UpdateListener
{
	private final Window window;
	private final ArrayList<Entity> entities = new ArrayList<>();
	
	private final SliderSetting radius = new SliderSetting("Radius",
		"Radius in blocks.", 100, 1, 100, 1, ValueDisplay.INTEGER);
	private final CheckboxSetting rotate =
		new CheckboxSetting("Rotate with player", true);

	public final ColorSetting arrowColor =
			new ColorSetting("Arrow main color", "Arrow main color", new Color(0x000000));

	public final ColorSetting arrowOutline =
			new ColorSetting("Arrow outline color", "Arrow outline color", new Color(0x404040));
	
	private final EntityFilterList entityFilters =
		new EntityFilterList(FilterPlayersSetting.genericVision(false),
			FilterSleepingSetting.genericVision(false),
			FilterHostileSetting.genericVision(false),
			FilterPassiveSetting.genericVision(false),
			FilterPassiveWaterSetting.genericVision(false),
			FilterBatsSetting.genericVision(true),
			FilterSlimesSetting.genericVision(false),
			FilterInvisibleSetting.genericVision(false));

	private final CheckboxSetting onlyPlayers = new CheckboxSetting("Show only players",
			"Show only players", false);
	
	public RadarHack()
	{
		super("Radar");

		setCategory(Category.RENDER);
		addSetting(radius);
		addSetting(rotate);
		addSetting(arrowColor);
		addSetting(arrowOutline);
		addSetting(onlyPlayers);
		entityFilters.forEach(this::addSetting);
		
		window = new Window("Radar");
		window.setPinned(true);
		window.setInvisible(true);
		window.add(new RadarComponent(this));
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		window.setInvisible(false);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		window.setInvisible(true);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		entities.clear();
		Stream<Entity> stream;

		if (onlyPlayers.isChecked()) {
			stream = StreamSupport.stream(world.getEntities().spliterator(), true)
					.filter(e -> e instanceof PlayerEntity)
					.filter(e -> {
                        PlayerEntity p = (PlayerEntity) e;
                        return p.getHealth() > 0;
                    })
					.filter(e -> !e.isRemoved())
					.filter(e -> e != MC.player)
					.filter(e -> !(e instanceof FakePlayerEntity))
					.filter(e -> Math.abs(e.getY() - MC.player.getY()) <= 1e6);
		}
		else {
			stream = StreamSupport.stream(world.getEntities().spliterator(), true)
					.filter(e -> !e.isRemoved() && e != player)
					.filter(e -> !(e instanceof FakePlayerEntity))
					.filter(LivingEntity.class::isInstance)
					.filter(e -> ((LivingEntity)e).getHealth() > 0);

			stream = entityFilters.applyTo(stream);
		}
		
		entities.addAll(stream.collect(Collectors.toList()));
	}
	
	public Window getWindow()
	{
		return window;
	}
	
	public Iterable<Entity> getEntities()
	{
		return Collections.unmodifiableList(entities);
	}
	
	public double getRadius()
	{
		return radius.getValue();
	}
	
	public boolean isRotateEnabled()
	{
		return rotate.isChecked();
	}
}
