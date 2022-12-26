package me.iwareq.scoreboard;

import lombok.Getter;
import me.iwareq.scoreboard.manager.ScoreboardManager;

public class ScoreboardAPI {

	@Getter
	private static ScoreboardAPI instance;

	@Getter
	private ScoreboardManager scoreboardManager;

	public void init() {
		ScoreboardAPI.instance = this;

		scoreboardManager = new ScoreboardManager();
	}
}
