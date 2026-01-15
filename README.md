# API Rate Limiter for Spring WebFlux

This library provides a simple way to add API rate limiting to your Spring WebFlux application. It uses an in-memory storage for tracking request counts.

## Installation

To use this library, add it as a dependency to your `build.gradle` file.

```groovy
dependencies {
    implementation 'dev.yunseong:api-limit-webflux:0.0.1'
}
```

## Configuration

You can configure the rate limiting rules in your `application.yml` or `application.properties` file.

The following properties are available:

- `api-limit.rules`: A list of rate limiting rules.

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