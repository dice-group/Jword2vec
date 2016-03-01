export MAVEN_OPTS=-Xmx10G
nohup mvn exec:java -Dexec.mainClass="org.aksw.word2vecrestful.Application" -Dexec.cleanupDaemonThreads=false > run.log &
