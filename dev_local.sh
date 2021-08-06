#!/usr/bin/env bash
#
# ----------------------------------------------------------------------------------------------------------------------
#
# EN: How to use this script:
# 1. Make sure your JAVA_HOME points to a JRE7 or JRE8 installation
# 2. Modify the SERVER_DIR constant below to point to an existing Tekkit server directory
# 3. Run this script ( chmod +x ./dev_local.sh && ./dev_local.sh )
#
# NL: Gebruiksaanwijzing:
# 1. Zorg er voor dat JAVA_HOME naar een JRE7 of JRE8 installatie wijst
# 2. Pas de waarde van SERVER_DIR aan naar een bestaande Tekkit server map
# 3. Voer dit script uit ( chmod +x ./dev_local.sh && ./dev_local.sh )
#
# ----------------------------------------------------------------------------------------------------------------------

CURRENT_DIR="$( cd "$( dirname $0 )" && pwd )"
SERVER_DIR="$CURRENT_DIR/server"
BUILD_DIR="$CURRENT_DIR/build/libs"

# ----------------------------------------------------------------------------------------------------------------------

# Execute JAR build task (which, in turn, depends on addonJar and loaderJar tasks in build.gradle)
./gradlew -Plocal clean jar || exit

# Glob all output JAR files
GRADLE_OUTPUT_JARS=("$BUILD_DIR/"*".jar")

# Move the newly built jars into server folder
cp -v "${GRADLE_OUTPUT_JARS[@]}" "$SERVER_DIR" || exit

# Changedir into server directory
cd "$SERVER_DIR" || exit

LOADER_JAR=("$SERVER_DIR/asm-htf-loader-"*".jar")
ADDONS_JAR=("$SERVER_DIR/tekkit-server-patches-"*".jar")
TEKKIT_JAR="$SERVER_DIR/Tekkit.jar"

# Start server
# shellcheck disable=SC2128 # we only want the first args
"$JAVA_HOME"/bin/java \
    -javaagent:"$LOADER_JAR=$ADDONS_JAR" \
    -cp "$TEKKIT_JAR" \
    org.bukkit.craftbukkit.Main