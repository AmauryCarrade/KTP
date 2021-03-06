package me.azenet.UHPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;

import me.azenet.UHPlugin.i18n.I18n;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UHPluginCommand implements CommandExecutor {
	
	private UHPlugin p = null;
	
	private ArrayList<String> commands = new ArrayList<String>();
	private ArrayList<String> teamCommands = new ArrayList<String>();
	private ArrayList<String> specCommands = new ArrayList<String>();
	private ArrayList<String> borderCommands = new ArrayList<String>();
	
	private I18n i = null;


	public UHPluginCommand(UHPlugin p) {
		this.p = p;
		this.i = p.getI18n();
		
		commands.add("start");
		commands.add("shift");
		commands.add("team");
		commands.add("addspawn");
		commands.add("generatewalls");
		commands.add("border");
		commands.add("heal");
		commands.add("healall");
		commands.add("resurrect");
		commands.add("tpback");
		commands.add("spec");
		
		teamCommands.add("add");
		teamCommands.add("remove");
		teamCommands.add("addplayer");
		teamCommands.add("removeplayer");
		teamCommands.add("list");
		teamCommands.add("reset");
		
		specCommands.add("add");
		specCommands.add("remove");
		specCommands.add("list");
		
		borderCommands.add("current");
		borderCommands.add("set");
		borderCommands.add("warning");
		borderCommands.add("check");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("uh") && !command.getName().equalsIgnoreCase("t")) {
			return false;
		}
		
		if(command.getName().equalsIgnoreCase("t")) { // Special case for /t command
			doTeamMessage(sender, command, label, args);
			return true;
		}
		
		if(args.length == 0) {
			help(sender, false);
			return true;
		}
		
		String subcommandName = args[0].toLowerCase();
		
		// First: subcommand existence.
		if(!this.commands.contains(subcommandName)) {
			help(sender, true);
			return true;
		}
		
		// Second: is the sender allowed?
		if(!isAllowed(sender, subcommandName)) {
			unauthorized(sender, command);
			return true;
		}
		
		// Third: instantiation
		try {
			Class<? extends UHPluginCommand> cl = this.getClass();
			Class[] parametersTypes = new Class[]{CommandSender.class, Command.class, String.class, String[].class};
			
			Method doMethod = cl.getDeclaredMethod("do" + WordUtils.capitalize(subcommandName), parametersTypes);
			
			doMethod.invoke(this, new Object[]{sender, command, label, args});
			
			return true;
			
		} catch (NoSuchMethodException e) {
			// Unknown method => unknown subcommand.
			help(sender, true);
			return true;
			
		} catch(SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			sender.sendMessage(i.t("cmd.errorLoad"));
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Prints the help.
	 * 
	 * @param sender
	 * @param error True if the help is printed because the used typed an unknown command.
	 */
	private void help(CommandSender sender, boolean error) {
		sender.sendMessage(i.t("cmd.titleHelp", p.getDescription().getDescription(), p.getDescription().getVersion()));
		
		if(error) {
			sender.sendMessage(i.t("cmd.errorUnknown"));
		}
		
		sender.sendMessage(i.t("cmd.inviteHelp"));
		sender.sendMessage(i.t("cmd.legendHelp"));
		
		sender.sendMessage(i.t("cmd.titleGameCmd"));
		sender.sendMessage(i.t("cmd.helpStart"));
		sender.sendMessage(i.t("cmd.helpStartSlow"));
		sender.sendMessage(i.t("cmd.helpShift"));
		sender.sendMessage(i.t("cmd.helpTeam"));
		sender.sendMessage(i.t("cmd.helpAddspawn"));
		sender.sendMessage(i.t("cmd.helpAddspawnXZ"));
		sender.sendMessage(i.t("cmd.helpSpec"));
		sender.sendMessage(i.t("cmd.helpWall"));
		sender.sendMessage(i.t("cmd.helpBorder"));
		
		sender.sendMessage(i.t("cmd.titleBugCmd"));
		sender.sendMessage(i.t("cmd.helpHeal"));
		sender.sendMessage(i.t("cmd.helpHealall"));
		sender.sendMessage(i.t("cmd.helpResurrect"));
		sender.sendMessage(i.t("cmd.helpTpback"));
	}
	
	/**
	 * This method checks if an user is allowed to send a command.
	 * 
	 * @param sender
	 * @param subcommand
	 * @return boolean The allowance status.
	 */
	private boolean isAllowed(CommandSender sender, String subcommand) {
		if(sender instanceof Player) {
			if(sender.isOp()) {
				return true;
			}
			else if(sender.hasPermission("uh." + subcommand)) {
				return true;
			}
			else {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * This method sends a message to a player who try to use a command without the permission.
	 * 
	 * @param sender
	 * @param command
	 */
	private void unauthorized(CommandSender sender, Command command) {
		sender.sendMessage(i.t("cmd.errorUnauthorized"));
	}
	
	/**
	 * This command starts the game.
	 * Usage: /uh start [slow]
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doStart(CommandSender sender, Command command, String label, String[] args) {
		
		if(args.length == 1) { // /uh start (standard mode)
			try {
				p.getGameManager().start(sender, false);
			} catch(RuntimeException e) {
				sender.sendMessage(i.t("start.already"));
			}
		}
		else if(args.length == 2 && args[1].equalsIgnoreCase("slow")) { // /uh start slow
			try {
				p.getGameManager().start(sender, true);
			} catch(RuntimeException e) {
				sender.sendMessage(i.t("start.already"));
			}
		}
		else if(args.length == 3 && args[1].equalsIgnoreCase("slow") && args[2].equalsIgnoreCase("go")) { // /uh start slow go
			p.getGameManager().finalizeStartSlow(sender);
		}
		else {
			sender.sendMessage(i.t("start.syntax"));
		}
	}
	
	/**
	 * This command generates the walls around the map.
	 * Usage: /uh generatewalls
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doGeneratewalls(CommandSender sender, Command command, String label, String[] args) {	
		sender.sendMessage(i.t("wall.startGen"));
		
		World world = null;
		
		if(sender instanceof Player) {
			world = ((Player) sender).getWorld();
		}
		else {
			world = p.getServer().getWorlds().get(0);
			sender.sendMessage(i.t("wall.consoleDefaultWorld", world.getName()));
		}
		
		try {
			UHWallGenerator wallGenerator = new UHWallGenerator(this.p, world);
			Boolean success = wallGenerator.build();
			
			if(!success) {
				sender.sendMessage(i.t("wall.error"));
			}
		}
		catch(Exception e) {
			sender.sendMessage(i.t("wall.unknownError"));
			e.printStackTrace();
		}
		
		sender.sendMessage(i.t("wall.done"));
	}

	/**
	 * This command adds a spawn point for a team or a player.
	 * Usage: /uh addspawn (as a player).
	 * Usage: /uh addspawn <x> <z> (as everyone).
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doAddspawn(CommandSender sender, Command command, String label, String[] args) {	
		if(args.length == 1) { // No coordinates given.
			if(!(sender instanceof Player)) {
				sender.sendMessage(i.t("addspawn.errorCoords"));
				sender.sendMessage(i.t("addspawn.usage"));
				return;
			}
			else {
				Player pl = (Player) sender; // Just a way to avoid casts everywhere.
				p.getGameManager().addLocation(pl.getLocation().getBlockX(), pl.getLocation().getBlockZ());
				sender.sendMessage(i.t("addspawn.added", String.valueOf(pl.getLocation().getBlockX()), String.valueOf(pl.getLocation().getBlockZ())));
			}
		}
		else if(args.length == 2) { // Two coordinates needed!
			sender.sendMessage(i.t("addspawn.error2Coords"));
			sender.sendMessage(i.t("addspawn.usage"));
		}
		else {
			p.getGameManager().addLocation(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
			sender.sendMessage(i.t("addspawn.added", args[1], args[2]));
		}
	}
	
	/**
	 * This command is used to manage the teams.
	 * Usage: /uh team (for the doc).
	 * Usage: /uh team <add|remove|addplayer|removeplayer|list|reset> (see doc for details).
	 * 	
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doTeam(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 1) { // No action provided: doc
			sender.sendMessage(i.t("cmd.teamHelpAvailable"));
			sender.sendMessage(i.t("cmd.teamHelpAdd"));
			sender.sendMessage(i.t("cmd.teamHelpAddName"));
			sender.sendMessage(i.t("cmd.teamHelpRemove"));
			sender.sendMessage(i.t("cmd.teamHelpAddplayer"));
			sender.sendMessage(i.t("cmd.teamHelpRemoveplayer"));
			sender.sendMessage(i.t("cmd.teamHelpList"));
			sender.sendMessage(i.t("cmd.teamHelpReset"));
		}
		else {
			UHTeamManager tm = p.getTeamManager();
			String subcommand = args[1];
			
			if(subcommand.equalsIgnoreCase("add")) {
				if(args.length == 3) { // /uh team add <color>
					
					ChatColor color = p.getTeamManager().getChatColorByName(args[2]);
					
					if(color == null) {
						sender.sendMessage(i.t("team.add.errorColor"));
					}
					else {
						try {
							tm.addTeam(color, args[2].toLowerCase());
						}
						catch(IllegalArgumentException e) {
							sender.sendMessage(i.t("team.add.errorExists"));
						}
						sender.sendMessage(i.t("team.add.added", color.toString(), args[2]));
					}
				
				}
				else if(args.length == 4) { // /uh team add <color> <name>
					
					ChatColor color = p.getTeamManager().getChatColorByName(args[2]);
					
					if(color == null) {
						sender.sendMessage(i.t("team.add.errorColor"));
					}
					else if(args[3].length() > 16) {
						sender.sendMessage(i.t("team.add.nameTooLong"));
					}
					else {
						try {
							tm.addTeam(color, args[3].toLowerCase());
						}
						catch(IllegalArgumentException e) {
							sender.sendMessage(i.t("team.add.errorExists"));
							return;
						}
						sender.sendMessage(i.t("team.add.added", color.toString(), args[3]));
					}
					
				}
				else {
					sender.sendMessage(i.t("team.syntaxError"));
				}
			}
			
			
			else if(subcommand.equalsIgnoreCase("remove")) {
				if(args.length == 3) { // /uh team remove <teamName>
					if(!tm.removeTeam(args[2].toLowerCase())) {
						sender.sendMessage(i.t("team.remove.doesNotExists"));
					}
					else {
						sender.sendMessage(i.t("team.remove.removed", args[2]));
					}
				}
				else {
					sender.sendMessage(i.t("team.syntaxError"));
				}
			}
			
			
			else if(subcommand.equalsIgnoreCase("addplayer")) {
				if(args.length == 4) { // /uh team addplayer <teamName> <player>
					
					Player player = p.getServer().getPlayer(args[3]);
					
					if(player == null || !player.isOnline()) {
						sender.sendMessage(i.t("team.addplayer.disconnected", args[3], args[2]));
					}
					else {
						try {
							tm.addPlayerToTeam(args[2], player);
						} catch(IllegalArgumentException e) {
							sender.sendMessage(i.t("team.addplayer.doesNotExists"));
							return;
						}
						sender.sendMessage(i.t("team.addplayer.success", args[3], args[2]));
					}
				}
				else {
					sender.sendMessage(i.t("team.syntaxError"));
				}
			}
			
			
			else if(subcommand.equalsIgnoreCase("removeplayer")) {
				if(args.length == 3) { // /uh team removeplayer <player>
					
					Player player = p.getServer().getPlayer(args[2]);
					
					if(player == null || !player.isOnline()) {
						sender.sendMessage(i.t("team.removeplayer.disconnected", args[2]));
					}
					else {
						tm.removePlayerFromTeam(player);
						sender.sendMessage(i.t("team.removeplayer.success", args[2]));
					}
				}
				else {
					sender.sendMessage(i.t("team.syntaxError"));
				}
			}
			
			
			else if(subcommand.equalsIgnoreCase("list")) {
				if(tm.getTeams().size() == 0) {
					sender.sendMessage(i.t("team.list.nothing"));
					return;
				}
				
				ChatColor lc = ChatColor.LIGHT_PURPLE; // List color
				
				for(final UHTeam team : tm.getTeams()) {
					sender.sendMessage(i.t("team.list.itemTeam", team.getChatColor().toString(), team.getName(), ((Integer) team.getPlayers().size()).toString()));
					for(final Player player : team.getPlayers()) {
						if(!p.getGameManager().isGameRunning()) {
							sender.sendMessage(i.t("team.list.itemPlayer", player.getName()));
						}
						else {
							if(p.getGameManager().isPlayerDead(player.getName())) {
								sender.sendMessage(i.t("team.list.itemPlayerDead", player.getName()));
							}
							else {
								sender.sendMessage(i.t("team.list.itemPlayerAlive", player.getName()));
							}
						}
					}
				}
			}
			
			else if(subcommand.equalsIgnoreCase("reset")) {
				tm.reset();
				sender.sendMessage(i.t("team.reset.success"));
			}
			else {
				sender.sendMessage(i.t("team.unknownCommand"));
			}
		}
	}
	
	/**
	 * This command shifts an episode.
	 * Usage: /uh shift (during the game).
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doShift(CommandSender sender, Command command, String label, String[] args) {
		if(p.getGameManager().isGameRunning()) {
			if(sender instanceof Player) {
				p.getGameManager().shiftEpisode((((Player) sender).getName()));
			}
			else {
				p.getGameManager().shiftEpisode(i.t("shift.consoleName"));
			}
		}
		else {
			sender.sendMessage(i.t("shift.cantNotStarted"));
		}
	}
	
	
	/**
	 * This command heals a player.
	 * Usage: /uh heal <player> <half-hearts>
	 * 
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doHeal(CommandSender sender, Command command, String label, String[] args) {
		if(args.length < 2 || args.length > 3) {
			sender.sendMessage(i.t("heal.usage"));
			return;
		}
		
		Player player = p.getServer().getPlayer(args[1]);
		if(player == null || !player.isOnline()) {
			sender.sendMessage(i.t("heal.offline"));
			return;
		}
		
		double health = 0D;
		
		if(args.length == 2) { // /uh heal <player> : full life for player.
			health = 20D;
		}
		else { // /uh heal <player> <hearts>
			try {
				health = Double.parseDouble(args[2]);
			}
			catch(NumberFormatException e) {
				sender.sendMessage(i.t("heal.errorNaN"));
				return;
			}
			
			if(health <= 0D) {
				sender.sendMessage(i.t("heal.errorNoKill"));
				return;
			}
			else if(health > 20D) {
				health = 20D;
			}
		}
		
		player.setHealth(health);
	}
	
	/**
	 * This command heals all players.
	 * Usage: /uh healall <half-hearts>
	 * 
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doHealall(CommandSender sender, Command command, String label, String[] args) {
		String healthArg = null;
		if(args.length == 1) {
			healthArg = "20";
		}
		else {
			healthArg = args[1];
		}
		
		try {
			if(Double.parseDouble(healthArg) <= 0D) {
				sender.sendMessage(i.t("heal.allErrorNoKill"));
				return;
			}
		}
		catch(NumberFormatException e) {
			sender.sendMessage(i.t("heal.errorNaN"));
			return;
		}
		
		for(final Player player : p.getServer().getOnlinePlayers()) {
			String[] argsToHeal = {"heal", player.getName(), healthArg};
			doHeal(sender, command, label, argsToHeal);
		}
	}
	
	
	/**
	 * This command resurrects a player.
	 * Usage: /uh resurrect <player>
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doResurrect(CommandSender sender, Command command, String label, String[] args) {
		if(args.length != 2) {
			sender.sendMessage(i.t("resurrect.usage"));
			return;
		}
		
		boolean success = p.getGameManager().resurrect(args[1]);
		
		Player player = p.getServer().getPlayer(args[1]);
		if(player == null || !player.isOnline()) {
			if(!success) { // Player does not exists or is nod dead.
				sender.sendMessage(i.t("resurrect.unknownOrDead"));
			}
			else { // Resurrected
				sender.sendMessage(i.t("resurrect.offlineOk", args[1]));
			}
		}
		else {
			if(!success) { // The player is not dead
				sender.sendMessage(i.t("resurrect.notDead", args[1]));
			}
		}
	}
	
	/**
	 * This command safely teleports back a player to his death location.
	 * Usage: /uh tpback <player>
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doTpback(CommandSender sender, Command command, String label, String[] args) {
		if(args.length < 2) {
			sender.sendMessage(i.t("tpback.usage"));
			return;
		}
		
		Player player = p.getServer().getPlayer(args[1]);
		if(player == null || !player.isOnline()) {
			sender.sendMessage(i.t("tpback.offline", args[1]));
			return;
		}
		else if(!p.getGameManager().hasDeathLocation(player)) {
			sender.sendMessage(i.t("tpback.noDeathLocation", args[1]));
			return;
		}
		
		
		Location deathLocation = p.getGameManager().getDeathLocation(player);
		
		if(args.length >= 3 && args[2].equalsIgnoreCase("force")) {
			UHUtils.safeTP(player, deathLocation, true);
			sender.sendMessage(i.t("tpback.teleported", args[1]));
			p.getGameManager().removeDeathLocation(player);
		}
		else if(UHUtils.safeTP(player, deathLocation)) {
			sender.sendMessage(i.t("tpback.teleported", args[1]));
			p.getGameManager().removeDeathLocation(player);
		}
		else {
			sender.sendMessage(i.t("tpback.notTeleportedNoSafeSpot", args[1]));
			sender.sendMessage(i.t("tpback.notTeleportedNoSafeSpotCmd", args[1]));
		}
	}
	
	
	/**
	 * This command manages spectators (aka ignored players).
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doSpec(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 1) { // /uh spec
			sender.sendMessage(i.t("cmd.specHelpAvailable"));
			if(!p.getSpectatorPlusIntegration().isSPIntegrationEnabled()) {
				sender.sendMessage(i.t("cmd.specHelpNoticeSpectatorPlusNotInstalled"));
			}
			sender.sendMessage(i.t("cmd.specHelpAdd"));
			sender.sendMessage(i.t("cmd.specHelpRemove"));
			sender.sendMessage(i.t("cmd.specHelpList"));
		}
		else {
			String subcommand = args[1];
			
			if(subcommand.equalsIgnoreCase("add")) {
				if(args.length == 2) { // /uh spec add
					sender.sendMessage(i.t("spectators.syntaxError"));
				}
				else { // /uh spec add <player>
					Player newSpectator = p.getServer().getPlayer(args[2]);
					if(newSpectator == null) {
						sender.sendMessage(i.t("spectators.offline", args[2]));
					}
					else {
						p.getGameManager().addSpectator(newSpectator);
						sender.sendMessage(i.t("spectators.add.success", args[2]));
					}
				}
			}
			
			else if(subcommand.equalsIgnoreCase("remove")) {
				if(args.length == 2) { // /uh spec remove
					sender.sendMessage(i.t("spectators.syntaxError"));
				}
				else { // /uh spec remove <player>
					Player oldSpectator = p.getServer().getPlayer(args[2]);
					if(oldSpectator == null) {
						sender.sendMessage(i.t("spectators.offline", args[2]));
					}
					else {
						p.getGameManager().removeSpectator(oldSpectator);
						sender.sendMessage(i.t("spectators.remove.success", args[2]));
					}
				}
			}
			
			else if(subcommand.equalsIgnoreCase("list")) {
				HashSet<String> spectators = p.getGameManager().getSpectators();
				if(spectators.size() == 0) {
					sender.sendMessage(i.t("spectators.list.nothing"));
				}
				else {
					sender.sendMessage(i.t("spectators.list.countSpectators", String.valueOf(spectators.size())));
					sender.sendMessage(i.t("spectators.list.countOnlyInitial"));
					for(String spectator : spectators) {
						sender.sendMessage(i.t("spectators.list.itemSpec", spectator));
					}
				}
			}
		}
	}
	
	
	/**
	 * This command manages borders (gets current, checks if players are out, sets a new size, warnings players
	 * about the futur size).
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void doBorder(CommandSender sender, Command command, String label, String[] args) {
		if(args.length == 1) { // /uh border
			sender.sendMessage(i.t("cmd.borderHelpAvailable"));
			sender.sendMessage(i.t("cmd.borderHelpCurrent"));
			sender.sendMessage(i.t("cmd.borderHelpSet"));
			sender.sendMessage(i.t("cmd.borderHelpWarning"));
			sender.sendMessage(i.t("cmd.borderHelpWarningCancel"));
			sender.sendMessage(i.t("cmd.borderHelpCheck"));
		}
		else {
			String subcommand = args[1];
			
			if(subcommand.equalsIgnoreCase("current")) { // /uh border current
				sender.sendMessage(i.t("borders.current.message", String.valueOf(p.getBorderManager().getCurrentBorderDiameter())));
			}
			
			else if(subcommand.equalsIgnoreCase("set")) { // /uh border set
				if(args.length == 2) { // /uh border set
					sender.sendMessage(i.t("borders.syntaxError"));					
				}
				else if(args.length == 3) { // /uh border set <?>
					try {
						Integer newDiameter = Integer.valueOf(args[2]);
						
						if(p.getBorderManager().getPlayersOutside(newDiameter).size() != 0) { // Some players are outside
							sender.sendMessage(i.t("borders.set.playersOutsideCanceled"));
							sender.sendMessage(i.t("borders.set.playersOutsideCanceledCmd", args[2]));
							if(!p.getWorldBorderIntegration().isWBIntegrationEnabled()) {
								sender.sendMessage(i.t("borders.set.playersOutsideCanceledWarnWorldBorder"));
							}
						}
						else {
							p.getBorderManager().setCurrentBorderDiameter(newDiameter);
							p.getServer().broadcastMessage(i.t("borders.set.broadcast", args[2]));
						}
						
					} catch(NumberFormatException e) {
						sender.sendMessage(i.t("borders.NaN", args[2]));
					}
				}
				else if(args.length == 4 && args[3].equalsIgnoreCase("force")) { // /uh border set <?> force
					try {
						Integer newDiameter = Integer.valueOf(args[2]);
						
						p.getBorderManager().setCurrentBorderDiameter(newDiameter);
						p.getServer().broadcastMessage(i.t("borders.set.broadcast", args[2]));
						
					} catch(NumberFormatException e) {
						sender.sendMessage(i.t("borders.NaN", args[2]));
					}
				}
			}
			
			else if(subcommand.equalsIgnoreCase("warning")) { // /uh border warning 
				if(args.length == 2) { // /uh border warning
					sender.sendMessage(i.t("borders.syntaxError"));					
				}
				else if(args[2].equalsIgnoreCase("cancel")) { // /uh border warning cancel
					p.getBorderManager().cancelWarning();
					sender.sendMessage(i.t("borders.warning.canceled"));
				}
				else { // /uh border warning <?>
					try {
						Integer warnDiameter = Integer.valueOf(args[2]);
						p.getBorderManager().setWarningSize(warnDiameter);
						sender.sendMessage(i.t("borders.warning.set", p.getConfig().getString("map.border.warningInterval", "90")));
						
					} catch(NumberFormatException e) {
						sender.sendMessage(i.t("borders.NaN", args[2]));
					}
				}
			}
			else if(subcommand.equalsIgnoreCase("check")) {
				if(args.length == 2) { // /uh border check
					sender.sendMessage(i.t("borders.syntaxError"));					
				}
				else { // /uh border check <?>
					try {
						Integer checkDiameter = Integer.valueOf(args[2]);
						HashSet<Player> playersOutside = p.getBorderManager().getPlayersOutside(checkDiameter);
						
						if(playersOutside.size() == 0) {
							sender.sendMessage(i.t("borders.check.allPlayersInside"));
						}
						else {
							sender.sendMessage(i.t("borders.check.countPlayersOutside", String.valueOf(playersOutside.size())));
							for(Player player : p.getBorderManager().getPlayersOutside(checkDiameter)) {
								int distance = p.getBorderManager().getDistanceToBorder(player.getLocation(), checkDiameter);
								if(distance > 150) {
									sender.sendMessage(i.t("borders.check.itemPlayerFar", player.getName()));
								}
								else if(distance > 25) {
									sender.sendMessage(i.t("borders.check.itemPlayerClose", player.getName()));
								}
								else {
									sender.sendMessage(i.t("borders.check.itemPlayerVeryClose", player.getName()));
								}
							}
						}
						
					} catch(NumberFormatException e) {
						sender.sendMessage(i.t("borders.NaN", args[2]));
					}
				}
			}
		}
		
	}
	
	
	/**
	 * This command, /t <message>, is used to send a team-message.
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doTeamMessage(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage(i.t("team.message.noConsole"));
			return;
		}
		
		if(args.length == 0) { // /t
			sender.sendMessage(i.t("team.message.usage"));
			return;
		}
		
		UHTeam team = p.getTeamManager().getTeamForPlayer((Player) sender);
		
		if(team == null) {
			sender.sendMessage(i.t("team.message.noTeam"));
			return;
		}
		
		String message = "";
		for(Integer i = 0; i < args.length; i++) {
			message += args[i] + " ";
		}
		
		for(final Player player : team.getPlayers()) {
			player.sendMessage(i.t("team.message.format", ((Player) sender).getDisplayName(), message));
		}
	}
	
	
	
	public ArrayList<String> getCommands() {
		return commands;
	}

	public ArrayList<String> getTeamCommands() {
		return teamCommands;
	}
	
	public ArrayList<String> getSpecCommands() {
		return specCommands;
	}
	
	public ArrayList<String> getBorderCommands() {
		return borderCommands;
	}
}
