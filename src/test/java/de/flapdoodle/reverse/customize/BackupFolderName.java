package de.flapdoodle.reverse.customize;

import org.immutables.value.Value;

@Value.Immutable
public interface BackupFolderName {
	@Value.Parameter
	String name();

	static BackupFolderName of(String name) {
		return ImmutableBackupFolderName.of(name);
	}
}
