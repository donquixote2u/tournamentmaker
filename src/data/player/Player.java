package data.player;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import data.tournament.TournamentUtils;

public class Player implements Serializable {
	private static final long serialVersionUID = -96926304857187476L;
	private transient Date estimatedDate;
	private String name;
	private String address, email, phoneNumber;
	private boolean isMale;
	private String level, membershipNumber, club;
	private double amountDue, amountPaid;
	private boolean checkedIn, inGame;
	private Date dateOfBirth;
	private Date oldMatchTime, lastMatchTime, requestedDelay;
	private Set<String> events;
	
	public Player(String name, boolean isMale) {
		this.name = name;
		this.isMale = isMale;
		events = new HashSet<String>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public boolean isMale() {
		return isMale;
	}

	public void setIsMale(boolean isMale) {
		this.isMale = isMale;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getMembershipNumber() {
		return membershipNumber;
	}

	public void setMembershipNumber(String membershipNumber) {
		this.membershipNumber = membershipNumber;
	}

	public double getAmountDue() {
		return amountDue;
	}

	public void setAmountDue(double amountDue) {
		this.amountDue = amountDue;
	}

	public double getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(double amountPaid) {
		this.amountPaid = amountPaid;
	}
	
	public boolean paidInFull() {
		return (new BigDecimal(String.valueOf(amountPaid))).compareTo(new BigDecimal(String.valueOf(amountDue))) == 0;
	}

	public boolean isCheckedIn() {
		return checkedIn;
	}

	public void setCheckedIn(boolean checkedIn) {
		this.checkedIn = checkedIn;
	}
	
	public boolean isInGame() {
		return inGame;
	}
	
	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}

	public Date getLastMatchTime() {
		return lastMatchTime;
	}

	public void setLastMatchTime(Date lastMatchTime) {
		if(this.lastMatchTime != null) {
			oldMatchTime = this.lastMatchTime;
		}
		this.lastMatchTime = lastMatchTime;
	}
	
	public void undoSetLastMatchTime() {
		lastMatchTime = oldMatchTime;
	}

	public Date getRequestedDelay() {
		return requestedDelay;
	}

	public void setRequestedDelay(Date requestedDelay) {
		this.requestedDelay = requestedDelay;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Set<String> getEvents() {
		return Collections.unmodifiableSet(events);
	}

	public void setEvents(Set<String> events) {
		this.events.clear();
		if(events != null) {
			this.events.addAll(events);
		}
	}

	public String getClub() {
		return club;
	}

	public void setClub(String club) {
		this.club = club;
	}
	
	public void setEstimatedDate(Date estimatedDate) {
		if(this.estimatedDate == null || TournamentUtils.compareDates(this.estimatedDate, estimatedDate) > 0) {
			this.estimatedDate = estimatedDate;
		}
	}
	
	public Date getEstimatedDate() {
		return estimatedDate;
	}
	
	public String toString() {
		return name;
	}
}
