package de.flapdoodle.transition;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PreconditionsTest {

	@Test
	public void emptyArgsMustGiveMessage() {
		assertEquals("foo", Preconditions.format("foo"));
	}
	
	@Test
	public void oneArgMustGiveMessageWithArg() {
		assertEquals("foo bar", Preconditions.format("foo %s","bar"));
	}
	
	@Test
	public void oneMoreArgThanPlaceholderMustGiveArgAppendedToTheEnd() {
		assertEquals("foo bar,blub", Preconditions.format("foo %s","bar","blub"));
	}
	
	@Test
	public void oneMorePlaceholderThanArgMustGiveEmpty() {
		assertEquals("foo bar blub <arg2>", Preconditions.format("foo %s %s %s","bar","blub"));
	}
}
