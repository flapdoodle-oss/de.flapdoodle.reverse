package de.flapdoodle.transition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;

public class TearDownCounter {
	
	Set<Object> tearDowns=new LinkedHashSet<>();
	
	public <T> TearDown<T> listener() {
		return t -> {
			if (!tearDowns.add(t)) {
				throw new IllegalArgumentException("tearDown for "+t+" already called");
			}
		};
	}
	
	public void assertTearDowns(Object ...values) {
		List<Object> missedTearDowns = Stream.of(values)
			.filter(v -> !tearDowns.contains(v))
			.collect(Collectors.toList());
		
		Assert.assertTrue("missed tearDowns: "+missedTearDowns, missedTearDowns.isEmpty());
	}
}
