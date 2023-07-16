# Customize Transitions

You may start to create a solution based on single transitions as 
in the following example. This could be a backup application:

```java
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
```

You can avoid names if you just use many types to express the different results of
your application. Most of the time these types are just wrapper:

```java
@Value.Immutable
public interface ListOfFiles {
  List<Path> files();

  static ListOfFiles of(Path ... paths) {
     return ImmutableListOfFiles.builder()
       .addFiles(paths)
       .build();
  }

}
```

```java
@Value.Immutable
public interface BackupFolderName {
  @Value.Parameter
  String name();

  static BackupFolderName of(String name) {
    return ImmutableBackupFolderName.of(name);
  }
}
```

You can create your own implementation of an transition:

```java
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
```

... which has `BackupID` as result: 

```java
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
```

But if you want to customize this, you have to change the code which creates all
these transitions. If you want to create a library any customization is very limited.

As your application consists of more or less independent parts it would be helpful if
one can change only some parts and reuse most of the other parts. 
One way to do this: create a class whith all these parts:

```java
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
```
           
This way you can just use it:

```java
BackupID backupId = new BackupApp().startBackup();

assertThat(backupId.backupFolderName().name())
  .startsWith("backup-");
```

... or customize it with inheritance:

```java
BackupID backupId = new BackupApp() {
  @Override public Transition<BackupFolderName> backupFolderName() {
    return Derive.given(UUID.class)
      .state(BackupFolderName.class)
      .deriveBy(id -> BackupFolderName.of("override-backup-" + id.toString()));
  }
}.startBackup();

assertThat(backupId.backupFolderName().name())
  .startsWith("override-backup-");
```

... or customize it using code generated by (immutables.org)[immutables.org]:

```java
BackupID backupId = BackupApp.instance()
  .withBackupFolderName(Derive.given(UUID.class)
    .state(BackupFolderName.class)
    .deriveBy(id -> BackupFolderName.of("builder-backup-" + id.toString())))
  .startBackup();

assertThat(backupId.backupFolderName().name())
  .startsWith("builder-backup-");
```