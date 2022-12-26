package me.iwareq.scoreboard;

import cn.nukkit.Player;
import cn.nukkit.Server;
import lombok.Getter;
import lombok.Setter;
import me.iwareq.scoreboard.line.ScoreboardLine;
import me.iwareq.scoreboard.manager.ScoreboardManager;
import me.iwareq.scoreboard.packet.RemoveObjectivePacket;
import me.iwareq.scoreboard.packet.SetDisplayObjectivePacket;
import me.iwareq.scoreboard.packet.SetScorePacket;
import me.iwareq.scoreboard.packet.data.DisplaySlot;
import me.iwareq.scoreboard.packet.data.ScorerInfo;
import me.iwareq.scoreboard.packet.data.SortOrder;
import me.iwareq.scoreboard.updater.ScoreboardUpdater;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Scoreboard {

	@Setter
	private String displayName;

	@Setter
	private DisplaySlot displaySlot;

	@Setter
	private SortOrder sortOrder;
	private final ScoreboardManager manager;

	@Getter
	private final Set<Player> viewers = new HashSet<>();
	private final Map<Integer, ScoreboardLine> lines = new HashMap<>();

	private Consumer<Player> consumer = (p) -> {};
	private int lastIndex;

	public Scoreboard() {
		this("");
	}

	public Scoreboard(String displayName) {
		this(displayName, DisplaySlot.SIDEBAR);
	}

	public Scoreboard(String displayName, DisplaySlot displaySlot) {
		this(displayName, displaySlot, SortOrder.ASCENDING);
	}

	public Scoreboard(String displayName, DisplaySlot displaySlot, SortOrder sortOrder) {
		this(displayName, displaySlot, sortOrder, 20);
	}

	public Scoreboard(String displayName, DisplaySlot displaySlot, SortOrder sortOrder, int updateTime) {
		this.displayName = displayName;
		this.displaySlot = displaySlot;
		this.sortOrder = sortOrder;
		this.manager = ScoreboardAPI.getInstance().getScoreboardManager();

		Server.getInstance().getScheduler().scheduleRepeatingTask(new ScoreboardUpdater(this), updateTime, true);
	}

	public void setHandler(Consumer<Player> consumer) {
		this.consumer = consumer;
	}

	public void setLine(int index, String text) {
		checkLineIndex(index);

		this.lastIndex = index;

		ScoreboardLine line = new ScoreboardLine(this, text);
		this.lines.put(index, line);
	}

	public void addLine(String text) {
		if (this.lastIndex != 15) {
			this.lastIndex++;

			this.setLine(this.lastIndex, text);
		}
	}

	private void checkLineIndex(int index) {
		if (index < 1 || index > 15) {
			throw new IllegalArgumentException("The line index value should be from 1 to 15, your index: " + index);
		}
	}

	public void onUpdate() {
		this.lines.clear();
		this.lastIndex = 0;
		this.viewers.removeIf(viewer -> {
			boolean remove = !viewer.isConnected() || !viewer.isOnline();
			if (remove) {
				this.manager.removeScoreboard(viewer);
			}

			return remove;
		});

		this.viewers.forEach(viewer -> {
			this.hide(viewer, false);
			this.show(viewer, false);
		});
	}

	public void show(Player player) {
		this.show(player, true);
	}

	private void show(Player player, boolean add) {
		if (!add || this.viewers.add(player)) {
			this.consumer.accept(player);

			SetDisplayObjectivePacket objectivePacket = new SetDisplayObjectivePacket();
			objectivePacket.setDisplaySlot(this.displaySlot);
			objectivePacket.setObjectiveId("objective");
			objectivePacket.setDisplayName(this.displayName);
			// dummy is the only criterion supported. As such, score can only be changed by commands.
			objectivePacket.setCriteria("dummy");
			objectivePacket.setSortOrder(sortOrder);

			player.dataPacket(objectivePacket);

			SetScorePacket scorePacket = new SetScorePacket(SetScorePacket.Action.SET);
			this.lines.forEach((index, line) ->
					scorePacket.getInfos().add(
							new ScorerInfo(index, "objective", index, line.getText())
					)
			);

			player.dataPacket(scorePacket);

			if (add) {
				this.manager.setScoreboard(player, this);
			}
		}
	}

	public void hide(Player player) {
		this.hide(player, true);
	}

	private void hide(Player player, boolean remove) {
		if (!remove || this.viewers.remove(player)) {
			RemoveObjectivePacket packet = new RemoveObjectivePacket();
			packet.setObjectiveId("objective");

			player.dataPacket(packet);

			if (remove) {
				this.manager.removeScoreboard(player);
			}
		}
	}
}
