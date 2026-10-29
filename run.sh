#!/usr/bin/env sh
# Запуск консоли администратора anakata.
#   ./run.sh          — собрать и начать игру (параметры спросит при старте)
#   ./run.sh --help   — показать шпаргалку по параметрам
set -eu

cd "$(dirname "$0")"
mvn -q compile
exec java -cp target/classes io.github.manjago.anakata.console.ConsoleApplication "$@"
