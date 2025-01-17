#!/bin/bash

./samples/gradlew -p ./samples clean shadowJar

docker cp ./samples/jobs/random-kafka-sink/build/libs/random-kafka-sink-all-1.0-SNAPSHOT.jar jobmanager:/opt/flink/examples/streaming
docker exec -it jobmanager ./bin/flink run -d examples/streaming/random-kafka-sink-all-1.0-SNAPSHOT.jar

docker cp ./samples/jobs/kafka-source-redis-sink/build/libs/kafka-source-redis-sink-all-1.0-SNAPSHOT.jar jobmanager:/opt/flink/examples/streaming
docker exec -it jobmanager ./bin/flink run -d examples/streaming/kafka-source-redis-sink-all-1.0-SNAPSHOT.jar

docker cp ./samples/jobs/partitioned-stream/build/libs/partitioned-stream-all-1.0-SNAPSHOT.jar jobmanager:/opt/flink/examples/streaming
docker exec -it jobmanager ./bin/flink run -d examples/streaming/partitioned-stream-all-1.0-SNAPSHOT.jar