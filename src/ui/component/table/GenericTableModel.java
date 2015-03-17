package ui.component.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import ui.main.TournamentViewManager;

public class GenericTableModel<T> extends AbstractTableModel {
	private static final long serialVersionUID = -8326705759884878890L;
	private List<T> data;
	private List<GenericTableColumn> columns;
	private TournamentViewManager view;
	
	public GenericTableModel(TournamentViewManager view) {
		this.view = view;
		data = new ArrayList<T>();
		columns = new ArrayList<GenericTableColumn>();
	}
	
	public void addColumn(String name, boolean editable, Class<?> columnClass, GenericValue<T> obj) {
		columns.add(new GenericTableColumn(name, editable, columnClass, obj));
	}
	
	public void addColumn(String name, boolean editable, Class<?> columnClass, GenericValue<T> obj, boolean autoUpdate) {
		addColumn(name, editable, columnClass, obj);
		columns.get(columns.size() - 1).autoUpdate = autoUpdate;
	}
	
	public void addColumn(String name, boolean editable, Class<?> columnClass, GenericValue<T> obj, Collection<Integer> relatedColumns) {
		addColumn(name, editable, columnClass, obj, false, relatedColumns);
	}
	
	public void addColumn(String name, boolean editable, Class<?> columnClass, GenericValue<T> obj, boolean autoUpdate, Collection<Integer> relatedColumns) {
		addColumn(name, editable, columnClass, obj, autoUpdate);
		if(relatedColumns != null && !relatedColumns.isEmpty()) {
			columns.get(columns.size() - 1).relatedColumns.addAll(relatedColumns);
		}
	}
	
	public void update() {
		for(int i = 0; i < columns.size(); ++i) {
			if(columns.get(i).autoUpdate) {
				for(int j = 0; j < data.size(); ++j) {
					fireTableCellUpdated(j, i);
				}
			}
		}
	}
	
	public void addData(Collection<? extends T> data) {
		insertData(this.data.size(), data);
	}
	
	public void insertData(int index, Collection<? extends T> data) {
		if(data == null || data.isEmpty()) {
			return;
		}
		this.data.addAll(index, data);
		fireTableRowsInserted(index, index + data.size() - 1);
	}
	
	public void setData(Collection<? extends T> data) {
		this.data.clear();
		if(data != null) {
			this.data.addAll(data);
		}
		fireTableDataChanged();
	}
	
	public void removeRow(int row) {
		data.remove(row);
		fireTableRowsDeleted(row, row);
	}
	
	public int indexOf(T value) {
		for(int i = 0; i < data.size(); ++i) {
			if(data.get(i).equals(value)) {
				return i;
			}
		}
		return -1;
	}
	
	public T getData(int row) {
		return data.get(row);
	}

	public int getColumnCount() {
		return columns.size();
	}

	public int getRowCount() {
		return data.size();
	}

	public Object getValueAt(int row, int col) {
		return columns.get(col).obj.getValue(data.get(row));
	}
	
	public Class<?> getColumnClass(int col) {
		return columns.get(col).columnClass;
	}
	
	public boolean isCellEditable(int row, int col) {
		return columns.get(col).editable;
	}
	
	public String getColumnName(int col) {
		return columns.get(col).name;
	}
	
	public void setValueAt(Object value, int row, int col) {
		columns.get(col).obj.setValue(value, data.get(row));
		fireTableCellUpdated(row, col);
		for(Integer integer : columns.get(col).relatedColumns) {
			fireTableCellUpdated(row, integer);
		}
		view.modified();
    }
	
	private class GenericTableColumn {
		String name;
		boolean editable;
		Class<?> columnClass;
		GenericValue<T> obj;
		boolean autoUpdate;
		List<Integer> relatedColumns;
		
		public GenericTableColumn(String name, boolean editable, Class<?> columnClass, GenericValue<T> obj) {
			this.name = name;
			this.editable = editable;
			this.columnClass = columnClass;
			this.obj = obj;
			autoUpdate = false;
			relatedColumns = new ArrayList<Integer>();
		}
	}
}
