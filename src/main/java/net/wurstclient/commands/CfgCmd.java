/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import com.google.gson.*;
import net.minecraft.client.util.InputUtil;
import net.wurstclient.DontBlock;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hack.Hack;
import net.wurstclient.hack.HackList;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.keybinds.KeybindList;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.SettingsFile;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonArray;
import net.wurstclient.util.json.WsonObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DontBlock
public final class CfgCmd extends Command
{
	public CfgCmd()
	{
		super("cfg", "Allows you to make configs of wurst settings.",
			".cfg load <name>",
			".cfg save <name>",
			".cfg list [<page>]",
			".cfg delete",
			".cfg dir",
			"Configs are saved in '" + WURST.cfgDir + "'.");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		switch(args[0].toLowerCase())
		{
			case "load":
			loadCfg(args);
			break;
			
			case "save":
			saveCfg(args);
			break;
			
			case "list":
			cfgList(args);
			break;

			case "remove":
			case "delete":
			case "rm":
			case "del":
			deleteCfg(args);
			break;

			case "dir":
			case "path":
			case "folder":
			openCfgDir();
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}

	private void openCfgDir()
	{
        try {
			Runtime.getRuntime().exec("explorer.exe \"" + WURST.cfgDir + "\"");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private void deleteCfg(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();

		String name = parseFileName(args[1]);

		try {
			String fullPath = new File(WURST.cfgDir.toFile(), name).getAbsolutePath();
			File file = new File(fullPath);
			if(!(file.exists() && !file.isDirectory())) {
				throw new NoSuchFileException(file.getPath());
			}
			boolean deleted = file.delete();
			if (deleted)
				ChatUtils.message("Cfg '" + name.replace(".json", "") + "' deleted");
			else
				throw new CmdError("Can't delete '" + name.replace(".json", "") + "' cfg");
		}
		catch (NoSuchFileException ex) {
			throw new CmdError("Cfg '" + name.replace(".json", "") + "' doesn't exist.");
		}
		catch (Exception ex) {
			throw new CmdError("Can't delete '" + name.replace(".json", "") + "' cfg: " + ex);
		}
	}
	
	private void loadCfg(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();

		String name = parseFileName(args[1]);

		try
		{
			String fullPath = new File(WURST.cfgDir.toFile(), name).getAbsolutePath();
			File f = new File(fullPath);
			if(!(f.exists() && !f.isDirectory())) {
				throw new NoSuchFileException(f.getPath());
			}

			Reader reader = Files.newBufferedReader(Paths.get(fullPath));
			Gson gson = new Gson();
			JsonObject fullJson;
			try {
				fullJson = gson.fromJson(reader, JsonObject.class);
			} catch (Exception ignored) { throw new IOException("Invalid JSON"); }

			// Load hacks
			try {
				JsonArray hacksJson = fullJson.getAsJsonArray("hacks");
				for (Hack hack : WURST.getHax().getAllHax())
					hack.setEnabled(false);

				for (JsonElement el : hacksJson) {
					String hackName = el.getAsString();
					Hack hack = WURST.getHax().getHackByName(hackName);
					if (hack == null || !hack.isStateSaved())
						continue;

					hack.setEnabled(true);
				}
			}
			catch (Exception e) { throw new CmdError("Can't load enabled hacks from cfg: " + e.getMessage()); }
			finally {
				JsonArray json = createJson(WURST.getHax());

				try {
					JsonUtils.toJson(json, WURST.getHax().getEnabledHacksFile().getPath());
				} catch(IOException | JsonException e)
				{
					System.out.println("Couldn't save enabled hacks as default: " + WURST.getHax().getEnabledHacksFile().getPath().getFileName());
					e.printStackTrace();
				}
			}

			// Load keybinds
			try {
				JsonObject keybindsJson = fullJson.getAsJsonObject("keybinds");
				Set<Keybind> newKeybinds = new HashSet<>();

				for (Map.Entry<String, JsonElement> entry : keybindsJson.entrySet()) {
					String key = entry.getKey();
					String command = entry.getValue().getAsString();

					if(!isValidKeyName(key))
						continue;

					Keybind keybind = new Keybind(key, command);
					newKeybinds.add(keybind);
				}

				WURST.getKeybinds().setKeybinds(newKeybinds);
			}
			catch (Exception e) { throw new CmdError("Can't load keybinds from cfg: " + e.getMessage()); }

			// Load settings
			JsonObject settingsJson = fullJson.getAsJsonObject("settings");
			try
			{
				for(Map.Entry<String, JsonElement> e : settingsJson.entrySet())
				{
					Feature feature = WURST.getSettingsFile().featuresWithSettings.get(e.getKey());

					if(feature == null)
						continue;

					for (Map.Entry<String, JsonElement> settingEntry : e.getValue().getAsJsonObject().entrySet()) {
						Map<String, Setting> settings = feature.getSettings();
						String key = settingEntry.getKey().toLowerCase();
						if(!settings.containsKey(key))
							continue;

						settings.get(key).fromJson(settingEntry.getValue());
					}
				}
			}
			catch (Exception e) { throw new CmdError("Can't load settings from cfg: " + e.getMessage()); }
			finally
			{
				JsonObject json = createJson(WURST.getSettingsFile());

				try
				{
					JsonUtils.toJson(json, WURST.getSettingsFile().getPath());

				}catch(IOException | JsonException e)
				{
					System.out.println("Couldn't save settings as default: " + WURST.getSettingsFile().getPath().getFileName());
					e.printStackTrace();
				}
			}

			ChatUtils.message("Cfg loaded: " + name.replace(".json", ""));
		}
		catch(NoSuchFileException e) {
			throw new CmdError("Cfg '" + name.replace(".json", "") + "' doesn't exist.");
		} catch(IOException e) {
			e.printStackTrace();
			throw new CmdError("Couldn't load cfg: " + e.getMessage());
		}
	}
	
	private void saveCfg(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = parseFileName(args[1]);
		String fullPath = new File(WURST.cfgDir.toFile(), name).getAbsolutePath();

        JsonObject json = new JsonObject();
        json.add("hacks", createJson(WURST.getHax()));
        json.add("keybinds", createJson(WURST.getKeybinds()));
        json.add("settings", createJson(WURST.getSettingsFile()));

        try (Writer writer = Files.newBufferedWriter(Paths.get(fullPath))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(json, writer);
        } catch (Exception e) {
			throw new CmdError("Couldn't save cfg: " + e.getMessage());
        }

        ChatUtils.message("Cfg saved: " + name.replace(".json", ""));
    }
	
	private String parseFileName(String input)
	{
		String fileName = input;
		if(!fileName.endsWith(".json"))
			fileName += ".json";
		
		return fileName;
	}
	
	private void cfgList(String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		ArrayList<Path> files = cfgList();
		int page = parsePage(args);
		int pages = (int)Math.ceil(files.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("Invalid page: " + page);
		
		String total = "Total: " + files.size() + " cfg";
		total += files.size() != 1 ? "s" : "";
		ChatUtils.message(total);
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, files.size());
		
		ChatUtils.message("Cfg list (page " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(files.get(i).getFileName().toString().replace(".json", ""));
	}
	
	private int parsePage(String[] args) throws CmdSyntaxError
	{
		if(args.length < 2)
			return 1;
		
		if(!MathUtils.isInteger(args[1]))
			throw new CmdSyntaxError("Not a number: " + args[1]);
		
		return Integer.parseInt(args[1]);
	}

	private JsonObject createJson(SettingsFile settingsFile)
	{
		JsonObject json = new JsonObject();

		for(Feature feature : settingsFile.featuresWithSettings.values())
		{
			Collection<Setting> settings = feature.getSettings().values();

			JsonObject jsonSettings = new JsonObject();
			settings.forEach(s -> jsonSettings.add(s.getName(), s.toJson()));

			json.add(feature.getName(), jsonSettings);
		}

		return json;
	}

	private JsonArray createJson(HackList hax)
	{
		Stream<Hack> enabledHax = hax.getAllHax().stream()
				.filter(Hack::isEnabled).filter(Hack::isStateSaved);

		JsonArray json = new JsonArray();
		enabledHax.map(Hack::getName).forEach(json::add);

		return json;
	}

	private JsonObject createJson(KeybindList list)
	{
		JsonObject json = new JsonObject();

		for(Keybind kb : list.getAllKeybinds())
			json.addProperty(kb.getKey(), kb.getCommands());

		return json;
	}

	private boolean isValidKeyName(String key)
	{
		try
		{
			InputUtil.fromTranslationKey(key);
			return true;

		}catch(IllegalArgumentException e)
		{
			return false;
		}
	}

	private ArrayList<Path> cfgList()
	{
		if(!Files.isDirectory(WURST.cfgDir))
			return new ArrayList<>();

		try(Stream<Path> files = Files.list(WURST.cfgDir)) {
			return files.filter(Files::isRegularFile).collect(Collectors.toCollection(ArrayList::new));
		}catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
