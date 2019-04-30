/*
 * autogenerate doubles pairs .
 */
package ui.component.dialog;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import data.event.Event;
import data.player.Player;
import data.team.Team;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.WindowConstants;
import ui.component.label.MessageLabel;
import static ui.main.TournamentUI.NEW_DISPLAY_TEXT;
import ui.main.TournamentViewManager;
import ui.util.GenericUtils;
/**
 *
 * @author bruce
 */
public class AutoPairDialog extends JDialog {
  	private static final long serialVersionUID = 2564561655115801740L;
       	private TournamentViewManager tournamentViewManager;
        private String teamType; 
        private int teamCount;
        
        public AutoPairDialog(JFrame owner, TournamentViewManager tournamentViewManager) {
                
		final JDialog dialog = new JDialog(this, "New Team");
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setResizable(false);
		dialog.getContentPane().setLayout(new BorderLayout());
		final MessageLabel message = new MessageLabel();
		dialog.add(message, BorderLayout.PAGE_START);
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("Type of Team"), GenericUtils.createGridBagConstraint(0, 0, 0.3));
		final JComboBox<String> type = new JComboBox<String>();
		panel.add(type, GenericUtils.createGridBagConstraint(1, 0, 0.7));
		List<String> list = tournamentViewManager.getTournament().getTeamTypes();
		Collections.sort(list);
		list.add(NEW_DISPLAY_TEXT);
		for(String teamTypeCode : list) {
 			type.addItem(teamTypeCode);
		}
                type.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
			setTeamType((String) type.getSelectedItem());
                        // setTeamType(teamType);
                        }
                 }); 
                teamCount=0;
                setTeamType(type.getSelectedItem().toString());
                if(!(teamType==null)) {teamCount=tournamentViewManager.getTournament().getTeamByType(teamType).getNumberOfPlayers();}   
                message.success("Teamcount "+teamCount);    
                dialog.getContentPane().add(panel, BorderLayout.CENTER);
                // build list of players eligible for pairing
                List <Player> players= new ArrayList<Player>();
                int i=0; // index 
		for(Player player : tournamentViewManager.getTournament().getPlayers()) {
			if(player != null && player.isCheckedIn()) {
                                player.setInGame(false);
                                players.add(i,player);i++;
			}
		}
		JPanel buttons = new JPanel(new FlowLayout());
		JButton ok = new JButton("Make Teams");
		ok.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent event) {
                        // for each player in list, traverse the list until eligible partner found     
                        int indx=0;
                        while (indx < players.size()) { 
                            Player player1 = players.get(indx);
                            if(AlreadyInTeam(tournamentViewManager,player1)) {
                                indx++;
                                continue; }
                            int indy=0;
                            while(indy < players.size()) {
                                if(indy==indx) {
                                    indy++;
                                    continue;} // dont match with self!
                                Player player2 = players.get(indy);
                                Team team = tournamentViewManager.getTournament().getTeamByType(teamType).newInstance();
                                team.setPlayer(0, player1);
                                team.setPlayer(1, player2);
                                if((team.isValid()) && !(AlreadyInTeam(tournamentViewManager,player2))) { 
                                    tournamentViewManager.getTournament().addTeam(team);
                                    players.remove(indy);
                                    players.remove(indx);
                                    break;
                                    }
                                else {
                                    indy++; 
                                    }
                                }
                                indx++;
			    }
                        teamCount=tournamentViewManager.getTournament().getTeamByType(teamType).getNumberOfPlayers();
                        message.success("Teamcount "+teamCount);
                        }
                });
		buttons.add(ok);
		final JButton cancel = new JButton("Done");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dialog.dispose();
					tournamentViewManager.switchToTab(TournamentViewManager.TEAMS_TAB, true);
			}
		});
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				cancel.doClick();
			}
		});
		buttons.add(cancel);
		dialog.getContentPane().add(buttons, BorderLayout.PAGE_END);
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
        }
        
        public final String getTeamType() {
		return this.teamType;
	}
	
        public final void setTeamType(String teamType) {
		this.teamType=teamType;
	}
        
        private boolean AlreadyInTeam (TournamentViewManager tournamentViewManager, Player player) {        
                // new test added for players being already in a team of same type  22/4/19 bvw
                List<Team> teams= tournamentViewManager.getTournament().getTeams();
                    for(Team eachTeam : teams) {
                        if(eachTeam.getTeamType().equals(this.getTeamType())) {
                            for(Player anyplayer : eachTeam.getPlayers()) {
                                  if(player==anyplayer) {
                                        return true;   
                                    }
                            }
                        }
                }
        return false;            
        }
        
	}