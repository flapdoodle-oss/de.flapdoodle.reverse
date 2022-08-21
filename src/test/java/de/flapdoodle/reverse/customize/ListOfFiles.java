package de.flapdoodle.reverse.customize;

import org.immutables.value.Value;

import java.nio.file.Path;
import java.util.List;

@Value.Immutable
public interface ListOfFiles {
	List<Path> files();

	static ListOfFiles of(Path ... paths) {
		 return ImmutableListOfFiles.builder()
			 .addFiles(paths)
			 .build();
	}

}
