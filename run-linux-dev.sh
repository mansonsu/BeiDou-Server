#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$ROOT/gms-server"

JAVA_HOME="$ROOT/jdk-21.0.2"
if [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "JDK 21 not found or not executable: $JAVA_HOME" >&2
  exit 1
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-beidou}"
DB_USER="${DB_USER:-beidou}"
DB_PASSWORD="${DB_PASSWORD:-beidou}"
DB_URL="${DB_URL:-jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai}"

"$JAVA_HOME/bin/java" -Dspring.config.location="$ROOT/gms-server/src/main/resources/application.yml" \
  -jar "$ROOT/gms-server/target/BeiDou.jar" \
  --mybatis-flex.datasource.mysql.url="$DB_URL" \
  --mybatis-flex.datasource.mysql.username="$DB_USER" \
  --mybatis-flex.datasource.mysql.password="$DB_PASSWORD"
