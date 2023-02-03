#!/bin/bash

usage()
{
    echo start.sh -d DB_DIR [-u USER] [-j | -b]
    exit 0
}

home=$(realpath $(dirname $0))

DB_DIR=$home/dat/
USER=pi
RUN_BASH=0
RUN_JAVA=0


while getopts "bd:u:j" opt
do
    case $opt in
        b) RUN_BASH=1 ;;
        d) DB_DIR=$OPTARG ;;
        u) USER=$OPTARG ;;
        j) RUN_JAVA=1 ;;
        *) usage ;;
    esac
done

DB_DIR=`realpath $DB_DIR`

if [ -z "$DB_DIR" ]
then
    echo Please Specify a database directory
    usage
fi

if [ ! -d $DB_DIR ]
then
    echo \"${DB_DIR}\" not found
    usage
fi

if [ $RUN_BASH = 1 -a $RUN_JAVA = 1 ]
then
    echo Cannot specify both -j and -b
    exit 0
fi

if [ $RUN_BASH = 1 ]
then
    EXEC=/bin/bash
elif [ $RUN_JAVA = 1 ]
then
    EXEC=/home/pi/start_java.sh
else
    EXEC=/home/pi/start_manager.sh
fi

docker run \
    --rm \
    -ti \
    --user $USER \
    --hostname cafe-yike \
    --workdir /home/pi \
    -p 8000:8000 \
    -e YM_SYS_CFG=/home/pi/system.conf \
    -e CafeYikeDB=/home/pi/dat/cafe.db \
    cafe-yike:1 $EXEC
