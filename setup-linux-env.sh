#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-beidou}"
DB_USER="${DB_USER:-beidou}"
DB_PASSWORD="${DB_PASSWORD:-beidou}"
RUN_AFTER_BUILD=1
SKIP_INSTALL=0
SKIP_DB=0

usage() {
  cat <<EOF
Usage: sh setup-linux-env.sh [options]

Options:
  --no-run        Install, configure DB, and build, but do not start the server.
  --skip-install  Skip apt package installation.
  --skip-db       Skip MySQL database/user setup.
  --help          Show this help.

Environment variables:
  DB_HOST         Default: localhost
  DB_PORT         Default: 3306
  DB_NAME         Default: beidou
  DB_USER         Default: beidou
  DB_PASSWORD     Default: beidou

Example:
  sh setup-linux-env.sh --no-run
  DB_USER=beidou DB_PASSWORD=beidou123 sh setup-linux-env.sh
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --no-run)
      RUN_AFTER_BUILD=0
      ;;
    --skip-install)
      SKIP_INSTALL=1
      ;;
    --skip-db)
      SKIP_DB=1
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
  shift
done

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1"
    exit 1
  fi
}

sudo_cmd() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    sudo "$@"
  fi
}

install_packages() {
  if [ "$SKIP_INSTALL" -eq 1 ]; then
    echo "[setup] Skip package installation."
    return
  fi

  if ! command -v apt-get >/dev/null 2>&1; then
    echo "This setup script currently supports Ubuntu/Debian apt-based systems."
    echo "Install JDK 21, Maven, Node.js, Yarn, and MySQL manually, then run:"
    echo "  sh setup-linux-env.sh --skip-install"
    exit 1
  fi

  echo "[setup] Installing base packages..."
  sudo_cmd apt-get update
  sudo_cmd apt-get install -y ca-certificates curl gnupg build-essential maven mysql-server

  if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q 'version "21'; then
    echo "[setup] Installing JDK 21..."
    if apt-cache show openjdk-21-jdk >/dev/null 2>&1; then
      sudo_cmd apt-get install -y openjdk-21-jdk
    else
      sudo_cmd install -d -m 0755 /etc/apt/keyrings
      curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
        | sudo_cmd gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
      . /etc/os-release
      echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb ${VERSION_CODENAME} main" \
        | sudo_cmd tee /etc/apt/sources.list.d/adoptium.list >/dev/null
      sudo_cmd apt-get update
      sudo_cmd apt-get install -y temurin-21-jdk
    fi
  fi

  if ! command -v node >/dev/null 2>&1 || ! node -e "process.exit(Number(process.versions.node.split('.')[0]) >= 14 ? 0 : 1)" >/dev/null 2>&1; then
    echo "[setup] Installing Node.js 20..."
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo_cmd bash -
    sudo_cmd apt-get install -y nodejs
  fi

  if ! command -v yarn >/dev/null 2>&1; then
    echo "[setup] Installing Yarn 1.22.22..."
    sudo_cmd npm install -g yarn@1.22.22
  fi
}

start_mysql() {
  if command -v systemctl >/dev/null 2>&1; then
    sudo_cmd systemctl enable --now mysql
  else
    sudo_cmd service mysql start
  fi
}

configure_mysql() {
  if [ "$SKIP_DB" -eq 1 ]; then
    echo "[setup] Skip MySQL database setup."
    return
  fi

  echo "[setup] Configuring MySQL database: $DB_NAME"
  start_mysql

  sql_file="$(mktemp)"
  trap 'rm -f "$sql_file"' EXIT

  if [ "$DB_USER" = "root" ]; then
    cat > "$sql_file" <<EOF
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER USER 'root'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER 'root'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO 'root'@'localhost';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO 'root'@'127.0.0.1';
FLUSH PRIVILEGES;
EOF
  else
    cat > "$sql_file" <<EOF
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
CREATE USER IF NOT EXISTS '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER '$DB_USER'@'127.0.0.1' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'localhost';
GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'127.0.0.1';
FLUSH PRIVILEGES;
EOF
  fi

  if sudo_cmd mysql < "$sql_file"; then
    echo "[setup] MySQL configured with sudo mysql."
  elif mysql -h "$DB_HOST" -P "$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" < "$sql_file"; then
    echo "[setup] MySQL configured with DB_USER credentials."
  else
    echo "Failed to configure MySQL."
    echo "Check MySQL root permissions, then rerun with --skip-install if packages are already installed."
    exit 1
  fi
}

verify_tools() {
  need_cmd java
  need_cmd mvn
  need_cmd node
  need_cmd yarn
  need_cmd mysql

  echo "[setup] Versions:"
  java -version
  mvn -version | sed -n '1,2p'
  node --version
  yarn --version
}

build_server() {
  echo "[setup] Building BeiDou server..."
  cd "$ROOT"
  sh "$ROOT/build-linux.sh"
}

run_server() {
  echo "[setup] Starting BeiDou server..."
  echo "[setup] Web admin: http://localhost:8686/"
  echo "[setup] Game login port: 8484"
  DB_HOST="$DB_HOST" DB_PORT="$DB_PORT" DB_NAME="$DB_NAME" DB_USER="$DB_USER" DB_PASSWORD="$DB_PASSWORD" \
    sh "$ROOT/run-linux-dev.sh"
}

install_packages
configure_mysql
verify_tools
build_server

if [ "$RUN_AFTER_BUILD" -eq 1 ]; then
  run_server
else
  echo "[setup] Done. Start later with:"
  echo "  DB_USER=$DB_USER DB_PASSWORD=$DB_PASSWORD sh run-linux-dev.sh"
fi
