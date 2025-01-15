#!/bin/bash

# set the path to your Java executable
JAVA_EXECUTABLE=/usr/bin/java

# set the path to your Maven executable
MVN_EXECUTABLE=/usr/bin/mvn

# set the path to your Java program
JAVA_PROGRAM=client/target/client-1.0-SNAPSHOT.jar:library/target/library-1.0-SNAPSHOT.jar:links/target/links-1.0-SNAPSHOT.jar:server/target/server-1.0-SNAPSHOT.jar

# set the number of members to run
NUM_MEMBERS=4

# get the client number and config file name from the command line
CLIENT_NUM=5
CONFIG_FILE=configFile$1.txt

# set the input and output file names
INPUT_FILE=demos/input$2.txt
OUTPUT_FILE=demos/output$2.txt
EXPECTED_OUTPUT_FILE=demos/expected_output$2.txt

# kill any processes associated with ports 8080 through 8085
for (( port=8080; port<=8085; port++ ))
do
  PIDS=$(lsof -i :$port -t)
  if [ ! -z "$PIDS" ]; then
    kill $PIDS
  fi
done

cd ../

# clean and install the Maven project
#mvn clean install

# start the member processes
for (( i=1; i<=$NUM_MEMBERS; i++ ))
do
  $JAVA_EXECUTABLE -cp $JAVA_PROGRAM pt.ulisboa.tecnico.sec.Member $i &
  MEMBER_PIDS[$i]=$!
done

sleep 2

# start the client process and pipe input from the input file
$JAVA_EXECUTABLE -cp $JAVA_PROGRAM pt.ulisboa.tecnico.sec.Client $CLIENT_NUM $CONFIG_FILE < $INPUT_FILE > $OUTPUT_FILE &
CLIENT_PID=$!

sleep 30
kill $CLIENT_PID

# wait for the client process to finish
wait $CLIENT_PID

# kill the member processes
for (( i=1; i<=$NUM_MEMBERS; i++ ))
do
  kill -INT ${MEMBER_PIDS[$i]}
done

# compare the output produced by the client to the expected output file
if cmp -s "$OUTPUT_FILE" "$EXPECTED_OUTPUT_FILE"; then
  echo "Output matches expected output"
else
  echo "Output does not match expected output"
fi

cd demos/