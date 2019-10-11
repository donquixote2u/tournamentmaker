package ui.component.button;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import ui.component.dialog.CourtDialog;
import ui.main.TournamentViewManager;
import ui.util.GenericUtils;
import data.match.Match;
import data.player.Player;
import data.team.Team;
import data.tournament.Court;
import data.tournament.Tournament;

public class CourtButton extends JPanel {
	private static final long serialVersionUID = -4304082780647976590L;
	public static final String MATCH_TEXT = CourtButton.class.getName() + ".MatchString." + (new Date()).getTime();
	private static final Color GREEN = new Color(0x00AE0E);
	private static final Color YELLOW = new Color(0xFFE97F);
	private static final Color RED = new Color(0xE10000);
	private static final Color BLUE = new Color(0x0026F8);
	private static final Color GRAY = new Color(0x838383);
	private TournamentViewManager manager;
	private JLabel time;
	private Court court;             
	private JButton courtButton;
	private boolean hasDrop, startedDrag;
	private DragSource dragSource;
	
	@SuppressWarnings("serial")
	public CourtButton(Court court, final TournamentViewManager manager, final CourtDialog courtDialog) {
		super(new BorderLayout());
		if(court == null) {
			throw new IllegalArgumentException("court can not be null");
		}
		this.court = court;
		this.manager = manager;
		JCheckBox usable = new JCheckBox("Available");
		usable.setSelected(court.isUsable());
		usable.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				CourtButton.this.court.setUsable(!CourtButton.this.court.isUsable());
				updateCourtStatus();
				manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
			}
		});
		add(usable, BorderLayout.PAGE_START);
		time = new JLabel("");
		time.setHorizontalAlignment(SwingConstants.CENTER);
		add(time, BorderLayout.PAGE_END);
		courtButton = new JButton(court.getId()) {
			protected void paintComponent(Graphics g) {
				Match match = CourtButton.this.court.getCurrentMatch();
				if(match == null) {
					super.paintComponent(g);
					return;
				}
				float fontSize = 14.0f;
				g.setFont(getFont().deriveFont(Font.PLAIN, fontSize));
				super.paintComponent(g);
				g.setColor(Color.BLACK);
				int textPaddingWidth = 10;
				int textPaddingHeight = 5;
				FontMetrics metrics = g.getFontMetrics(g.getFont());
				int fontHeight = metrics.getHeight();
				String teamName = match.getTeam1().getName();
				if(metrics.stringWidth(teamName) >= getWidth() - (2 * textPaddingWidth)) {
					String firstName = generateFirstName(match.getTeam1());
					if(firstName.length() < teamName.length()) {
						teamName = firstName;
					}
				}
				g.drawString(teamName, textPaddingWidth, fontHeight + textPaddingHeight);
				teamName = match.getTeam2().getName();
				if(metrics.stringWidth(teamName) >= getWidth() - (2 * textPaddingWidth)) {
					String firstName = generateFirstName(match.getTeam2());
					if(firstName.length() < teamName.length()) {
						teamName = firstName;
					}
				}
				g.drawString(teamName, textPaddingWidth, getHeight() + textPaddingHeight - fontHeight);
			}
		};
		courtButton.setFont(courtButton.getFont().deriveFont(Font.BOLD, 30.0f));
		courtButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				courtDialog.show(CourtButton.this.court);
			}
		});
		add(courtButton, BorderLayout.CENTER);
		setPreferredSize(new Dimension(200, 125));
		updateCourtStatus();
		setDropTarget(new DropTarget(courtButton, new DropTargetAdapter() {
			public void dragEnter(DropTargetDragEvent event) {
				boolean isMatchDrop = isMatchDrop(event);
				if(isMatchDrop) {
					Point point = courtButton.getLocation();
					int width = courtButton.getWidth();
					scrollRectToVisible(new Rectangle(point.x - (width / 4), point.y, (int) (width * 1.5), 1));
				}
				if(isMatchDrop && manager.canStartSelectedMatch(CourtButton.this)) {
					hasDrop = true;
				}
				else {
					hasDrop = false;
					event.rejectDrag();
				}
				updateCourtStatus();
			}

			public void dragExit(DropTargetEvent event) {
				hasDrop = false;
				updateCourtStatus();
			}

			public void drop(DropTargetDropEvent event) {
				if(isMatchDrop(event) && manager.addSelectedMatchToCourtButton(CourtButton.this)) {
					event.acceptDrop(DnDConstants.ACTION_COPY);
					event.dropComplete(true);
				}
				else {
					event.rejectDrop();
				}
				hasDrop = false;
				updateCourtStatus();
			}
			
			private boolean isMatchDrop(DropTargetDragEvent event) {
				try {
					return MATCH_TEXT.equals(event.getTransferable().getTransferData(DataFlavor.stringFlavor));
				}
				catch(Exception e) {}
				return false;
			}
			
			private boolean isMatchDrop(DropTargetDropEvent event) {
				try {
					return MATCH_TEXT.equals(event.getTransferable().getTransferData(DataFlavor.stringFlavor));
				}
				catch(Exception e) {}
				return false;
			}
		}));
		dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(courtButton, DnDConstants.ACTION_COPY, new DragGestureListener() {
			public void dragGestureRecognized(DragGestureEvent event) {
				courtButton.setEnabled(false);
				startedDrag = true;
				updateCourtStatus();
				dragSource.startDrag(event, DragSource.DefaultCopyNoDrop, new StringSelection(MATCH_TEXT), new DragSourceAdapter() {
					public void dragDropEnd(DragSourceDropEvent event) {
						// this is so the default button action doesn't fire
						courtButton.setEnabled(true);
						startedDrag = false;
						manager.refreshCurrentTab();
					}
					
					public void dragEnter(DragSourceDragEvent event) {
						if(event.getTargetActions() == DnDConstants.ACTION_NONE) {
							event.getDragSourceContext().setCursor(DragSource.DefaultCopyNoDrop);
						}
						else {
							event.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
						}
					}
					
					public void dragExit(DragSourceEvent event) {
						event.getDragSourceContext().setCursor(DragSource.DefaultCopyNoDrop);
					}
				});
			}
		});
	}
	
	public synchronized void updateCourtStatus() {
		String timeString = "";
		Match match = court.getCurrentMatch();
		if(match != null && match.getStart() != null) {
			timeString = GenericUtils.getDuration(match.getStart(), new Date(System.currentTimeMillis()));
		}
		Tournament tournament = manager.getTournament();
		long estimatedDuration = 0;
		if(tournament != null) {
			estimatedDuration = tournament.getEstimatedDuration(match);
		}
		if(estimatedDuration <= 0 || timeString.isEmpty()) {
			time.setText(GenericUtils.html(GenericUtils.color(timeString, "green")));
		}
		else {
			long estimatedEndTime = match.getStart().getTime() + estimatedDuration;
			timeString += " / " + GenericUtils.getDuration(match.getStart(), new Date(estimatedEndTime));
			if(estimatedEndTime >= System.currentTimeMillis()) {
				time.setText(GenericUtils.html(GenericUtils.color(timeString, "green")));
			}
			else {
				time.setText(GenericUtils.html(GenericUtils.color(timeString, "red")));
			}
		}
		if(startedDrag && court.isUsable()) {
			courtButton.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, GRAY));
			courtButton.setToolTipText(null);
		}
		else if(hasDrop) {
			courtButton.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, BLUE));
			courtButton.setToolTipText(null);
		}
		else if(!court.isUsable()) {
			courtButton.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, RED));
			courtButton.setToolTipText("Court Unavailable");
		}
		else if(!court.getPreviousMatches().isEmpty()) {
			courtButton.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, YELLOW));
			courtButton.setToolTipText("Finished Games Pending");
		}
		else if(!court.isAvailable()) {
			courtButton.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, Color.BLACK));
			courtButton.setToolTipText("Game In Progress");
		}
		else {
			courtButton.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, GREEN));
			courtButton.setToolTipText("Court Available");
		}
	}
	
	public boolean addMatch(Match match) {
		if(!court.isUsable() || match == null || !match.canStartMatch()) {
			return false;
		}
		court.setMatch(match);
		return true;
	}
	
	public boolean swapMatch(CourtButton courtButton) {
		if(courtButton == null || equals(courtButton) || !courtButton.isUsableCourt()) {
			return false;
		}
		return court.swapCurrentMatch(courtButton.court);
	}
	
	public Match undoMatch() {
		Match match = court.getCurrentMatch();
		if(match == null) {
			return null;
		}
		if(court.undoMatch(match)) {
			return match;
		}
		return null;
	}
        
        public String getCourtId() {                    // added 10/10/19 bvw autoassign debug use 
		return this.court.getId().toString();
	}
	
	public boolean isAvailableCourt() {
		return court.isAvailable();
	}
	
	public boolean isUsableCourt() {
		return court.isUsable();
	}
	
	public boolean startedDrag() {
		return startedDrag;
	}
	
	private String generateFirstName(Team team) {
		String name = "";
		for(Player player : team.getPlayers()) {
			String firstName = player.getName();
			if(firstName == null) {
				continue;
			}
			int index = firstName.indexOf(' ');
			if(index == -1) {
				name += firstName + " / ";
			}
			else {
				name += firstName.substring(0, index) + " / ";
			}
		}
		if(name.length() > 0) {
			name = name.substring(0, name.length() - 3);
		}
		return name;
	}
}
