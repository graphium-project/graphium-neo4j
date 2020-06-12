# GET route segments

## Resource URL

`http://localhost/graphium/api/metadata/graphs/{graph}/routing/getRoute.do?timestamp={timestamp}&startX={startX}&startY={startY}&endX={endX}&endY={endY}&cutsegments={cutsegments}&routingMode={routingMode}&routingCriteria={routingCriteria}`

## Parameters

| **Attribut**        | **Datentyp**                | **Beschreibung**                         |
| ------------------- | --------------------------- | ---------------------------------------- |
| **graph**           | String                      | unique graph name                        |
| **timestamp**       | String (pattern yyyy-MM-dd) | timestamp for selecting fitting graph version |
| **startX**          | double                      | start X coordinate                       |
| **startY**          | double                      | start Y coordinate                       |
| **endX**            | double                      | end X coordinate                         |
| **endY**            | double                      | end Y coordinate                         |
| **cutsegments**     | boolean                     | if true start and end segments will be cut at requested coordinate; optional, default "true" |
| **routingMode**     | String                      | mode for routing; values are: BIKE, CAR, PEDESTRIAN, PEDESTRIAN_BARRIERFREE; optional, default "PEDESTRIAN" |
| **routingCriteria** | String                      | route for minimal length or duration; values are: LENGTH, MIN_DURATION, CURRENT_DURATION; optional, default "LENGTH" |

## Example URL

`http://localhost:7474/graphium/api/graphs/osm_at/routing/getRouteSegments.do?timestamp=2020-06-10&startX=13.043516&startY=47.812558&endX=13.044208&endY=47.812287&routingMode=CAR&routingCriteria=length`

## Example Response
```json
[{
  "segmentId":374121070,
  "geometry":"LINESTRING (13.043502439876509 47.81256718801095, 13.0436858 47.812837800000004, 13.0437224 47.812893)",
  "length":39.7864,
  "duration":3,
  "maxSpeed":50
},{
  "segmentId":259075831,
  "geometry":"LINESTRING (13.0437224 47.812893, 13.0437382 47.812848800000005, 13.043785900000001 47.8128207, 13.0439938 47.812758, 13.0443133 47.8126617, 13.0443653 47.8126314, 13.0443835 47.8126022, 13.044388600000001 47.812555700000004)",
  "length":66.931305,
  "duration":5,
  "maxSpeed":50
},{
  "segmentId":37517506,
  "geometry":"LINESTRING (13.044220748718725 47.812278930070875, 13.0442896 47.8123877, 13.044388600000001 47.812555700000004)",
  "length":32.835537,
  "duration":2,
  "maxSpeed":50
}]
```

