#!/bin/bash

function wait_for_redis_become_available() {
  local redis_name=$1
  local port=$2
  local is_started_fully=$(redis-cli -p "$port" PING | grep PONG)
  echo "Waiting for Redis $redis_name to become available"
  while [ -z "$is_started_fully" ]; do
    sleep 1
    is_started_fully=$(redis-cli -p "$MASTER_PORT" PING | grep PONG)
  done
  echo "$1 Redis started fully at port $MASTER_PORT"
}

readonly MASTER_PORT="${MASTER_PORT:-6379}"
readonly REPLICAS="${REPLICAS:-3}"
readonly AOF="${AOF:-no}"
readonly AOF_CONFIG="${AOF_CONFIG:-everysec}"

# make redis-master home directory
readonly MASTER_DIR=/etc/redis/redis-master
readonly REDIS_CONFIG_FILE=redis.conf

mkdir -p "$MASTER_DIR"
cd "$MASTER_DIR"


# create config file for the master
touch "$REDIS_CONFIG_FILE"
echo "protected-mode no" >>"$REDIS_CONFIG_FILE"
echo "dir $MASTER_DIR" >>"$REDIS_CONFIG_FILE"
echo "port $MASTER_PORT" >>"$REDIS_CONFIG_FILE"
echo "appendonly $AOF" >>"$REDIS_CONFIG_FILE"
echo "appendfsync $AOF_CONFIG" >>"$REDIS_CONFIG_FILE"

# start the master

redis-server "$MASTER_DIR/$REDIS_CONFIG_FILE" &
wait_for_redis_become_available "MASTER" "$MASTER_PORT"



# starting replicas
for ((i = 1; i <= REPLICAS; i++)); do
  replica_dir="/etc/redis/redis-replica-${i}"
  mkdir -p "$replica_dir"
  cd "$replica_dir"
  touch "$REDIS_CONFIG_FILE"
  echo "dir $replica_dir" >>"$REDIS_CONFIG_FILE"
  echo "port $((MASTER_PORT + i)) " >>"$REDIS_CONFIG_FILE"
  echo "appendonly $AOF" >>"$REDIS_CONFIG_FILE"
  echo "appendfsync $AOF_CONFIG" >>"$REDIS_CONFIG_FILE"
  echo "replicaof 127.0.0.1 $MASTER_PORT" >>"$REDIS_CONFIG_FILE"
  redis-server "$replica_dir/$REDIS_CONFIG_FILE" &

done

for ((i = 1; i <= REPLICAS; i++)); do
  wait_for_redis_become_available "REPLICA $i" "$((MASTER_PORT + i))"
done


tail -f /dev/null
