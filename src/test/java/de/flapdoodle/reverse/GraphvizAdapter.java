package de.flapdoodle.reverse;

import de.flapdoodle.types.Try;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class GraphvizAdapter {

	static byte[] asSvg(String dot) {
		return Try.get(() -> {
			try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
				Graphviz.fromString(dot)
					.render(Format.SVG_STANDALONE)
					.toOutputStream(os);
				return os.toByteArray();
			}
		});
	}
}
