package de.flapdoodle.reverse.customize;

import de.flapdoodle.reverse.State;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.StateLookup;
import de.flapdoodle.reverse.Transition;
import org.immutables.value.Value;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class WriteBackup implements Transition<BackupID> {

	@Override
	public StateID<BackupID> destination() {
		return StateID.of(BackupID.class);
	}

	public StateID<BackupFolderName> backupFolderNameStateID() {
		return StateID.of(BackupFolderName.class);
	}

	public StateID<ListOfFiles> listOfFilesStateID() {
		return StateID.of(ListOfFiles.class);
	}

	public Set<StateID<?>> sources() {
		return new HashSet<>(Arrays.asList(backupFolderNameStateID(), listOfFilesStateID()));
	}

	@Override
	public State<BackupID> result(StateLookup lookup) {
		BackupFolderName backupFolderName = lookup.of(backupFolderNameStateID());
		ListOfFiles listOfFiles = lookup.of(listOfFilesStateID());

		BackupID backupID = backupFiles(backupFolderName, listOfFiles);

		return State.of(backupID);
	}
	
	private static BackupID backupFiles(BackupFolderName backupFolderName, ListOfFiles listOfFiles) {
		// real backup not implemented ...
		
		return BackupID.of(backupFolderName, LocalDateTime.now());
	}
}
