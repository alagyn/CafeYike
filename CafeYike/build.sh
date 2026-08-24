#!/bin/bash

SRC_ROOT=$(realpath $(dirname $0))

cd $SRC_ROOT

if [ "$1" = "-v" ]
then
  EXEC="gradle --no-daemon printVersion -q"
else
  EXEC="gradle --no-daemon fatJar"
fi

podman run \
  --rm \
  --volume $SRC_ROOT:/src:rw,z \
  --volume $HOME/.m2/:/root/.m2:ro,z \
  -w /src \
  cafe-yike-build:1 $EXEC
