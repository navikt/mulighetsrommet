#!/bin/bash

CURRENT_PATH="$(cd "$(dirname "$1")"; pwd)/$(basename "$1")"

docker pull ghcr.io/navikt/pdfgen:2.0.56
docker run \
        -v $CURRENT_PATH/templates:/app/templates \
        -v $CURRENT_PATH/fonts:/app/fonts \
        -v $CURRENT_PATH/data:/app/data \
        -v $CURRENT_PATH/resources:/app/resources \
        -p 8080:8080 \
        -e DISABLE_PDF_GET=false \
        -e JDK_JAVA_OPTIONS \
        -it \
        --rm \
        ghcr.io/navikt/pdfgen:2.0.56

