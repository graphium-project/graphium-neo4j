FROM maven:3.5.3-jdk-8 as builder

ARG GRAPHIUM_BRANCH_NAME=master

# TODO: check if still required, java fx classes should have been removed in current version
# install openjfx
RUN apt-get update \
    && apt-get install --no-install-recommends -y openjfx \
    && apt-get clean \
    && rm -f /var/lib/apt/lists/*_dists_*

# build graphium dependency
RUN git clone https://github.com/graphium-project/graphium /graphium
RUN cd /graphium && git checkout $GRAPHIUM_BRANCH_NAME && mvn -f pom.xml clean install -DskipTests

COPY . /graphium-neo4j/
RUN mvn -f /graphium-neo4j/pom.xml clean package -DskipTests -Dsource.skip


FROM neo4j:3.2.9

RUN apk add --no-cache curl tzdata

# set default value for heap size configuration
ENV NEO4J_dbms_memory_heap_initial__size "1024m"
ENV NEO4J_dbms_memory_heap_max__size "4096m"

COPY --from=builder /graphium-neo4j/neo4j-server-integration/target/graphium-neo4j-server-integration-*.jar /plugins/
COPY --from=builder /graphium-neo4j/api-neo4j-plugin/target/graphium-api-neo4j-plugin-*.jar /plugins/
COPY --from=builder /graphium-neo4j/routing-neo4j-plugin/target/graphium-routing-neo4j-plugin-*.jar /plugins/
COPY --from=builder /graphium-neo4j/mapmatching-neo4j-plugin/target/graphium-mapmatching-neo4j-plugin-*.jar /plugins/
#COPY ./neo4j-server-integration/target/graphium-neo4j-server-integration-*.jar /plugins/
#COPY ./api-neo4j-plugin/target/graphium-api-neo4j-plugin-*.jar /plugins/
#COPY ./routing-neo4j-plugin/target/graphium-routing-neo4j-plugin-*.jar /plugins/
#COPY ./mapmatching-neo4j-plugin/target/graphium-mapmatching-neo4j-plugin-*.jar /plugins/

COPY ./neo4j-server-integration/doc/neo4j-default/conf/*  /var/lib/neo4j/conf/

COPY --from=builder /graphium/converters/osm2graphium/target/osm2graphium.one-jar.jar /osm2graphium.one-jar.jar
COPY --from=builder /graphium/converters/idf2graphium/target/idf2graphium.one-jar.jar /idf2graphium.one-jar.jar

COPY docker-entrypoint.sh /docker-graphium-entrypoint.sh
ENTRYPOINT ["/sbin/tini", "-g", "--", "/docker-graphium-entrypoint.sh"]
