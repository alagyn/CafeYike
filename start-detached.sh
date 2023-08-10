#!/bin/bash

home=$(realpath $(dirname $0))

DB_DIR=$home/dat/
DB_DIR=`realpath $DB_DIR`
USER=root

docker run \
    --rm \
    --user $USER \
    --hostname cafe-yike \
    --workdir /home/$USER \
    -p 8000:8000 \
    -e YM_SYS_CFG=/home/$USER/system.conf \
    -e CafeYikeDB=/home/$USER/dat/cafe.db \
    -v $DB_DIR:/home/$USER/dat/ \
    cafe-yike:1 "/home/$USER/start_manager.sh" > manager.log 2>&1 &
