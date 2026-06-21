#!/bin/bash
# Build rk-user service

export JAVA_HOME="C:/Users/Administrator/AppData/Local/Programs/IntelliJ IDEA/jbr"
export PATH="$JAVA_HOME/bin:$PATH"

MAVEN_HOME="C:/Users/Administrator/AppData/Local/Programs/IntelliJ IDEA/plugins/maven/lib/maven3"

cd "C:/Users/Administrator/IdeaProjects/RK-Web/rk-user"

echo "Building rk-user..."
echo "JAVA_HOME: $JAVA_HOME"

"$MAVEN_HOME/bin/mvn.cmd" clean package -DskipTests

echo "Done. Exit code: $?"
