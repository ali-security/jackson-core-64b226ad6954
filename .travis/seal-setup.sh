# Sourced from .travis.yml before_install (do not use `set -e`: it would kill the shell).
if [ -n "$SEAL_JDK" ]; then
  echo "Installing $SEAL_JDK"
  mkdir -p /tmp/seal-jdk || return 1
  curl -fsSL --retry 5 --retry-delay 10 -o /tmp/seal-jdk.tar.gz "https://cdn.azul.com/zulu/bin/${SEAL_JDK}.tar.gz" || return 1
  tar -xzf /tmp/seal-jdk.tar.gz -C /tmp/seal-jdk --strip-components=1 || return 1
  # Zulu 7 ships an old trust store; reuse the image's JDK 8 cacerts for Maven Central TLS.
  cp "$JAVA_HOME/jre/lib/security/cacerts" /tmp/seal-jdk/jre/lib/security/cacerts || return 1
  export JAVA_HOME=/tmp/seal-jdk
  export PATH="$JAVA_HOME/bin:$PATH"
  export MAVEN_OPTS="-Dhttps.protocols=TLSv1.2"
fi
# Travis's preinstalled Maven 3.9 needs Java 8; Maven 3.8.8 still runs on Java 7.
for URL in https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.8.8/apache-maven-3.8.8-bin.tar.gz \
           https://archive.apache.org/dist/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz; do
  curl -fsSL --retry 5 --retry-delay 10 -o /tmp/maven.tar.gz "$URL" && tar -xzf /tmp/maven.tar.gz -C /tmp && break
done
[ -x /tmp/apache-maven-3.8.8/bin/mvn ] || return 1
export M2_HOME=/tmp/apache-maven-3.8.8
export PATH="$M2_HOME/bin:$PATH"
java -version
mvn -version
