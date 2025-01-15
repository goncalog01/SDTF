# How to run

In the root directory of the project:

- run `mvn clean install`
- run client: `java -cp client/target/client-1.0-SNAPSHOT.jar:library/target/library-1.0-SNAPSHOT.jar:links/target/links-1.0-SNAPSHOT.jar:server/target/server-1.0-SNAPSHOT.jar pt.ulisboa.tecnico.sec.Client [clientId] [configFileName]` (configFile should be in the /config directory)
- run member: `java -cp client/target/client-1.0-SNAPSHOT.jar:library/target/library-1.0-SNAPSHOT.jar:links/target/links-1.0-SNAPSHOT.jar:server/target/server-1.0-SNAPSHOT.jar pt.ulisboa.tecnico.sec.Member [memberId] [configFileName]` (configFile should be in the /config directory)

# Run tests

You can test the system by launching it using the different configuration files in the /config directory.

# Configuration files

Each line of a configuration file corresponds to a node of the system and has the following information about the node (separated by commas):
- the type ("member" or "client")
- the id
- the port

If the node is a member, it should also have:
- if it is the leader (1 for true, 0 for false)
- if it is byzantine (1 for true, 0 for false)

If the member is byzantine, it should specify which mode it should run:
- "silent" - does not send messages
- "spam" - sends the same message multiple times
- "mischief" - sends a wrong message
- "chameleon" - sends a message on behalf of another node

_Note: All the members should be listed before the clients_