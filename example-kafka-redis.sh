#!/bin/bash

./samples/gradlew -p ./samples clean :kafka-source-redis-sink:shadowJar
docker cp ./samples/jobs/kafka-source-redis-sink/build/libs/kafka-source-redis-sink-all-1.0-SNAPSHOT.jar jobmanager:/opt/flink/examples/streaming
docker exec -it jobmanager ./bin/flink run -d examples/streaming/kafka-source-redis-sink-all-1.0-SNAPSHOT.jar
