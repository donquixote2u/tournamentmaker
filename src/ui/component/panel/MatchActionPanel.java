package ui.component.panel;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ui.component.dialog.MatchResultDialog;
import ui.main.TournamentViewManager;
import data.match.Match;
import data.tournament.Tournament;
import java.util.Iterator;
import ui.component.button.CourtButton;

/**
 *
 * @author bruce
 */
public class MatchActionPanel extends JPanel {
	private static final long serialVersionUID = 7657469267729125196L;
	private static final String SET_REQUESTED_TIME = "Set Scheduled Time";
	private static final String REMOVE_REQUESTED_TIME = "Remove Scheduled Time";
	private static final String SET_NEXT_AVAILABLE = "Reserve Next Available Court";
	private static final String REMOVE_NEXT_AVAILABLE = "Remove Court Reservation";
       	private static final String AUTO_ASSIGN_COURTS = "Auto-assign Courts"; // added 10/10/19 bvw
	private TournamentViewManager manager;
	private Match match;
	private JPanel timePanel;
	private JComboBox<String> hour, minute;
	private JButton timeButton;
	private JButton nextCourtButton;
	private JButton matchDialogButton;
       	private JButton courtAutoAssignButton;  // auto-assign added 10/10/19 bvw
	private MatchResultDialog matchResult;
	
    /**
     *
     * @param owner
     * @param manager
     */
    public MatchActionPanel(JFrame owner, TournamentViewManager manager) {
		super(new FlowLayout(FlowLayout.LEFT, 10, 0));
		this.manager = manager;
		setBackground(manager.getBackgroundColor());
		timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		timePanel.setBackground(manager.getBackgroundColor());
		hour = new JComboBox<String>();
		for(int i = 1; i <= 12; ++i) {
			hour.addItem(getStringValue(i));
		}
		timePanel.add(hour);
		timePanel.add(new JLabel(" : "));
		minute = new JComboBox<String>();
		for(int i = 0; i < 60; ++i) {
			minute.addItem(getStringValue(i));
		}
		timePanel.add(minute);
		timeButton = new JButton();
		timeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(match.getRequestedDate() == null) {
					Date date = new Date();
					Calendar cal = Calendar.getInstance();
					cal.setTime(date);
					int hourValue = getIntegerValue(hour.getSelectedItem());
					cal.set(Calendar.HOUR, hourValue == 12 ? 0 : hourValue);
					cal.set(Calendar.MINUTE, getIntegerValue(minute.getSelectedItem()));
					if(cal.getTime().before(date)) {
						cal.set(Calendar.AM_PM, cal.get(Calendar.AM_PM) == Calendar.AM ? Calendar.PM : Calendar.AM);
					}
					match.setRequestedDate(cal.getTime());
				}
				else {
					match.setRequestedDate(null);
				}
				MatchActionPanel.this.manager.getTournament().addUserActionMatch(match);
				MatchActionPanel.this.manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
			}
		});
		add(timePanel);
		add(timeButton);
		nextCourtButton = new JButton("");
		nextCourtButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Tournament tournament = MatchActionPanel.this.manager.getTournament();
				tournament.setMatchOrder(match, match.getNextAvailableCourtOrder() > 0 ? 0 : tournament.getNextFreeCourt());
				MatchActionPanel.this.manager.getTournament().addUserActionMatch(match);
				MatchActionPanel.this.manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
			}
		});
                courtAutoAssignButton = new JButton(AUTO_ASSIGN_COURTS);
		courtAutoAssignButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
                            Tournament tournament = MatchActionPanel.this.manager.getTournament();
                            for (Iterator<Match> it = tournament.getMatches().iterator(); it.hasNext();) {
                                Match current = it.next();
                                System.out.println("Considering "+current.getIndex());
                                for(Component comp : MatchActionPanel.this.manager.courtsPanel.getComponents()) {
                                    CourtButton courtButton = (CourtButton) comp;
                                    if(!courtButton.isAvailableCourt() || !courtButton.isUsableCourt()) {
                                        continue;
                                    }
                                    if(MatchActionPanel.this.manager.addMatchToCourtButton(current, courtButton)) {
                                        System.out.println(" Court "+courtButton.getCourtId()+" assigned");
                                        break;
                                    }
                                }
                            }
                        }    
		});
		matchResult = new MatchResultDialog(owner);
		matchDialogButton = new JButton("Set Result");
		matchDialogButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				matchResult.setMatch(match);
				matchResult.setVisible(true);
				Set<Match> result = match.finish();
				if(result != null) {
					match.end();
					MatchActionPanel.this.manager.getTournament().removeMatch(match);
					MatchActionPanel.this.manager.getTournament().addCompletedMatch(match);
					MatchActionPanel.this.manager.getTournament().addMatches(result);
				}
				MatchActionPanel.this.manager.getTournament().addUserActionMatch(match);
				MatchActionPanel.this.manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
			}
		});
		add(matchDialogButton);
		add(nextCourtButton);
                add(courtAutoAssignButton);
		setMatch(null);
	}
	
	public void setMatch(Match match) {
		this.match = match;
		for(Component component : getComponents()) {
			component.setVisible(match != null);
		}
		if(match == null) {
			return;
		}
		update();
	}
	
	public void update() {
		if(match == null) {
			return;
		}
		boolean editable = match.getTeam1() != null && match.getTeam2() != null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(match.getEstimatedDate() != null ?  match.getEstimatedDate() : new Date());
		hour.setSelectedItem(getStringValue(cal.get(Calendar.HOUR_OF_DAY) - (cal.get(Calendar.HOUR_OF_DAY) > 12 ? 12 : 0)));
		minute.setSelectedItem(getStringValue(cal.get(Calendar.MINUTE)));
		timePanel.setVisible(match.getRequestedDate() == null);
		timeButton.setText(match.getRequestedDate() == null ? SET_REQUESTED_TIME : REMOVE_REQUESTED_TIME);
		boolean canReserve = manager.getTournament().getNextFreeCourt() > 0;
		nextCourtButton.setText(match.getNextAvailableCourtOrder() > 0 ? REMOVE_NEXT_AVAILABLE : SET_NEXT_AVAILABLE);
		nextCourtButton.setVisible(match.getNextAvailableCourtOrder() > 0 || editable);
		nextCourtButton.setEnabled(match.getNextAvailableCourtOrder() > 0 || canReserve);
		nextCourtButton.setToolTipText(match.getNextAvailableCourtOrder() > 0 || canReserve ? null : "Only 9 courts can be reserved at a time.");
	}
	
	private String getStringValue(int value) {
		return (value < 10 ? "0" : "") + value;
	}
	
	private int getIntegerValue(Object value) {
		if(!(value instanceof String)) {
			return 1;
		}
		try {
			return Integer.parseInt((String) value);
		}
		catch(NumberFormatException e) {
			return 1;
		}
	}
}
