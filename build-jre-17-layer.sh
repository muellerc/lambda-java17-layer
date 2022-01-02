#!/bin/sh

# remove a maybe earlier build layers
rm jre-17-layer-x86.zip
rm jre-17-layer-arm.zip

###############
## X86 BUILD ##
###############
docker build --platform=linux/amd64 --progress=plain -t lambda-jre-17-layer-x86 .
# extract the runtime.zip from the Docker container and store it locally
docker run --rm --entrypoint cat lambda-jre-17-layer-x86 jre-17-layer.zip > jre-17-layer-x86.zip

###############
## ARM BUILD ##
###############
docker build --platform=linux/arm64/v8 --progress=plain -t lambda-jre-17-layer-arm .
# extract the runtime.zip from the Docker container and store it locally
docker run --rm --entrypoint cat lambda-jre-17-layer-arm jre-17-layer.zip > jre-17-layer-arm.zip
