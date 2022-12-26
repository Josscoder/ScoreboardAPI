package me.iwareq.scoreboard;

import cn.nukkit.plugin.PluginBase;
import lombok.Getter;
import me.iwareq.scoreboard.manager.ScoreboardManager;

public class ScoreboardAPI {

	@Getter
	private static ScoreboardAPI instance;

	@Getter
	private ScoreboardManager scoreboardManager;

	public void onEnable() {
		ScoreboardAPI.instance = this;

		this.scoreboardManager = new ScoreboardManager();
	}
}
