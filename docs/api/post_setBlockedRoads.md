# POST (set) road blocks

For each graph it is possible to define road blocks. They will be specified via segment-ID and direction and optionally its validity. This request sets a complete list of road blocks for one graph.

## Resource URL

`http://localhost/graphium/api/blockedroads/graphs/{graph}`

## Parameters

| **Attribut** | **Datentyp** | **Beschreibung**  |
| ------------ | ------------ | ----------------- |
| **graph**    | String       | unique graph name |

**Road blocks JSON format example**

The attributes validFrom and validTo are optional. If validFrom is null, its value will be set to the current timestamp. If validTo is null or is to high its value will be set to the current timestamp plus maxTTL (defined in routing.properties).

```json
{"segments": 
 [{
  "id" : 901417346,
  "linkDirectionForward" : false,
  "validFrom" : "2021-04-20 09:30:00",
  "validTo" : "2021-04-20 10:00:00"
 },
 {
 ...
 }
 ]
}
```

## Example URL

`http://localhost/graphium/api/blockedroads/graphs/osm_at`

## Response

The count of stored and segment entities.