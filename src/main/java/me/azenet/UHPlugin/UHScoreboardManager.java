package me.azenet.UHPlugin;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import me.azenet.UHPlugin.i18n.I18n;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criterias;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class UHScoreboardManager {
	
	private UHPlugin p = null;
	private I18n i = null;
	private UHGameManager gm = null;
	private Scoreboard sb = null;
	private Objective objective = null;
	
	// Old values, to be able to update the minimum.
	// Initialized to -1 to force an update at the first launch.
	private Integer oldEpisode = -1;
	private Integer oldAlivePlayersCount = -1;
	private Integer oldAliveTeamsCount = -1;
	private Integer oldMinutes = 0;
	private Integer oldSeconds = 0;
	
	// Static values
	private String objectiveName = "UHPlugin";
	private NumberFormat formatter = new DecimalFormat("00");
	
	
	/**
	 * Constructor.
	 * Initialize the scoreboard.
	 * 
	 * @param plugin
	 */
	public UHScoreboardManager(UHPlugin plugin) {
		this.p  = plugin;
		this.i  = p.getI18n();
		this.gm = p.getGameManager();
		this.sb = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
		
		
		// Initialization of the scoreboard (match info in the sidebar)
		if(p.getConfig().getBoolean("scoreboard.enabled")) {
			try {
				this.sb.clearSlot(DisplaySlot.SIDEBAR);
				this.sb.getObjective(objectiveName).unregister();
			} catch(NullPointerException | IllegalArgumentException e) { }
			
			this.objective = this.sb.registerNewObjective(objectiveName, "dummy");
			this.objective.setDisplayName(getScoreboardName());
			this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			
			// The "space" score needs to be set only one time, and only if the episodes/timer are enabled.
			if(p.getConfig().getBoolean("episodes.enabled") && p.getConfig().getBoolean("scoreboard.timer")) {
				this.objective.getScore("").setScore(2);
			}
			
			updateScoreboard();
		}
		
		// Initialization of the scoreboard (health in players' list)
		if(p.getConfig().getBoolean("scoreboard.health")) {
			Objective healthObjective = this.sb.registerNewObjective("Health", Criterias.HEALTH);
			healthObjective.setDisplayName("Health");
			healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
			
			// Sometime the health is initialized to 0. This is used to fix this.
			updateHealthScore();
		}
		else {
			this.sb.clearSlot(DisplaySlot.PLAYER_LIST); // Just in case
		}
	}
	
	public void updateScoreboard() {
		if(p.getConfig().getBoolean("scoreboard.enabled")) {
			Integer episode = gm.getEpisode();
			Integer alivePlayersCount = gm.getAlivePlayersCount();
			Integer aliveTeamsCount = gm.getAliveTeamsCount();
			Integer minutesLeft = gm.getMinutesLeft();
			Integer secondsLeft = gm.getSecondsLeft();
			
			if(!episode.equals(oldEpisode) && p.getConfig().getBoolean("episodes.enabled") && p.getConfig().getBoolean("scoreboard.episode")) {
				sb.resetScores(getText("episode", oldEpisode));
				objective.getScore(getText("episode", episode)).setScore(5);
				oldEpisode = episode;
			}
			
			if(!alivePlayersCount.equals(oldAlivePlayersCount) && p.getConfig().getBoolean("scoreboard.players")) {
				sb.resetScores(getText("players", oldAlivePlayersCount));
				objective.getScore(getText("players", alivePlayersCount)).setScore(4);
				oldAlivePlayersCount = alivePlayersCount;
			}
			
			// This is displayed when the game is running to avoid a special case used to remove it
			// if the game is without teams.
			if(gm.isGameRunning() && gm.isGameWithTeams() && !aliveTeamsCount.equals(oldAliveTeamsCount) && p.getConfig().getBoolean("scoreboard.teams")) {
				sb.resetScores(getText("teams", oldAliveTeamsCount));
				objective.getScore(getText("teams", aliveTeamsCount)).setScore(3);
				oldAliveTeamsCount = aliveTeamsCount;
			}
			
			// The timer score is reset every time.
			if(p.getConfig().getBoolean("episodes.enabled") && p.getConfig().getBoolean("scoreboard.timer")) {
				sb.resetScores(getTimerText(oldMinutes, oldSeconds));
				objective.getScore(getTimerText(minutesLeft, secondsLeft)).setScore(1);
				oldMinutes = minutesLeft;
				oldSeconds = secondsLeft;
			}
		}
	}
	
	/**
	 * Returns the text displayed in the scoreboard.
	 * 
	 * @param textType Either "episode", "players" or "teams".
	 * @param arg Respectively, the episode number, the players count and the teams count.
	 * @return The text.
	 * @throws IllegalArgumentException if the textType is not one of the listed types.
	 */
	private String getText(String textType, Integer arg) {
		switch(textType) {
			case "episode":
				return i.t("scoreboard.episode", arg.toString());
			case "players":
				return i.t("scoreboard.players", arg.toString());
			case "teams":
				return i.t("scoreboard.teams", arg.toString());
			default:
				throw new IllegalArgumentException("Incorrect text type, see javadoc");
		}
	}
	
	/**
	 * Returns the text displayed in the scoreboard, for the timer.
	 * 
	 * @param minutes The minute in the timer
	 * @param seconds The second in the timer
	 * @return The text of the timer
	 */
	private String getTimerText(Integer minutes, Integer seconds) {
		return i.t("scoreboard.timer", formatter.format(minutes), formatter.format(seconds));
	}
	
	public void updateHealthScore() {
		for(final Player player : p.getServer().getOnlinePlayers()) {
			updateHealthScore(player);
		}
	}
	
	public void updateHealthScore(final Player player) {
		if(player.getHealth() != 1D) {
			player.setHealth(player.getHealth() - 1);
			
			Bukkit.getScheduler().runTaskLater(p, new BukkitRunnable() {
				@Override
				public void run() {
					player.setHealth(player.getHealth() + 1);
				}
			}, 1L);
		}
	}
	
	/**
	 * Tell the player's client to use this scoreboard.
	 * 
	 * @param p The player.
	 */
	public void setScoreboardForPlayer(Player p) {
		p.setScoreboard(sb);
	}
	
	public String getScoreboardName() {
		String s = p.getConfig().getString("scoreboard.title", "Kill the Patrick");
		return s.substring(0, Math.min(s.length(), 16));
	}
	
	public Scoreboard getScoreboard() {
		return sb;
	}
}
