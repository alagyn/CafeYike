#!/bin/bash

home=$(realpath $(dirname $0))

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

USER=root

docker run \
    --rm \
    --user $USER \
    --hostname cafe-yike \
    --workdir /home/$USER \
    --name yike-manager \
    -p 8000:8000 \
    -e YM_SYS_CFG=/home/$USER/system.conf \
    -e CafeYikeDB=/home/$USER/dat/cafe.db \
    -v $DB_DIR:/home/$USER/dat/ \
    cafe-yike:1 "/home/$USER/start_manager.sh" > manager.log 2>&1 &
