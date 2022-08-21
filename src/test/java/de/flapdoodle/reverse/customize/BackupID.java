package de.flapdoodle.reverse.customize;

import org.immutables.value.Value;

import java.time.LocalDateTime;

@Value.Immutable
public interface BackupID {
	@Value.Parameter
	BackupFolderName backupFolderName();

	@Value.Parameter
	LocalDateTime timeStamp();

	static BackupID of(BackupFolderName backupFolderName, LocalDateTime timeStamp) {
		return ImmutableBackupID.of(backupFolderName, timeStamp);
	}
}
