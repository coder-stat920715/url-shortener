# URL Shortening Service (Spring Boot 3.3, Java 17)

Microservices reference implementation: Cassandra (mappings), Redis + Caffeine (cache and rate limit),
Kafka (click events), ClickHouse (analytics), ZooKeeper (key-range allocation).

| Module | Port | Role |
|---|---|---|
| `common` | - | Base62, Feistel key scrambler, RFC 7807 problem factory, shared DTOs |
| `kgs-service` | 8081 | Leases ID ranges from ZooKeeper, returns scrambled 7-char Base62 keys (internal API) |
| `url-creation-service` | 8082 | `POST /api/v1/urls`: validation, custom alias (Cassandra LWT), Redis token-bucket limiter |
| `redirection-service` | 8083 | WebFlux `GET /{key}`: L1 Caffeine, Redis, Cassandra, async Kafka click event, 301/302/404/410 |
| `analytics-ingestion-service` | 8084 | Kafka batch consumer, enrichment, bulk insert to ClickHouse |
| `api-gateway` | 8080 | Spring Cloud Gateway: `/api/**` to creation, `/{key}` to redirection |

## Run everything
```bash
docker compose up --build        # first build takes several minutes
```

## Try it
```bash
# create (generated key)
curl -s -X POST localhost:8080/api/v1/urls -H 'Content-Type: application/json' \
  -d '{"longUrl":"https://www.wikipedia.org/"}'

# custom alias with expiry, permanent (301) redirect
curl -s -X POST localhost:8080/api/v1/urls -H 'Content-Type: application/json' \
  -d '{"longUrl":"https://www.wikipedia.org/","customAlias":"my-link","expiresAt":"2030-01-01T00:00:00Z","permanent":true}'

# redirect
curl -i localhost:8080/my-link

# duplicate alias -> 409 problem+json, expired -> 410, unknown -> 404
curl -i -X POST localhost:8080/api/v1/urls -H 'Content-Type: application/json' \
  -d '{"longUrl":"https://example.org","customAlias":"my-link"}'

# analytics
curl -s 'localhost:8123/?query=SELECT+short_key,count()+FROM+clicks_raw+GROUP+BY+short_key'
```

## Run locally without Docker for the apps
Start only the infrastructure, then run each service from your IDE or `mvn -pl <module> spring-boot:run`:
```bash
docker compose up zookeeper kafka redis cassandra cassandra-init clickhouse
```
Default `application.yml` values point to `localhost`. Start order: `kgs-service`, then `url-creation-service`,
`redirection-service`, `analytics-ingestion-service`, `api-gateway`.

## Build and test
```bash
mvn clean verify        # runs Base62 and KeyScrambler unit tests in `common`
```

## Notes and production hardening
- **Key uniqueness:** `KeyScrambler` is a keyed bijection over [0, 62^7). Counter IDs are unique, so keys never collide.
  Keep `KGS_SCRAMBLER_SEED` secret and never change it after launch.
- **Auth:** the creation service reads an optional `X-User-Id` header. Add JWT validation at the gateway and set that
  header from the verified subject. A `user-account-service` (PostgreSQL, JPA) is not included in this archive.
- **Geo enrichment:** `ClickEnricher` leaves country as `XX`. Add MaxMind GeoLite2 (`com.maxmind.geoip2:geoip2`) there.
- **Scale settings:** Cassandra `NetworkTopologyStrategy` RF=3, Redis Cluster, Kafka 48 partitions RF=3, ClickHouse sharding.
- **Kafka DLT:** the analytics consumer retries 3 times and then skips the batch. Add a `DeadLetterPublishingRecoverer`.
- **Link safety:** `UrlValidator` has a static blocklist plus an SSRF guard. Integrate Google Safe Browsing asynchronously
  and set `status = 2` in Cassandra to flag a link; the redirect service then returns 451.
- **301 vs 302:** default is 302 (accurate analytics, enforceable expiry). `permanent=true` gives 301 with a 1-day cache.
