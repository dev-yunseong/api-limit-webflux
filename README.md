# API Rate Limiter for Spring WebFlux

This library provides a simple way to add API rate limiting to your Spring WebFlux application. It uses an in-memory storage for tracking request counts.

## Installation

To use this library, add it as a dependency to your `build.gradle` file.

```groovy
dependencies {
    implementation 'dev.yunseong:api-limit-webflux:0.1.0'
}
```

## Configuration

You can configure the rate limiting rules in your `application.yml` or `application.properties` file.

The following properties are available:

- `api-limit.rules`: A list of rate limiting rules.
- `api-limit.client-ip-header`: Header carrying the visitor address for `factor: IP`.
  Unset by default — see [Running behind a proxy](#running-behind-a-proxy).

Each rule has the following properties:

- `path`: A path pattern to match against the request path.
- `limit`: The number of allowed requests within the specified duration.
- `duration`: The time window for the rate limit (e.g., `1s`, `1m`, `1h`).
- `factor`: The criteria for rate limiting. Currently, only `IP` is supported.

### Example

Here's an example of how to configure a rate limit of 100 requests per minute for all requests to `/api/**`.

**application.yml**
```yaml
api-limit:
  rules:
    - path: /api/**
      limit: 100
      duration: 1m
      factor: IP
```

This will limit the number of requests from a single IP address to 100 per minute for all endpoints under `/api/`. If the limit is exceeded, the server will respond with a `429 Too Many Requests` status code.

## Running behind a proxy

`factor: IP` counts against the socket peer. That is the visitor only when nothing
sits in front of the application, or when the proxy is one
`server.forward-headers-strategy` recognises as trusted — which a CDN with public
edge addresses (Cloudflare, for one) is **not**.

When the proxy is not recognised, every visitor arrives with the same peer
address, shares a single bucket, and the rule's `limit` becomes a site-wide quota
instead of a per-visitor one. Nothing errors; the limiter just quietly changes
meaning.

If your proxy forwards the original address in a header, name it:

```yaml
api-limit:
  client-ip-header: CF-Connecting-IP   # X-Real-IP behind nginx, and so on
  rules:
    - path: /api/**
      limit: 100
      duration: 1m
      factor: IP
```

The header is read when it is present and holds an IP literal; otherwise the
socket peer is used. Values that are not addresses — host names, comma-separated
chains — are ignored rather than stored as-is.

> **The origin must refuse requests that bypass the proxy.** A named header is
> trustworthy because the proxy overwrites whatever the client sent — which only
> holds on the path through the proxy. Anyone who can reach the origin directly
> can otherwise send any value they like, pick their own bucket, and ignore the
> limit. Restrict inbound traffic to the proxy's address ranges, or use a tunnel.
> This is why no header is trusted unless you name one.

For anything this does not model, define a `ClientIpResolver` bean and the
auto-configuration will back off:

```kotlin
@Bean
fun clientIpResolver() = ClientIpResolver { exchange -> /* your own derivation */ }
```
