package ui.component.table;

// override these functions to properly get and set the values for T
public abstract class GenericValue<T> {
	public Object getValue(T obj) {
		return null;
	}
	
	public void setValue(Object value, T obj) {}
}
