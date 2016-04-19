package ui.component.editor;

import java.util.HashSet;

import javax.swing.JFrame;
import javax.swing.JTable;

import ui.component.table.GenericTableModel;
import ui.main.TournamentViewManager;
import data.event.Event;
import data.player.Player;
import data.tournament.TournamentUtils;

public class EventEditor extends ListEditor {
	private static final long serialVersionUID = 9055589327955083655L;
	private TournamentViewManager manager;

	public EventEditor(JFrame owner, TournamentViewManager manager, String emptyValueMessage) {
		super(owner, emptyValueMessage);
		this.manager = manager;
	}
	
	@SuppressWarnings("unchecked")
	protected void setEditorValue(JTable table, Object value, int row, int column) {
		HashSet<String> disabled = new HashSet<String>();
		for(Event event : manager.getTournament().getEvents()) {
			if(event.isStarted()) {
				disabled.add(event.getName());
			}
		}
		setDisabledStrings(disabled);
		setValues(TournamentUtils.getValidEventNames(((GenericTableModel<Player>) table.getModel()).getData(table.convertRowIndexToModel(row)), manager.getTournament().getEvents()));
		super.setEditorValue(table, value, row, column);
	}
}
