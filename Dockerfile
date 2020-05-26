FROM maven:3.5.3-jdk-8 as builder

ARG GRAPHIUM_BRANCH_NAME=master

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
COPY --from=builder /graphium-neo4j/neo4j-server-integration/target/graphium-neo4j-server-integration-*.jar /plugins/
COPY --from=builder /graphium-neo4j/api-neo4j-plugin/target/graphium-api-neo4j-plugin-*.jar /plugins/
COPY --from=builder /graphium-neo4j/routing-neo4j-plugin/target/graphium-routing-neo4j-plugin-*.jar /plugins/
COPY --from=builder /graphium-neo4j/mapmatching-neo4j-plugin/target/graphium-mapmatching-neo4j-plugin-*.jar /plugins/
#COPY ./neo4j-server-integration/target/graphium-neo4j-server-integration-*.jar /plugins/
#COPY ./api-neo4j-plugin/target/graphium-api-neo4j-plugin-*.jar /plugins/
#COPY ./routing-neo4j-plugin/target/graphium-routing-neo4j-plugin-*.jar /plugins/
#COPY ./mapmatching-neo4j-plugin/target/graphium-mapmatching-neo4j-plugin-*.jar /plugins/

COPY ./neo4j-server-integration/doc/neo4j-default/conf/*  /var/lib/neo4j/conf/
COPY ./mapmatching-neo4j/src/test/resources/mapmatcher.properties  /var/lib/neo4j/conf/

ENV routing.astarEstimatorFactor=0.8

COPY docker-entrypoint.sh /docker-graphium-entrypoint.sh
ENTRYPOINT ["/sbin/tini", "-g", "--", "/docker-graphium-entrypoint.sh"]