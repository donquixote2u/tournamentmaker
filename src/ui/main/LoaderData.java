package ui.main;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import data.event.CreatableEvent;
import data.event.Event;
import data.team.modifier.CreatableTeamModifier;
import data.team.modifier.TeamModifier;

public class LoaderData {
	private Map<String, Class<? extends Event>> events;
	private Map<String, Class<? extends TeamModifier>> modifiers;
	
	public LoaderData() {
		events = new TreeMap<String, Class<? extends Event>>();
		modifiers = new TreeMap<String, Class<? extends TeamModifier>>();
	}
	
	@SuppressWarnings("unchecked")
	public void addData(Class<?> c) {
		if(c == null) {
			return;
		}
		CreatableEvent eventAnnotation = c.getAnnotation(CreatableEvent.class);
		if(eventAnnotation != null && Event.class.isAssignableFrom(c)) {
			events.put(getDisplayName(eventAnnotation.displayName(), events), (Class<? extends Event>) c);
		}
		CreatableTeamModifier modifierAnnotation = c.getAnnotation(CreatableTeamModifier.class);
		if(modifierAnnotation != null && TeamModifier.class.isAssignableFrom(c)) {
			modifiers.put(getDisplayName(modifierAnnotation.displayName(), modifiers), (Class<? extends TeamModifier>) c);
		}
	}
	
	public Map<String, Class<? extends Event>> getEvents() {
		return Collections.unmodifiableMap(events);
	}
	
	public Map<String, Class<? extends TeamModifier>> getModifiers() {
		return Collections.unmodifiableMap(modifiers);
	}
	
	private String getDisplayName(String name, Map<String, ?> map) {
		int count = 1;
		String displayName = name;
		while(map.containsKey(displayName)) {
			displayName = name + " (" + count++ + ")";
		}
		return displayName;
	}
}
