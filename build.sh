#!/bin/bash
set -e
# Get location of script
home=$(realpath $(dirname $0))

BUILD_DIR=${home}/docker/build

mkdir -p $BUILD_DIR

usage()
{
    exit 0
}

# Build CafeYike Java
build_java()
{
    cd ${home}/CafeYike

    YIKE_VERS=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`
    YIKE_EXEC=CafeYike-${YIKE_VERS}.jar
    echo Building ${YIKE_EXEC}

    mvn assembly:assembly --quiet
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
    if [ $RELEASE = 1 ]
    then
        echo Using LIVE token
        token_file=${home}/token.txt
        loglevel=Debug
    else
        echo Using TEST token
        token_file=${home}/test-token.txt
        loglevel=Debug
    fi

    if [ -f $token_file ]
    then
        TOKEN=`cat ${token_file}`
    else
        TOKEN=""
    fi
    
    if [ -z "${TOKEN}" ]
    then
        echo Token not found, please create ${token_file}
        exit 1
    fi

    if [ -z "${YIKE_EXEC}" ]
    then
        echo CafeYike exec not found
        exit 1
    fi

    sed \
        -e "s/@@token/${TOKEN}/" \
        -e "s/@@CafeYikeJar/${YIKE_EXEC}/" \
        -e "s/@@loglevel/${loglevel}/" \
        ${home}/docker/system_template.conf \
        > ${BUILD_DIR}/system.conf

    sed \
        -e "s/@@CafeYikeJar/${YIKE_EXEC}/" \
        ${home}/docker/start_java.sh \
        > ${BUILD_DIR}/start_java.sh
}

# Build the docker container
build_docker()
{
    echo Building Container
    export BUILDKIT_COLORS="run=cyan:error=yellow:cancel=blue:warning=white"
    cd ${home}/docker
    docker build -t cafe-yike:1 .
}

BUILD_JAVA=1
BUILD_FE=1
BUILD_BE=1
RELEASE=0

while getopts "rjbfad" opt
do
    case $opt in
        # Build java
        j) 
            BUILD_JAVA=1
            BUILD_FE=0
            BUILD_BE=0
        ;;
        # Build backend
        b)
            BUILD_BE=1
            BUILD_JAVA=0
            BUILD_FE=0
        ;;
        # Build frontend
        f) 
            BUILD_FE=1
            BUILD_JAVA=0
            BUILD_BE=0
        ;;
        # Build release mode
        r)
            RELEASE=1
        ;;
        # Docker only
        d)
            BUILD_JAVA=0
            BUILD_BE=0
            BUILD_FE=0
        ;;
        # Unknown option
        *) usage ;;
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
