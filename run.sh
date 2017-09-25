args=""

mvn exec:java -Dexec.mainClass=org.keybase.JSONParser -Dexec.cleanupDaemonThreads=false -Dexec.args="$args"
