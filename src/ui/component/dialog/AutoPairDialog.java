/*
 * autogenerate doubles pairs .
 */
package ui.component.dialog;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import data.player.Player;
import data.team.Team;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
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
        private int playerCount;
        private int gradeAvg;
        
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
                setTeamType(type.getSelectedItem().toString());
                dialog.getContentPane().add(panel, BorderLayout.CENTER);
                // build list of players eligible for pairing
                List <Player> players= new ArrayList<Player>();
                playerCount=0; // index
                int gradeSum=0;  // total of all player grades
		for(Player player : tournamentViewManager.getTournament().getPlayers()) {
			if(player != null && player.isCheckedIn()) {
                                Player playerData = new Player(player.getName(),false); 
                                playerData.setIsMale(player.isMale());
                                playerData.setCheckedIn(player.isCheckedIn());
                                int calcLevel = Integer.valueOf(player.getLevel());
                                if(player.isMale()) {calcLevel=calcLevel-2;}
                                playerData.setLevel(Integer.toString(calcLevel));
                                players.add(playerCount,playerData);playerCount++;
                                gradeSum=gradeSum+calcLevel;
			}
		}
                gradeAvg=gradeSum / playerCount;
                Collections.shuffle(players);               // randomise players for matching
                message.success("Grade Avg = "+ gradeAvg);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton ok = new JButton("Make Teams");
		ok.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent event) {
                        // for each player in list, traverse the list until best eligible partner found     
                        for (Player player1 : players) { 
                            System.out.println(player1.getName() + " matching:");
                            if(AlreadyInTeam(tournamentViewManager,player1)) {
                                continue; }
                            int ylim=players.size();    // set loop control to size of list
                            Random rand = new Random();
                            int y = rand.nextInt(ylim); // randomize start point
                            int ycount=0;                          // use count to control loop 
                            Player pairCandidate = new Player(player1.getName(),true);        // dummy instance
                            pairCandidate.setLevel("99");
                            Team team = tournamentViewManager.getTournament().getTeamByType(teamType).newInstance();
                            while(ycount<(ylim-1)) {
                                ycount++;
                                y++; if(!(y<ylim)) {y=0;}              // if over top of list, back to start
                                Player player2 = players.get(y);
                                System.out.print("considering " + player2.getName());
                                if((player2.getName()==player1.getName()) || (AlreadyInTeam(tournamentViewManager,player2)) ) {
                                    System.out.println(" invalid player");
                                    continue;} // dont match with self or an already matched player!
                                team.setPlayer(0, player1);
                                team.setPlayer(1, player2);
                                if(!(team.isValid())) {
                                    System.out.println(" invalid team");
                                    continue;}
                                pairCandidate=checkGradeDiffs(player1,player2,pairCandidate);
                                }
                                if(!(player1.getName()==pairCandidate.getName()) ) { 
                                    team.setPlayer(1, pairCandidate);
                                    tournamentViewManager.getTournament().addTeam(team);
                                    System.out.println(player1.getName() + " matched with " + pairCandidate.getName());
                                    }
			    }
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
                                  if(player.getName()==anyplayer.getName()) {
                                        return true;   
                                    }
                            }
                        }
                }
        return false;            
        }
        
        private Player checkGradeDiffs (Player player1, Player player2, Player pairCandidate ) {
        int gradeDiff1 = abs((this.gradeAvg*2)-(Integer.valueOf(player1.getLevel())+Integer.valueOf(pairCandidate.getLevel())));  
        int gradeDiff2 = abs((this.gradeAvg*2)-(Integer.valueOf(player1.getLevel())+Integer.valueOf(player2.getLevel())));   
        System.out.println( " this diff "+gradeDiff2+"; candidate diff "+gradeDiff1);
        if(gradeDiff2<gradeDiff1) {return player2; } else { return pairCandidate; }
        }
       
	}