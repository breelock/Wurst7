/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

public class FriendsCmd extends Command
{
	private static final int FRIENDS_PER_PAGE = 8;
	
	private final CheckboxSetting middleClickFriends =
		new CheckboxSetting("Middle click frens",
			"Add/remove fren by clicking them with the middle mouse button.",
			true);
	
	public FriendsCmd()
	{
		super("friends", "Manages your frens list.", ".friends add <name>",
			".friends remove <name>", ".friends clear", ".friends clear temp",
			".friends list [<page>]");
		
		addSetting(middleClickFriends);
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1 || args.length > 2)
			throw new CmdSyntaxError();
		
		switch(args[0].toLowerCase())
		{
			case "add":
			add(args);
			break;
			
			case "remove":
			case "delete":
			case "rm":
			case "del":
			remove(args);
			break;
			
			case "clear":
			case "clean":
			removeAll(args);
			break;
			
			case "list":
			list(args);
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}

	private void add(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = args[1];
		if(WURST.getFriends().contains(name))
			throw new CmdError(
				"\"" + name + "\" is already in your frens list.");
		
		WURST.getFriends().addAndSave(name);
		ChatUtils.message("Added fren \"" + name + "\".");
	}
	
	private void remove(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String name = args[1];
		if(!WURST.getFriends().contains(name))
			throw new CmdError("\"" + name + "\" is not in your frens list.");
		
		WURST.getFriends().removeAndSave(name);
		ChatUtils.message("Removed fren \"" + name + "\".");
	}
	
	private void removeAll(String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();

		if (args.length == 2)
		{
			if (args[1].equals("tmp") || args[1].equals("temp") || args[1].equals("teammates"))
			{
				TreeSet<String> tempFriends;
				final Path path = MC.runDirectory.toPath().normalize().resolve("wurst").resolve("tempFriends.json");
				try { // getting list of temp frens
					tempFriends = new TreeSet<>(JsonUtils.parseFileToArray(path).getAllStrings());
				} catch (IOException | JsonException e) {
					tempFriends = new TreeSet<>();
				}

				if (!tempFriends.isEmpty()) {
					// del temp frens
					ArrayList<String> tmpFren = new ArrayList<>(tempFriends);
					for (int i = 0; i < tempFriends.size(); i++) {
						String name = tmpFren.get(i);
						if (WURST.getFriends().contains(name))
							WURST.getFriends().removeAndSave(name);
					}

					// clear temp frens
					try {
						JsonUtils.toJson(new JsonArray(), path);
					} catch (IOException | JsonException e) {
						//
					}
				}

				ChatUtils.message("All temp frens removed. Oof.");
			}
			else
			{
				throw new CmdSyntaxError();
			}
		}
		else
		{
			WURST.getFriends().removeAllAndSave();
			ChatUtils.message("All frens removed. Oof.");
		}
	}
	
	private void list(String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		ArrayList<String> friends = WURST.getFriends().toList();
		int page = parsePage(args);
		int pages = (int)Math.ceil(friends.size() / (double)FRIENDS_PER_PAGE);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("Invalid page: " + page);
		
		ChatUtils.message("Current frens: " + friends.size());
		
		int start = (page - 1) * FRIENDS_PER_PAGE;
		int end = Math.min(page * FRIENDS_PER_PAGE, friends.size());
		
		ChatUtils.message("Frens list (page " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(friends.get(i).toString());
	}
	
	private int parsePage(String[] args) throws CmdSyntaxError
	{
		if(args.length < 2)
			return 1;
		
		if(!MathUtils.isInteger(args[1]))
			throw new CmdSyntaxError("Not a number: " + args[1]);
		
		return Integer.parseInt(args[1]);
	}
	
	public CheckboxSetting getMiddleClickFriends()
	{
		return middleClickFriends;
	}
}
