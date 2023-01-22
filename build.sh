#!/bin/bash

cd `dirname $0`

mkdir -f build

usage()
{
    exit 0
}

# Build CafeYike
cd CafeYike
mvn package
mvn assembly:assembly

# TODO Copy jar to build dir

# Build YikeManager
cd ../YikeManager

# TODO copy all folders to build dir?
# maybe build to wheel?