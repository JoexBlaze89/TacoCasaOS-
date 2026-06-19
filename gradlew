#!/usr/bin/env sh
# Gradle start up script for UNIX
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
CLASSPATH="$DIR/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$CLASSPATH" ]; then
  echo "gradle-wrapper.jar not found. Run 'gradle wrapper' locally or add the jar to $DIR/gradle/wrapper/"
  exit 1
fi
exec java -jar "$CLASSPATH" "$@"
