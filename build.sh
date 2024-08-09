#!/bin/bash
# Get location of script
set -e

home=$(realpath $(dirname $0))

DOCKER_VERSION=`cat $home/DOCKER_VERSION`

BUILD_DIR=${home}/docker/build

mkdir -p $BUILD_DIR

echo "Building YikeBot"

usage()
{
    echo "Usage TODO lol"
    exit 1
}

# Build CafeYike Java
build_java()
{
    cd ${home}/CafeYike
    YIKE_VERS=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`
    if [[ $? != 0 ]]
    then
        echo "Error getting version"
        echo $YIKE_VERS
        exit 1
    fi
    YIKE_EXEC=CafeYike-${YIKE_VERS}.jar
    echo Building ${YIKE_EXEC}

    mvn assembly:assembly --quiet -U
    cp target/CafeYike-${YIKE_VERS}-jar-with-dependencies.jar ${BUILD_DIR}/${YIKE_EXEC}

}

use_old_java()
{
    cd ${BUILD_DIR}
    YIKE_EXEC=`find | grep "\.jar"`
    YIKE_EXEC=`basename $YIKE_EXEC`
    YIKE_VERS=`echo ${YIKE_EXEC} | grep -E -o "[0-9](\.[0-9])+"`
    echo Using ${YIKE_EXEC} version ${YIKE_VERS}
}

# Build YikeManager backend
build_backend()
{
    cd ${home}/YikeManager/ym-backend

    rm -rf dist/*
    if [ ! -d venv ]
    then
        echo Creating venv
        python3 -m venv venv
        venv/bin/python -m pip install build hatchling wheel
    fi

    echo Building ym-backend
    venv/bin/python -m build --wheel

    cp dist/*.whl ${BUILD_DIR}/
    cp requirements.txt ${BUILD_DIR}/
}

# Build YikeManager frontend
build_frontend()
{
    echo Building ym-frontend
    cd ${home}/YikeManager/ym-frontend

    npm run build

    mkdir -p ${BUILD_DIR}/ym-frontend

    cp -r dist/* ${BUILD_DIR}/ym-frontend/
}

# generate system.conf
gen_config()
{
    echo Generating Config

    if [ -z "${YIKE_EXEC}" ]
    then
        echo CafeYike exec not found
        exit 1
    fi

    sed \
        -e "s/@@yikeJar/$YIKE_EXEC/" \
        ${home}/docker/bashrc_template.sh \
        > ${BUILD_DIR}/.bashrc

}

# Build the docker container
build_docker()
{
    echo Building Container version $DOCKER_VERSION
    export BUILDKIT_COLORS="run=cyan:error=yellow:cancel=blue:warning=white"
    cd ${home}/docker

    CY_UID=`id -u cafeyike`

    docker build \
        --build-arg UID=$CY_UID \
        -t cafe-yike:$DOCKER_VERSION .
}

BUILD_JAVA=1
BUILD_FE=1
BUILD_BE=1

echo "Parsing cmd line"

while getopts "rjbfad" opt
do
    case $opt in
        # Build java
        j) 
            echo "Building Java only"
            BUILD_JAVA=1
            BUILD_FE=0
            BUILD_BE=0
        ;;
        # Build backend
        b)
            echo "Building Backend only"
            BUILD_BE=1
            BUILD_JAVA=0
            BUILD_FE=0
        ;;
        # Build frontend
        f) 
            echo "Building Frontend only"
            BUILD_FE=1
            BUILD_JAVA=0
            BUILD_BE=0
        ;;
        # Docker only
        d)
            echo "Building container only"
            BUILD_JAVA=0
            BUILD_BE=0
            BUILD_FE=0
        ;;
        # Unknown option
        *) 
            echo "Unknown option $opt"
            usage ;;
    esac
done

if [ $BUILD_JAVA = 1 ]
then
    build_java
else
    use_old_java
fi

if [ $BUILD_BE = 1 ]
then
    build_backend
else
    echo Skipping Backend Build
fi

if [ $BUILD_FE = 1 ]
then
    build_frontend
else
    echo Skipping Frontend Build
fi

gen_config
build_docker
