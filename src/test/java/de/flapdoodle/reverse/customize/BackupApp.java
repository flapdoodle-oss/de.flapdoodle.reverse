/**
 * Copyright (C) 2016
 *   Michael Mosmann <michael@mosmann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.reverse.customize;

import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Start;
import org.immutables.value.Value;

import java.nio.file.Paths;
import java.util.UUID;

@Value.Immutable
public class BackupApp {

	@Value.Default
	public Transition<UUID> uuid() {
		return Start.to(UUID.class).providedBy(UUID::randomUUID);
	}

	@Value.Default
	public Transition<ListOfFiles> listOfFiles() {
		return Start.to(ListOfFiles.class)
			.initializedWith(ListOfFiles.of(Paths.get("a")));
	}

	@Value.Default
	public Transition<BackupFolderName> backupFolderName() {
		return Derive.given(UUID.class)
			.state(BackupFolderName.class)
			.deriveBy(id -> BackupFolderName.of("backup-" + id.toString()));
	}

	@Value.Default
	public Transition<BackupID> writeBackup() {
		return new WriteBackup();
	}

	public BackupID startBackup() {
		try (TransitionWalker.ReachedState<BackupID> withBackupID = Transitions.from(uuid(), listOfFiles(), backupFolderName(), writeBackup())
			.walker()
			.initState(StateID.of(BackupID.class))) {
			return withBackupID.current();
		}
	}

	public static ImmutableBackupApp instance() {
		return ImmutableBackupApp.builder().build();
	}
}
