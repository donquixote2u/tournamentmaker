package data.team.modifier;

import java.util.Calendar;
import java.util.Date;

import data.player.Player;
import ui.util.GenericUtils;

@CreatableTeamModifier(displayName = "Min Age", parameters = {"Age", "Date (MM/DD/YYYY)"})
public class MinAgeByDateModifier extends TeamModifier {
	private static final long serialVersionUID = -6097932164672409519L;
	private String age, date;
	private int playerAge;
	private Date endDate;
	
	public MinAgeByDateModifier(String[] args) {
		if(args == null || args.length != 2) {
			throw new RuntimeException("Min Age modifier needs to have 2 parameters");
		}
		String age = args[0];
		String date = args[1];
		try {
			playerAge = Integer.parseInt(age);
		}
		catch(Exception e) {
			throw new RuntimeException("Min Age - invalid age");
		}
		if(playerAge < 0) {
			throw new RuntimeException("Min Age - invalid age");
		}
		try {
			endDate = GenericUtils.stringToDate(date, "MM/dd/yyyy");
		}
		catch(Exception e) {
			throw new RuntimeException("Min Age - invalid date");
		}
		if(endDate == null) {
			throw new RuntimeException("Min Age - invalid date");
		}
		this.age = age;
		this.date = date;
	}
	
	public String[] getParameters() {
		return new String[]{age, date};
	}
	
	public boolean isValidPlayer(Player player) {
		if(player == null) {
			return false;
		}
		Date birthday = player.getDateOfBirth();
		if(birthday == null) {
			return false;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(birthday);
		cal.add(Calendar.YEAR, playerAge);
		return endDate.after(cal.getTime()) || endDate.equals(cal.getTime());
	}
}
