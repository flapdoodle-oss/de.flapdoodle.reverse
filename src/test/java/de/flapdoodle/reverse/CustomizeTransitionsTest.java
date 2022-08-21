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
package de.flapdoodle.reverse;

import de.flapdoodle.reverse.customize.*;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Start;
import de.flapdoodle.testdoc.Includes;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomizeTransitionsTest {

	@RegisterExtension
	public static Recording recording = Recorder.with("CustomizeTransitions.md", TabSize.spaces(2))
		.sourceCodeOf("ListOfFiles.java", ListOfFiles.class, Includes.Trim, Includes.WithoutImports, Includes.WithoutPackage)
		.sourceCodeOf("BackupFolderName.java", BackupFolderName.class, Includes.Trim, Includes.WithoutImports, Includes.WithoutPackage)
		.sourceCodeOf("BackupID.java", BackupID.class, Includes.Trim, Includes.WithoutImports, Includes.WithoutPackage)
		.sourceCodeOf("WriteBackup.java", WriteBackup.class, Includes.Trim, Includes.WithoutImports, Includes.WithoutPackage)
		.sourceCodeOf("BackupApp.java", BackupApp.class, Includes.Trim, Includes.WithoutImports, Includes.WithoutPackage);

	@Test
	void writeFilesToBackup() {
		recording.begin();
		Transition<UUID> uuid = Start.to(UUID.class)
			.providedBy(UUID::randomUUID);

		Transition<ListOfFiles> listOfFiles = Start.to(ListOfFiles.class)
			.initializedWith(ListOfFiles.of(Paths.get("a")));

		Transition<BackupFolderName> backupFolderName = Derive.given(UUID.class)
			.state(BackupFolderName.class)
			.deriveBy(id -> BackupFolderName.of("backup-" + id.toString()));

		Transition<BackupID> writeBackup = new WriteBackup();

		Transitions transitions = Transitions.from(uuid, listOfFiles, backupFolderName, writeBackup);

		try (TransitionWalker.ReachedState<BackupID> withBackupID = transitions.walker()
			.initState(StateID.of(BackupID.class))) {

			assertThat(withBackupID.current().backupFolderName().name())
				.startsWith("backup-");
		}
		recording.end();
	}

	@Test
	void callBackupApp() {
		recording.begin();
		BackupID backupId = new BackupApp().startBackup();

		assertThat(backupId.backupFolderName().name())
			.startsWith("backup-");
		recording.end();
	}

	@Test
	void callBackupAppWithOverride() {
		recording.begin();
		BackupID backupId = new BackupApp() {
			@Override public Transition<BackupFolderName> backupFolderName() {
				return Derive.given(UUID.class)
					.state(BackupFolderName.class)
					.deriveBy(id -> BackupFolderName.of("override-backup-" + id.toString()));
			}
		}.startBackup();

		assertThat(backupId.backupFolderName().name())
			.startsWith("override-backup-");
		recording.end();
	}

	@Test
	void callBackupAppWithBuilderPattern() {
		recording.begin();
		BackupID backupId = BackupApp.instance()
			.withBackupFolderName(Derive.given(UUID.class)
				.state(BackupFolderName.class)
				.deriveBy(id -> BackupFolderName.of("builder-backup-" + id.toString())))
			.startBackup();

		assertThat(backupId.backupFolderName().name())
			.startsWith("builder-backup-");
		recording.end();
	}
}
