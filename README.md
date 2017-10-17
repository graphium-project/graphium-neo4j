<p align="right">
<img src="docs/img/Graphium_225x130px.svg">
</p>

# Graphium Neo4j

Graphium Neo4j is an extension of the project Graphium based on Neo4j. Neo4j is a famous graph database which comes with various graph algorithms. Neo4j's graph model is built for answering graph dependent questions in a more flexible and performant way than you can do with a relational database. As transport graphs can be modeled as graphs and stored in Neo4j, we benefit from its features.

Graphium Neo4j is built for those who want to manage graphs and graph versions within Neo4j and / or need routing or even a map matching API.

## Neo4j Server Plugins

Graphium Neo4j consists of modules which must be deployed as Neo4j Server plugins (so called unmanaged extensions). Therefore Graphium Neo4j is not a standalone server but requires a Neo4j Server.

### Deployment

Following plugins have to be built and deployed in the Neo4j's *plugins* directory:

- `graphium-neo4j-server-integration-plugin-XXX.jar` (Graphium's core functionality and integration into Neo4j)
- `graphium-api-neo4j-plugin-XXX.jar` (API)
- `graphium-routing-neo4j-plugin-XXX.jar` (routing functionality and API)
- `graphium-mapmatching-neo4j-plugin-XXX.jar` (map matching functionality and API)

### Configuration of Graphium

Graphium Neo4j needs some properties files that must be copied in Neo4j's *conf* directory:

- graphVersionCapacities.properties
- import.properties
- log4j.xml
- mapmatcher.properties
- neo4j_db.properties
- server.properties

The following properties within server.properties have to be adapted. Be careful at changing other property values.

- graphium.server.name
- graphium.server.uri



Default property files can be found in [neo4j-server-integration/doc/neo4j-default/conf](neo4j-server-integration/doc/neo4j-default/conf).

### Configuration of Neo4j

Graphium Neo4j plugins has to be registered in Neo4j Server. Therefore, *neo4j.conf* in Neo4j's *conf* directory has to be extended by:

- `dbms.unmanaged_extension_classes=at.srfg.graphium.neo4j.bootstrap=/graphium`
- `dbms.jvm.additional=-Dgraphium.conf.path=file:conf/`
- `graphium.secured=true/false`

## Routing

Graphium Neo4j's routing API handles routing requests between two coordinates on a transport graph. The result will be returned in a JSON format.

### API

- [GET route](docs/api/get_route.md)
- [GET route on graph version](docs/api/get_routeOnGraphVersion.md)
- [GET route segments](docs/api/get_routeSegments.md)
- [GET route segments on graph version](docs/api/get_routeSegmentsOnGraphVersion.md)

### Examples

A visualized route calculated by Graphium Neo4j's routing engine.

<p align="center">
<img src="docs/img/routing_1.JPG" width="800">
<br/>
<a href="https://www.basemap.at/">Map: basemap.at</a>
</p>

## Map Matching

Graphium Neo4j's map matching API matches trajectories onto a transport graph. In contrast to Hidden Markov Model implementations, Graphium Neo4j's map matching core implementation is built for processing trajectories having a high sampling rate (<20 seconds). The main focus was to enable a high-performance processing of those high-sampled trajectories with a very low error ratio. Also low-sampled trajectories can be processed, but with an increasing error ratio. With Graphium's ability of working in distributed systems, you can improve the performance by horizontal scaling. Graphium Neo4j's map matching supports offline and online map matching.

### API

- [POST match track](docs/api/post_matchTrack.md)
- [POST match track on current graph version](docs/api/post_matchTrackOnCurrentGraphVersion.md)

### Examples

Map Matching of a lower sampled track (for better visualization). Blue point show GPS track points, red linestring represents the map matched path on graph (thin black linestrings).

<p align="center">
<img src="docs/img/mapmatching_2.JPG" width="800">
<br/>
<a href="https://www.basemap.at/">Map: basemap.at</a>
</p>

Map Matching of a track whose GPS track points partially could not have been matched onto graph. The map matcher detects non matchable parts of the track and splits the map matched path.

<p align="center">
<img src="docs/img/mapmatching_1.JPG" width="800">
<br/>
<a href="https://www.basemap.at/">Map: basemap.at</a>
</p>

## Plugins Development

You can build your own Neo4j plugins based on Graphium Neo4j. Graphium Neo4j plugins use Spring Framework, which defines configuration information within ApplicationContexts. For integration of custom Neo4j plugins into Graphium Neo4j only one ApplicationContext with the name pattern `application-context-graphium-neo4j-plugin*.xml` has to be provided. All Beans defined within this ApplicationContext will be loaded automatically by Graphium Neo4j.

## Dependencies

- Caffeine cache, Apache License, Version 2.0 (https://github.com/ben-manes/caffeine)
- Guava: Google Core Libraries for Java, Apache License, Version 2.0 (http://code.google.com/p/guava-libraries/guava)
- ConcurrentLinkedHashMap, Apache License, Version 2.0 (http://code.google.com/p/concurrentlinkedhashmap)
- Netty/All-in-One, Apache License, Version 2.0 (http://netty.io/netty-all/)
- opencsv, Apache License, Version 2.0 (http://opencsv.sf.net)
- Apache Commons Collections, Apache License, Version 2.0 (http://commons.apache.org/proper/commons-collections/)
- Apache Commons Compress, Apache License, Version 2.0 (http://commons.apache.org/proper/commons-compress/)
- Lucene Common Analyzers, Apache License, Version 2.0 (http://lucene.apache.org/lucene-parent/lucene-analyzers-common)
- Lucene Memory, Apache License, Version 2.0 (http://lucene.apache.org/lucene-parent/lucene-backward-codecs)
- Lucene codecs, Apache License, Version 2.0 (http://lucene.apache.org/lucene-parent/lucene-codecs)
- Lucene Core, Apache License, Version 2.0 (http://lucene.apache.org/lucene-parent/lucene-core)
- Lucene QueryParsers, Apache License, Version 2.0 (http://lucene.apache.org/lucene-parent/lucene-queryparser)
- Bouncy Castle PKIX, CMS, EAC, TSP, PKCS, OCSP, CMP, and CRMF APIs, Bouncy Castle Licence (http://www.bouncycastle.org/java.html)
- Bouncy Castle Provider, Bouncy Castle Licence (http://www.bouncycastle.org/java.html)
- Neo4j - Community, GPLv3 (http://components.neo4j.org/neo4j/3.2.3)
- Neo4j - Code Generator, GPLv3 (http://components.neo4j.org/neo4j-codegen/3.2.3)
- Neo4j - Collections, GPLv3 (http://components.neo4j.org/neo4j-collections/3.2.3)
- Neo4j - Command Line, GPLv3 (http://components.neo4j.org/neo4j-command-line/3.2.3/parent/neo4j-command-line)
- Neo4j - Common, GPLv3 (http://components.neo4j.org/neo4j-common/3.2.3)
- Neo4j - Configuration, GPLv3 (http://components.neo4j.org/neo4j-configuration/3.2.3)
- Neo4j - Consistency Checker, GPLv3 (http://components.neo4j.org/neo4j-consistency-check/3.2.3)
- Neo4j - CSV reading and parsing, GPLv3 (http://components.neo4j.org/neo4j-csv/3.2.3)
- Neo4j - Cypher, GPLv3 (http://components.neo4j.org/neo4j-cypher/3.2.3)
- Neo4j - Cypher Compiler 2.3, GPLv3 (http://components.neo4j.org/neo4j-cypher-compiler-2.3/2.3.11)
- Neo4j - Cypher Compiler 3.1, GPLv3 (http://components.neo4j.org/neo4j-cypher-compiler-3.1/3.1.5)
- Neo4j - Cypher Compiler 3.2, GPLv3 (http://components.neo4j.org/neo4j-cypher-compiler-3.2/3.2.3)
- Neo4j - Cypher Frontend 2.3, GPLv3 (http://components.neo4j.org/neo4j-cypher-frontend-2.3/2.3.11)
- Neo4j - Cypher Frontend 3.1, GPLv3 (http://components.neo4j.org/neo4j-cypher-frontend-3.1/3.1.5)
- Neo4j - Cypher Frontend 3.2, GPLv3 (http://components.neo4j.org/neo4j-cypher-frontend-3.2/3.2.3)
- Neo4j - Cypher Intermediate Representation 3.2, GPLv3 (http://components.neo4j.org/neo4j-cypher-ir-3.2/3.2.3)
- Neo4j - Database Management System, GPLv3 (http://components.neo4j.org/neo4j-dbms/3.2.3/)
- Neo4j - Graph Algorithms, GPLv3 (http://components.neo4j.org/neo4j-graph-algo/3.2.3)
- Neo4j - Graph Matching, GPLv3 (http://components.neo4j.org/neo4j-graph-matching/3.1.3)
- Neo4j - Graph Database API, GPLv3 (http://components.neo4j.org/neo4j-graphdb-api/3.2.3)
- Neo4j - Import Command Line Tool, GPLv3 (http://components.neo4j.org/neo4j-import-tool/3.2.3)
- Neo4j - Native index, GPLv3 (http://components.neo4j.org/neo4j-index/3.2.3)
- Neo4j - IO, GPLv3 (http://components.neo4j.org/neo4j-io/3.2.3)
- Neo4j - JMX support, GPLv3 (http://components.neo4j.org/neo4j-jmx/3.2.3)
- Neo4j - Graph Database Kernel, GPLv3 (http://components.neo4j.org/neo4j-kernel/3.2.3)
- Neo4j - Logging, GPLv3 (http://components.neo4j.org/neo4j-logging/3.2.3)
- Neo4j - Lucene Index, GPLv3 (http://components.neo4j.org/neo4j-lucene-index/3.2.3)
- Neo4j - Lucene Index Upgrade, GPLv3 (http://components.neo4j.org/neo4j-lucene-upgrade/3.2.3/parent/neo4j-lucene-upgrade)
- Neo4j - Primitive Collections, GPLv3 (http://components.neo4j.org/neo4j-primitive-collections/3.2.3)
- Neo4j - Resource interface, GPLv3 (http://components.neo4j.org/neo4j-resource/3.2.3)
- Neo4j - SSL, GPLv3 (http://components.neo4j.org/neo4j-ssl/3.2.3)
- Neo4j - Usage Data Collection, GPLv3 (http://components.neo4j.org/neo4j-udc/3.2.3)
- Neo4j - Unsafe Access, GPLv3 (http://components.neo4j.org/neo4j-unsafe/3.2.3)
- ASM Core, BSD (http://asm.objectweb.org/asm/)
- parboiled-core, Apache License, Version 2.0 (http://parboiled.org)
- parboiled-scala, Apache License, Version 2.0 (http://parboiled.org)
- Scala Library, BSD 3-Clause (http://www.scala-lang.org/)
- Scala Compiler, BSD 3-Clause (http://www.scala-lang.org/)