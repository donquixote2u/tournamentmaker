package ui.component;

public class GenericWrapper<V> {
	private V value;
	
	public GenericWrapper(V value) {
		this.value = value;
	}
	
	public V getValue() {
		return value;
	}
	
	public boolean equals(Object other) {
		if(other == null || !(other instanceof GenericWrapper<?>)) {
			return false;
		}
		GenericWrapper<?> obj = (GenericWrapper<?>) other;
		return (value == null && obj.value == null) || (value != null && value.equals(obj.value));
	}
	
	public int hashCode() {
		if(value == null) {
			return -1;
		}
		return value.hashCode();
	}
	
	public String toString() {
		return value == null ? null : value.toString();
	}
}
