package de.flapdoodle.transition.initlike;

import java.util.LinkedHashMap;
import java.util.Map;

import de.flapdoodle.transition.NamedType;
import de.flapdoodle.transition.Preconditions;
import de.flapdoodle.transition.State;

public class MapBasedStateOfNamedType implements StateOfNamedType {

	private final Map<NamedType<?>, State<?>> stateMap;

	public MapBasedStateOfNamedType(Map<NamedType<?>, State<?>> stateMap) {
		this.stateMap = new LinkedHashMap<>(stateMap);
	}
	
	@Override
	public <D> State<D> of(NamedType<D> type) {
		return (State<D>) Preconditions.checkNotNull(stateMap.get(type),"could find state for %s", type);
	}
	
}