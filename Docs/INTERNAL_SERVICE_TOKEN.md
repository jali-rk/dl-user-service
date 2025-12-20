# Internal Service Token (userservice)

This document explains the internal service token used for service-to-service calls into `userservice`. Share this with the BFF team to ensure correct usage.

## What it is
A static shared secret used to authenticate internal requests. Requests must include the header:

- Header name: `X-Service-Token`
- Header value: the configured token

If the header is missing or the value doesn not match, `userservice` returns `401 Unauthorized`.

## Configuration (userservice)
The token is configured via the property `user.service.internalToken` and can be injected from an environment variable.

- `application-dev.yml` (already present):

```yaml
user:
  service:
    internalToken: ${INTERNAL_SERVICE_TOKEN:change-me-in-production}
```

- Environment variable (recommended in non-dev):

```bash
export INTERNAL_SERVICE_TOKEN="super-secret-token-value"
```

Alternatively, set directly (not recommended for production):

```yaml
user:
  service:
    internalToken: super-secret-token-value
```

## How BFF should call userservice
Include the header on any internal endpoint call:

```bash
curl -sS \
  -H "X-Service-Token: super-secret-token-value" \
  "https://userservice.internal/api/internal/example"
```

In code (example, JavaScript/Node):

```js
await fetch("https://userservice.internal/api/internal/example", {
  headers: {
    "X-Service-Token": process.env.INTERNAL_SERVICE_TOKEN,
  },
});
```

## Rotation and rollout
- Generate a new strong random value (32+ bytes) periodically.
- Update the secret source (e.g., Kubernetes Secret, Vault, AWS Secrets Manager).
- Deploy userservice with the new value.
- Coordinate BFF rollout to start sending the new header value.
- Optionally support a short dual-token window if needed.

## Security recommendations
- Do not commit real tokens to source control.
- Prefer storing in a secret manager and injecting via env vars.
- Limit token scope to internal network; avoid exposing internal endpoints externally.
- Consider stronger alternatives for high-security contexts: mTLS, signed HMAC headers, or JWT with audience claims.

## Troubleshooting
- 401 Unauthorized: verify header presence and exact value; check env var on both sides.
- Local dev: set `INTERNAL_SERVICE_TOKEN` in your shell or use the default in `application-dev.yml`.
- Logs: server may log unauthorized attempts; tail logs when debugging.

