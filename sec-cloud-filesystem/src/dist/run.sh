#!/bin/sh

DOWNLOADS_DIR="downloads"
SNAPSHOTS_DIR="snapshots"
LOGS_DIR="logs"
MOUNT_POINT="$1"

if [ ! -d "$DOWNLOADS_DIR" ]; then
    mkdir -p "$DOWNLOADS_DIR"
    if [ $? -ne 0 ] ; then
        echo "Directory $DOWNLOADS_DIR couldn't be created"
        exit 1
    else
        echo "Directory $DOWNLOADS_DIR created"
    fi
fi

if [ ! -d "$SNAPSHOTS_DIR" ]; then
    mkdir -p "$SNAPSHOTS_DIR"
    if [ $? -ne 0 ] ; then
        echo "Directory $SNAPSHOTS_DIR couldn't be created"
        exit 1
    else
        echo "Directory $SNAPSHOTS_DIR created"
    fi
fi

if [ ! -d "$LOGS_DIR" ]; then
    mkdir -p "$LOGS_DIR"
    if [ $? -ne 0 ] ; then
        echo "Directory $LOGS_DIR couldn't be created"
        exit 1
    else
        echo "Directory $LOGS_DIR created"
    fi
fi

if [ ! -d "$MOUNT_POINT" ]; then
    mkdir -p "$MOUNT_POINT"
    if [ $? -ne 0 ] ; then
        echo "Mount point $MOUNT_POINT couldn't be created"
        exit 1
    else
        echo "Mount point $MOUNT_POINT created"
    fi
fi

#export JAVA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

./bin/seccloudfs "$@"
