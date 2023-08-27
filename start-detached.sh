#!/bin/bash

home=$(realpath $(dirname $0))

VERSION=`cat $home/DOCKER_VERSION`

usage()
{
    echo "Usage: start-detached.sh [database-dir]"
    exit 1
}

if [ -z "$1" ]
then
    usage
fi

DB_DIR=`realpath $1`

if [ ! -d $DB_DIR ]
then
    echo \"${DB_DIR}\" not found
    usage
fi

docker run \
    --rm \
    --user cafeyike \
    --hostname cafe-yike \
    --workdir /home/cafeyike \
    --name yike-manager \
    -p 8000:8000 \
    -v $DB_DIR:/home/cafeyike/dat/ \
    cafe-yike:$VERSION "/home/cafeyike/start_manager.sh" > manager.log 2>&1 &
