package de.flapdoodle.transition.processlike;

public interface HistoryBasedRetryWaitTime {
	long waitTime(long lastDurration, boolean success);
}
