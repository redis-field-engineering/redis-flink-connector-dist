#!/bin/bash

./samples/gradlew -p ./samples clean :random-kafka-sink:shadowJar
docker cp ./samples/jobs/random-kafka-sink/build/libs/random-kafka-sink-all-1.0-SNAPSHOT.jar jobmanager:/opt/flink/examples/streaming
docker exec -it jobmanager ./bin/flink run -d examples/streaming/random-kafka-sink-all-1.0-SNAPSHOT.jar