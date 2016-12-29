package de.flapdoodle.transition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;

public class TearDownCounter {
	
	Map<Object, RuntimeException> tearDowns=new LinkedHashMap<>();
	
	public <T> TearDown<T> listener() {
		return t -> {
			RuntimeException old = tearDowns.put(t, new RuntimeException("->"+t));
			if (old!=null) {
				old.printStackTrace();
				throw new IllegalArgumentException("tearDown for ["+t+"] already called");
			}
		};
	}
	
	public void assertTearDowns(Object ...values) {
		List<Object> missedTearDowns = Stream.of(values)
			.filter(v -> !tearDowns.containsKey(v))
			.collect(Collectors.toList());
		
		Assert.assertTrue("missed tearDowns: "+missedTearDowns, missedTearDowns.isEmpty());
	}
}
