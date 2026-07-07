#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$ROOT"

JAVA_HOME="$ROOT/jdk-21.0.2"
if [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "JDK 21 not found or not executable: $JAVA_HOME" >&2
  exit 1
fi
if [ ! -x "$JAVA_HOME/bin/javac" ]; then
  echo "Full JDK 21 is required for packaging: $JAVA_HOME" >&2
  echo "This folder looks like a JRE. Replace it with a JDK 21 build that includes javac." >&2
  exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

MIGRATION_DIR="$ROOT/gms-server/src/main/resources/db/migration"
TMP_MIGRATIONS="$(mktemp)"
trap 'rm -f "$TMP_MIGRATIONS"' EXIT

find "$MIGRATION_DIR" -type f -name 'V*__*.sql' -exec basename {} \; \
  | sed -n 's/^V\([^_][^_]*\)__.*$/\1	&/p' \
  | sort > "$TMP_MIGRATIONS"

DUPLICATE_VERSIONS="$(cut -f1 "$TMP_MIGRATIONS" | uniq -d)"
if [ -n "$DUPLICATE_VERSIONS" ]; then
  echo "Flyway migration version conflict detected:" >&2
  for version in $DUPLICATE_VERSIONS; do
    echo "  Version $version:" >&2
    awk -F '\t' -v version="$version" '$1 == version { print "    - " $2 }' "$TMP_MIGRATIONS" >&2
  done
  echo "" >&2
  echo "Please give each migration a unique V*.sql version before building." >&2
  exit 1
fi

echo "Flyway migration versions OK."

cd "$ROOT/gms-ui"
yarn install --frozen-lockfile
yarn build

rm -rf "$ROOT/gms-server/src/main/resources/static"
mkdir -p "$ROOT/gms-server/src/main/resources/static"
cp -R "$ROOT/gms-ui/dist/." "$ROOT/gms-server/src/main/resources/static/"

cd "$ROOT"
mvn -B -ntp -pl gms-server -am -DskipTests package
