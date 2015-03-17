package ui.component.button;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import ui.component.dialog.EventResultDialog;
import ui.component.dialog.EventTeamSelector;
import data.event.Event;
import data.event.EventUtils;

public class EventButton extends JButton {
	private static final long serialVersionUID = 6177228955673185215L;
	public static final Color PAUSED = new Color(255, 120, 0);
	public static final Color RUNNING = new Color(38, 120, 0);
	public static final Color WAITING = Color.RED;
	public static final Color FINISHED = Color.BLACK;
	private Event event;
	private boolean isPreview;
	
	public EventButton(Event event, final EventTeamSelector teamSelector, final EventResultDialog resultDialog) {
		super(event.getName());
		this.event = event;
		updateStatus();
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(EventButton.this.event.isStarted()) {
					resultDialog.show(EventButton.this);
				}
				else {
					teamSelector.show(EventButton.this);
				}
			}
		});
	}
	
	public synchronized void updateStatus() {
		setText(event.getName());
		if(!event.isStarted() || isPreview) {
			setForeground(WAITING);
			setToolTipText("Not Started");
		}
		else if(event.isComplete()) {
			setForeground(FINISHED);
			setToolTipText("Finished");
		}
		else if(EventUtils.eventIsPaused(event)) {
			setForeground(PAUSED);
			setToolTipText("Paused");
		}
		else {
			setForeground(RUNNING);
			setToolTipText("In Progress");
		}
	}
	
	public Event getEvent() {
		return event;
	}
	
	public void setIsPreview(boolean isPreview) {
		this.isPreview = isPreview;
	}
}
