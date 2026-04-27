#!/bin/sh
set -eu

mkdir -p /var/log/hang-anatomy
chown -R 1001:1001 /var/log/hang-anatomy

JAVA_BIN="${JAVA_HOME:-/opt/java/openjdk}/bin/java"

exec su -s /bin/sh appuser -c "exec ${JAVA_BIN} ${JAVA_OPTS:-} -jar /app/app.jar"
