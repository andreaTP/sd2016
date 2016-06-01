#!/usr/bin/env bash

case "$1" in
  build)
    docker -D build -t="slide-docker-akkajs:latest" .
  ;;
  run)
    docker run --rm -i -t -p 4000:4000 -p 8000:8000 -p 9001:9001 -p 9002:9002 "slide-docker-akkajs:latest"
  ;;
  *)
    echo "Usage [ build, run ] "
    exit 0
  ;;
esac
