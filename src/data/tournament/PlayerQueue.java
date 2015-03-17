package data.tournament;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;

import data.player.Player;
import ui.util.Pair;

/**
 * This class is used to hold the date to players mapping used in sortMatches by TournamentUtils.java. It wraps a sorted ArrayList
 * and provides convenience functions for data manipulation. Dates are sorted using TournamentUtils.java's compareDates method.
 * @author Jason Young
 *
 */
public class PlayerQueue {
	private ArrayList<Pair<Date, LinkedHashSet<Player>>> data;
	
	public PlayerQueue() {
		data = new ArrayList<Pair<Date, LinkedHashSet<Player>>>();
	}
	
	public Pair<Date, LinkedHashSet<Player>> get(int index) {
		if(index < 0 || index >= data.size()) {
			return null;
		}
		return data.get(index);
	}
	
	public Pair<Date, LinkedHashSet<Player>> get(Date date) {
		Pair<Date, LinkedHashSet<Player>> value = get(indexOf(date));
		if(value != null && TournamentUtils.compareDates(date, value.getKey()) == 0) {
			return value;
		}
		return null;
	}
	
	public Pair<Date, LinkedHashSet<Player>> poll() {
		if(data.isEmpty()) {
			return null;
		}
		return data.remove(0);
	}
	
	public boolean isEmpty() {
		return data.isEmpty();
	}
	
	public int size() {
		return data.size();
	}
	
	public void add(Date date, Player player) {
		int index = indexOf(date);
		if(index < data.size() && TournamentUtils.compareDates(date, data.get(index).getKey()) == 0) {
			data.get(index).getValue().add(player);
		}
		else {
			LinkedHashSet<Player> players = new LinkedHashSet<Player>();
			players.add(player);
			data.add(index, new Pair<Date, LinkedHashSet<Player>>(date, players));
		}
	}
	
	public boolean remove(Date date) {
		int index = indexOf(date);
		if(TournamentUtils.compareDates(date, data.get(index).getKey()) == 0) {
			data.remove(index);
			return true;
		}
		return false;
	}
	
	private int indexOf(Date date) {
		int start = 0;
		int end = data.size();
		while(start != end) {
			int mid = start + (end - start) / 2;
			int compare = TournamentUtils.compareDates(date, data.get(mid).getKey());
			if(compare > 0) {
				start = mid + 1;
			}
			else if(compare < 0) {
				end = mid;
			}
			else {
				start = mid;
				end = mid;
			}
		}
		return start;
	}
}
