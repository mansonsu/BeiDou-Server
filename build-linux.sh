#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$ROOT"

cd "$ROOT/gms-ui"
yarn install --frozen-lockfile
yarn build

rm -rf "$ROOT/gms-server/src/main/resources/static"
mkdir -p "$ROOT/gms-server/src/main/resources/static"
cp -R "$ROOT/gms-ui/dist/." "$ROOT/gms-server/src/main/resources/static/"

cd "$ROOT"
mvn -B -ntp -pl gms-server -am -DskipTests package
