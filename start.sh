#!/bin/bash

usage()
{
    echo "start.sh -d DB_DIR [-u USER] [-j | -b] | -t"
    echo "    -d : Set the database directory"
    echo "    -u : Override the user"
    echo "    -j : Run the bot only, without the manager"
    echo "    -b : Start an interactive bash session"
    echo "    -t : allocate a tty"
    echo "    Not specifying j or b will automatically run the manager"
    exit 0
}

home=$(realpath $(dirname $0))
VERSION=`cat $home/DOCKER_VERSION`

DB_DIR=$home/dat/
RUN_BASH=0
RUN_JAVA=0
MAKE_TTY=0

while getopts "bd:jht" opt
do
    case $opt in
        b) 
            RUN_BASH=1
            MAKE_TTY=1
            ;;
        d) DB_DIR=$OPTARG ;;
        u) USER=$OPTARG ;;
        j) RUN_JAVA=1 ;;
        h) usage ;;
        t) MAKE_TTY=1 ;;
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

EXTRA_ARGS=

if [ $RUN_BASH = 1 ]
then
    EXEC=/bin/bash
elif [ $RUN_JAVA = 1 ]
then
    EXEC="/bin/bash /home/cafeyike/start_java.sh"
else
    EXEC="/bin/bash /home/cafeyike/start_manager.sh"
fi

if [ $MAKE_TTY = 1 ]
then
    EXTRA_ARGS=-ti
fi

docker run \
    --rm \
    $EXTRA_ARGS \
    --user cafeyike \
    --hostname cafe-yike \
    --workdir /home/cafeyike \
    -p 8000:8000 \
    --name yike-manager \
    -v $DB_DIR:/home/cafeyike/dat/ \
    cafe-yike:$VERSION $EXEC
