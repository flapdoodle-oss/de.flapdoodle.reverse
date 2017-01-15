package de.flapdoodle.transition;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Preconditions {

	public static void checkArgument(boolean expression, String errorMessage) {
		if (!expression) {
			throw new IllegalArgumentException(errorMessage);
		}
	}

	public static void checkArgument(boolean expression, String errorMessage, Object ... args) {
		if (!expression) {
			throw new IllegalArgumentException(format(errorMessage,args));
		}
	}

	public static <T> T checkNotNull(T reference, String errorMessage) {
		if (reference == null) {
			throw new NullPointerException(errorMessage);
		}
		return reference;
	}
	
	public static <T> T checkNotNull(T reference, String errorMessage, Object ... args) {
		if (reference == null) {
			throw new NullPointerException(format(errorMessage, args));
		}
		return reference;
	}
	
	private static Pattern PLACEHOLDER=Pattern.compile("%s");
	
	protected static String format(String message, Object ... args) {
		if (args.length>0) {
			int currentArg=0;
			int last=0;
			
			StringBuilder sb=new StringBuilder();
			Matcher matcher = PLACEHOLDER.matcher(message);
			while (matcher.find()) {
				sb.append(message.substring(last, matcher.start()));
				if (currentArg<args.length) {
					sb.append(asObject(args[currentArg++]));
				} else {
					sb.append("<arg").append(currentArg).append(">");
				}
				last=matcher.end();
			}
			sb.append(message.substring(last));
			if (currentArg<args.length) {
				for (int i=currentArg;i<args.length;i++) {
					sb.append(",").append(asObject(args[i]));
				}
			}
			return sb.toString();
		}
		return message;
	}
	
	protected static Object asObject(Object value) {
		if (value instanceof LazyArgument) {
			return ((LazyArgument) value).get();
		}
		return value;
	}
	
	public interface LazyArgument extends Supplier<Object> {
		
	}
	
	public static LazyArgument lazy(Supplier<?> supplier) {
		return () -> supplier.get();
	}
}
