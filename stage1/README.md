# How to run

In the root directory of the project:

- run `mvn clean install`
- run client: `java -cp client/target/client-1.0-SNAPSHOT.jar:library/target/library-1.0-SNAPSHOT.jar:links/target/links-1.0-SNAPSHOT.jar:server/target/server-1.0-SNAPSHOT.jar pt.ulisboa.tecnico.sec.Client [clientId] [configFileName]` (configFile should be in the /config directory)
- run member: `java -cp client/target/client-1.0-SNAPSHOT.jar:library/target/library-1.0-SNAPSHOT.jar:links/target/links-1.0-SNAPSHOT.jar:server/target/server-1.0-SNAPSHOT.jar pt.ulisboa.tecnico.sec.Member [memberId]`

# Run tests

In the `/demos` directory:

- `./test1.sh [configFileId] [inputId]`

## Issue

Tests have some issues concerning sockets, so some tests may replicate the desired behavior but still fail.