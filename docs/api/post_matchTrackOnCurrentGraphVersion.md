# POST match track on current graph version

## Resource URL

`http://localhost/graphium/api/matching/graphs/{graph}/versions/current/matchtrack?outputVerbose={outputVerbose}&timeoutMs={timeoutMs}&startSegmentId={startSegmentId}`

## Parameters

| **Attribut**       | **Datentyp** | **Beschreibung**                         |
| ------------------ | ------------ | ---------------------------------------- |
| **graph**          | String       | unique graph name                        |
| **outputVerbose**  | boolean      | if true resulting segment information will be enhanced by additional attributes; optional, default "false" |
| **timeoutMS**      | int          | timeout in milliseconds                  |
| **startSegmentId** | long         | ID of start segment; the map matcher tries to extend a matching path starting at this segment; needed in online matching; optional |
| **track**          | JSON         | Track in JSON format                     |

**Track's JSON format example**

```json
{"id":14079459,
 "trackPoints":
 [{
  "id":0,
  "timestamp":1462535672000,
  "x":14.2717299,
  "y":48.2733533,
  "z":279.1
 },{
  "id":0,
  "timestamp":1462535682000,
  "x":14.272935,
  "y":48.274505,
  "z":272.9
 },
 ...
 ]
}
```

## Example URL

`http://localhost:7474/graphium/api/matching/graphs/osm_at/versions/current/matchtrack?outputVerbose=true&timeoutMs=60000`

Using curl:

`curl -H "Accept: application/json" -H "Content-Type: application/json" -X POST "http://localhost:7474/graphium/api/matching/graphs/osm_at/versions/current/matchtrack?outputVerbose=true&timeoutMs=60000" -d "@/path/to/file"`

## Example Response
```json
{"segments":[{
 "segmentId":10273437,
 "startPointIndex":0,
 "endPointIndex":4,
 "enteringThroughStartNode":true,
 "leavingThroughStartNode":false,
 "startSegment":true,
 "fromPathSearch":false,
 "uTurnSegment":false,
 "weight":0.016820744773617814,
 "matchedFactor":0.0,
 "geometry":"LINESTRING (13.553598000000001 48.241205300000004, 13.549009000000002 48.2416478, 13.5443821 48.242131500000006, 13.5406752 48.2426085, 13.539036900000001 48.242862200000005, 13.5374438 48.243127900000005, 13.5342532 48.2437301, 13.5330031 48.243989400000004)",
 "name":"A8 - Innkreis Autobahn",
 "length":1562.9772,
 "maxSpeedTow":130,
 "maxSpeedBkw":-1,
 "calcSpeedTow":130,
 "calcSpeedBkw":-1,
 "lanesTow":1,
 "lanesBkw":-1,
 "frc":0,
 "formOfWay":"PART_OF_MOTORWAY",
 "wayId":0,
 "startNodeIndex":0,
 "startNodeId":85975423,
 "endNodeIndex":7,
 "endNodeId":551872931,
 "accessTow":["PRIVATE_CAR"],
 "tunnel":false,
 "bridge":false,
 "urban":false
},{
 "segmentId":43640540,
 "startPointIndex":4,
 "endPointIndex":4,
 "enteringThroughStartNode":true,
 "leavingThroughStartNode":false,
 "startSegment":false,
 "fromPathSearch":false,
 "uTurnSegment":false,
 "weight":0.006702119227956184,
 "matchedFactor":0.0,
 "geometry":"LINESTRING (13.5330031 48.243989400000004, 13.531556100000001 48.2443199, 13.5306625 48.2445217)","name":"A8 - Innkreis Autobahn",
 "length":183.64206,
 "maxSpeedTow":130,
 "maxSpeedBkw":-1,
 "calcSpeedTow":130,
 "calcSpeedBkw":-1,
 "lanesTow":1,
 "lanesBkw":-1,
 "frc":0,
 "formOfWay":"PART_OF_MOTORWAY",
 "wayId":0,
 "startNodeIndex":0,
 "startNodeId":551872931,
 "endNodeIndex":2,
 "endNodeId":551872933,
 "accessTow":["PRIVATE_CAR"],
 "tunnel":false,
 "bridge":true,
 "urban":false
  ...
}],
"finished":true,
"nrOfUTurns":0,
"nrOfShortestPathSearches":2,
"length":7544.542520359771,
"matchedFactor":0.012284162523570815,
"matchedPoints":13
}
```

