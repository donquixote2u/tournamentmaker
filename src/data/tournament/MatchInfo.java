package data.tournament;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MatchInfo {
	private BigDecimal totalTime;
	private int numberOfMatches;
	
	public MatchInfo() {
		totalTime = new BigDecimal("0");
		numberOfMatches = 0;
	}
	
	public void addTime(long time) {
		++numberOfMatches;
		totalTime = totalTime.add(new BigDecimal(Long.toString(time)));
	}
	
	public void removeTime(long time) {
		--numberOfMatches;
		totalTime = totalTime.add(new BigDecimal("-" + Long.toString(time)));
		if(numberOfMatches <= 0) {
			numberOfMatches = 0;
			totalTime = new BigDecimal("0");
		}
	}
	
	public long getTime() {
		return totalTime.divide(new BigDecimal(Integer.toString(Math.max(1, numberOfMatches))),  0, RoundingMode.HALF_UP).longValue();
	}
	
	public long average(MatchInfo matchInfo) {
		if(matchInfo == null || matchInfo.getTime() == 0) {
			return getTime();
		}
		return (new BigDecimal(Long.toString(getTime() + matchInfo.getTime()))).divide(new BigDecimal("2"), 0, RoundingMode.HALF_UP).longValue();
	}
}
