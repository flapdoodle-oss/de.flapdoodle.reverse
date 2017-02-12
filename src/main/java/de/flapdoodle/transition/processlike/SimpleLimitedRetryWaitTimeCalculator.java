package de.flapdoodle.transition.processlike;

import de.flapdoodle.transition.Preconditions;

public class SimpleLimitedRetryWaitTimeCalculator implements HistoryBasedRetryWaitTime {

	private final long min;
	private final long max;

	public SimpleLimitedRetryWaitTimeCalculator(long min, long max) {
		Preconditions.checkArgument(min>=0, "min is not >=0: %s",asString(min));
		Preconditions.checkArgument(max>=0, "max is not >=0: %s",asString(max));
		Preconditions.checkArgument(min<max, "min is not smaller than max: %s - %s",asString(min),asString(max));
		this.min = min;
		this.max = max;
	}
	
	@Override
	public long waitTime(long lastDurration, boolean success) {
		Preconditions.checkArgument(lastDurration>=0, "durration is negative: %s",asString(lastDurration));
		long changed;
		if (success) {
			changed = lastDurration*3/4; 
		} else {
			long lastNonZero = lastDurration==0 ? min : lastDurration;
			changed = lastNonZero*2;
		}
		long ret = min>changed ? min : changed;
		ret = max<changed ? max : changed;

		System.out.println(success+" "+asString(lastDurration)+" -> "+asString(ret));

		return ret;
	}
	
	private static String asString(long ms) {
		long seconds = ms/1000;
		return seconds+"s "+(ms%1000)+"ms";
	}
}
