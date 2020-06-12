#!/bin/bash -eu

if [[ -v GRAPHIUM_SERVER_NAME ]]; then
  sed -i "s~^graphium\.server\.name=.*~graphium.server.name=${GRAPHIUM_SERVER_NAME}~" /var/lib/neo4j/conf/server.properties
fi

if [[ -v GRAPHIUM_SERVER_URI ]]; then
  sed -i "s~^graphium\.server\.uri=.*~graphium.server.uri=${GRAPHIUM_SERVER_URI}~" /var/lib/neo4j/conf/server.properties
fi

sed -i "s~.*dbms\.unmanaged\_extension\_classes=.*~dbms.unmanaged_extension_classes=at.srfg.graphium.neo4j.bootstrap=/graphium/api~" /var/lib/neo4j/conf/neo4j.conf
grep -q '^dbms\.jvm.additional=-Dgraphium\.conf\.' /var/lib/neo4j/conf/neo4j.conf || echo 'dbms.jvm.additional=-Dgraphium.conf.path=file:conf/' >> /var/lib/neo4j/conf/neo4j.conf

exec /sbin/tini -s -- /docker-entrypoint.sh neo4j