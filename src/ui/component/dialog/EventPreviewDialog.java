package ui.component.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import data.event.Event;
import data.event.painter.EventPainter;
import data.team.Team;
import ui.component.GenericWrapper;
import ui.component.panel.EventBracketCanvas;
import ui.main.TournamentViewManager;

public class EventPreviewDialog extends JDialog {
	private static final long serialVersionUID = -4383723095814957340L;

	public EventPreviewDialog(JFrame owner, final TournamentViewManager manager, final Event event, List<Team> newTeams) {
		// set up the dialog
		super(owner, "Event Preview", true);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
		setPreferredSize(new Dimension(dim.width - insets.left - insets.right, dim.height - insets.top - insets.bottom));
		final List<Team> originalTeams = new ArrayList<Team>();
		originalTeams.addAll(event.getTeams());
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				manager.getTournament().undoEvent(event);
				event.setTeams(originalTeams);
				dispose();
			}
		});
		// set up the event canvas
		JScrollPane scrollPane = new JScrollPane();
		final EventBracketCanvas eventCanvas = new EventBracketCanvas(36, 10, 10, 14.0f, 5, scrollPane);
		eventCanvas.setBackground(manager.getBackgroundColor());
		scrollPane.setViewportView(eventCanvas);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(36);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(36);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		// set up the filter
		final JComboBox<GenericWrapper<EventPainter>> filter = new JComboBox<GenericWrapper<EventPainter>>();
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				@SuppressWarnings("unchecked")
				GenericWrapper<EventPainter> selected = (GenericWrapper<EventPainter>) filter.getSelectedItem();
				if(selected == null) {
					eventCanvas.setEventPainter(null);
				}
				else {
					eventCanvas.setEventPainter(selected.getValue());
				}
			}
		});
		getContentPane().add(filter, BorderLayout.PAGE_START);
		// set up the buttons
		JPanel buttons = new JPanel(new FlowLayout());
		JButton print = new JButton("Print");
		print.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manager.printComponent(eventCanvas);
			}
		});
		buttons.add(print);
		JButton close = new JButton("Close");
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				processWindowEvent(new WindowEvent(EventPreviewDialog.this, WindowEvent.WINDOW_CLOSING));
			}
		});
		buttons.add(close);
		getContentPane().add(buttons, BorderLayout.PAGE_END);
		// show the preview
		try {
			event.setTeams(newTeams);
		}
		catch(Exception e) {}
		if(!event.canStart()) {
			JOptionPane.showMessageDialog(owner, "Invalid teams detected. Please fix before previewing the event.", "Error", JOptionPane.ERROR_MESSAGE);
			event.setTeams(originalTeams);
			dispose();
			return;
		}
		manager.getTournament().addMatches(event.start());
		for(EventPainter eventPainter : event.getEventPainters(event.getDisplayLevels().get(0), manager.getTournament().getDisableBracketPooling())) {
			filter.addItem(new GenericWrapper<EventPainter>(eventPainter));
		}
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}
}
