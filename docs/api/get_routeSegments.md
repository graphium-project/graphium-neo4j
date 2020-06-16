# GET route segments

Get route as geometry and path. A path consists of several ID/direction-pairs.

## Resource URL

`http://localhost/graphium/api/routing/graphs/{graph}/route?coords={startX},{startY};{endX},{endY}&mode={routingMode}&algo={routingAlgo}&criteria={routingCriteria}&time={timestamp}&output=path`

## Parameters

| attribute           | type                                                         | description                                                  |
| ------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **graph**           | String                                                       | unique graph name                                            |
| **startX**          | double                                                       | start X coordinate                                           |
| **startY**          | double                                                       | start Y coordinate                                           |
| **endX**            | double                                                       | end X coordinate                                             |
| **endY**            | double                                                       | end Y coordinate                                             |
| **routingMode**     | String                                                       | mode for routing; values are: bike, car, pedestrian, pedestrian_barrierfree; optional, default "car" |
| **routingAlgo**     | boolean                                                      | name of routing algorithm to use; supported values are: dijkstra, bidirectional_dijkstra; optional |
| **routingCriteria** | String                                                       | route for minimal length or duration; values are: length, min_duration, current_duration; optional, default "length" |
| **timestamp**       | String (ISO-8601 format or in UNIX timestamp in milliseconds) | timestamp for selecting fitting graph version                |

## Example URL

`http://localhost:7474/graphium/api/routing/graphs/osm_at/route?coords=13.043516,47.812558;13.044208,47.812287&time=2020-06-10T12:00:00&mode=car&criteria=length&output=path`

## Example Response
```json
{"route":  
 {"weight":null,
  "length":147.4177,
  "duration":10,
  "runtimeInMs":194,
  "graphName":"osm_at",
  "graphVersion":null,
  "geometry":"LINESTRING (13.043502439876509 47.81256718801095, 13.0436858 47.812837800000004, 13.0437224 47.812893, 13.0437382 47.812848800000005, 13.043785900000001 47.8128207, 13.0439938 47.812758, 13.0443133 47.8126617, 13.0443653 47.8126314, 13.0443835 47.8126022, 13.044388600000001 47.812555700000004, 13.0442896 47.8123877, 13.044220748718725 47.812278930070875)",
  "segments"
  [{"id":374121070,"linkDirectionForward":true,
   {"id":259075831,"linkDirectionForward":true},
   {"id":37517506,"linkDirectionForward":false}
  ]
 }
}
```

