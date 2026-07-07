#!/bin/sh

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"

"$ROOT/jdk-21.0.2/bin/java" -Dspring.config.location="$SCRIPT_DIR/application.yml" -jar "$SCRIPT_DIR/BeiDou.jar" &
