## Changes

### Version 9.14.0

- Forces Jackson dependencies to version 2.15.3
- Updates playframework to 3.0.8

### Version 9.13.0

- Updates `play-auditing` which removes unnecessary "authorization" field from implicit OutboundCall audits.
- Updates dropwizard metrics dependencies.

### Version 9.11.0

- Sets `mdtpdi` cookie attribute `SameSite=None` to override default browser behaviour where it would be treated as `SameSite=Lax` when not explicitly set.

### Version 9.9.0

- Updates `auth-client` for CL600 for OLfG (One Login).
- Drops Play 2.8

### Version 9.8.0

Updates `play-auditing` to include `auditProvider` field.

### Version 9.7.0

Updates `auth-client` to fix reported Auth-Client-Version.

### Version 9.5.0

Fields to be obfuscated in audits of outbound (http-verbs) calls can be customised with `httpclient.audit.fieldMaskPattern` configuration.

### Version 9.0.0

- Cross built for Scala 3 and 2.13. Scala 2.12 has been dropped.
- Long deprecated classes have been removed. These have just changed packages, so the functionality is still available. Apart from the following, which can just be removed from configuration:
  ```properties
  play.http.filters = "uk.gov.hmrc.play.bootstrap.backend.filters.BackendFilters"
  play.http.filters = "uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters"
  ```
- `uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler` supports asynchronous rendering.

  It will need an `ExecutionContext` injecting and the `standardErrorTemplate` and `notFoundTemplate` functions now return `Future`s. If you're not rendering asyncronously, you can just wrap the previous implementation in `Future.success`

  The request param now takes `RequestHeader` rather than `Request[_]` since the request body is not actually available. This may require similar changes in view templates.

  `uk.gov.hmrc.play.bootstrap.frontend.http.LegacyFrontendErrorHandler` exists to ease the upgrade, since it can simply be extended instead. However it is deprecated, and will eventually be removed.

- HttpVerbs has been bumped to `15.0.0` where `HttpClient` has been deprecated in favour or `HttpClientV2`. This will need enabling with
  ```properties
  play.modules.enabled += "uk.gov.hmrc.play.bootstrap.HttpClientV2Module"
  ```
  See [http-verbs README](https://github.com/hmrc/http-verbs)

### Version 8.6.0
- `application-json-logger.xml` has been included. Since it is customised by configuration (by `LoggerModule`) it is no longer required in services.

### Version 8.5.0
- `JsonErrorHandler` has been updated to allow downgrading log the level of `401` status code error messages from `ERROR` to `WARN` with the `bootstrap.errorHandler.warnOnly.statusCodes` configuration.

### Version 8.4.0
- `AuthRedirects` has been removed from Play 2.9/3.0 builds. You will need to inline anything that is still needed.

### Version 8.3.0
- Injected `RegistryMetrics` is ensured to be the same as `Metrics.defaultRegistry`. Previously it would be a new one created on demand, meaning clients would have to inject `Metrics` instead.

### Version 8.0.0
- Supports Play 2.8, Play 2.9, Play 3.0
- Play 3.0:
  - Uses `apache.org.pekko` instead of `akka`.
  - The artefact organisation has changed from `com.typesafe.play` to `org.playframework` - use this to register the plugin `sbt-plugin`.
- Drops kenshoo `metrics-play` and uses an inline version of `MetricsFilter`.
  - If you need to access `com.codahale.metrics.MetricRegistry` then inject this directly, rather than injecting `com.kenshoo.play.metrics.Metrics` and calling `defaultRegistry`.
  - The following route can be removed.
    ```
    GET /admin/metrics @com.kenshoo.play.metrics.MetricsController.metrics
    ```
    It is not used for deployed services, the metrics are collected by the Graphite integration.
- Updates playframework for play-28 to 2.8.21

### Version 7.22.0
- Deprecate `SafeRedirectUrl` public constructor

### Version 7.20.0
- Updates playframework to 2.8.20

### Version 7.16.0
- Updates the Allowlist Filter
  - removes all references to Akamai
  - provides a allowlist filter implementation and no longer extends the play-allowlist-filter library
  - new error handling and logging for configuration errors.
  - updated config values
    - `bootstrap.filters.allowlist.destination` is now obsolete use `bootstrap.filters.allowlist.redirectUrlWhenDenied` instead
    - `bootstrap.filters.allowlist.ips` is now an array
    - `bootstrap.filters.allowlist.excluded` is now an array
    - exclusions can, optionally, be defined with http methods in config, for example, `METHOD:/path`
    - exclusions now support a trailing wildcard, for example, `/path/*` excludes everything _below_ /path
    - frontend.conf includes a `/ping/ping` exclusion at `bootstrap.filters.allowlist.excluded[0]`


### Version 7.15.0

- Updates default configuration in frontend.conf and backend.conf.

  The following modules are enabled by default:
  - uk.gov.hmrc.play.audit.AuditModule
  - uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule
  - uk.gov.hmrc.play.bootstrap.frontend.FrontendModule

  They can be disabled by adding to `play.modules.disabled` if required.

- `play.filters.csp.directives` for frontends

  - Tightens the play defaults slightly by removing `'unsafe-eval'` from `script-src`
  - Adds `report-uri`

- Improves default values for `microservice.metrics.graphite`

### Version 7.11.0

Updates http-verbs to fix case insensitivity of returned headers for Scala 2.13.

### Version 7.9.0

Updates playframework to 2.8.18

### Version 7.8.0

More MDC data loss fixes.

### Version 7.5.0

The `AllowlistFilter` is added by default but now requires explicit enabling.

You will need to replace `play.filters.enabled += "uk.gov.hmrc.play.bootstrap.frontend.filters.AllowlistFilter"` with `bootstrap.filters.allowlist.enabled = true`

### Version 7.4.0

- `SessionIdFilter` is enabled in `frontend.conf` by default.

Any explicit addition of this filter can now be removed.

It's position by default ensures the sessionId is available to all auditing in the first request, something which isn't true if the filter is added manually at the end of the filter chain.

- `MDCFilter` is now a trait. `FrontendMdcFilter` and `BackendMdcFilter` will be bound accordingly to ensure the the data is populated consistently.

- `http-verbs` and `play-auditing` have been updated to fix some MDC data loss.

### Version 7.0.0

#### crypto

Crypto has been updated to version 7.0.0.

If clients are using `json-encryption`, they should ensure they are using at least version `5.0.0`, or `crypto-play-xx` instead for compatibility.

See [crypto](https://github.com/hmrc/crypto).

Note, `commons-codec` is no longer provided as a transitive dependency, and will need adding explicitly if clients require it.

#### form bindings

`WithDefaultFormBinding` has been removed. `WithUnsafeDefaultFormBinding` should be used instead.

### Version 6.0.0

#### http-verbs

Http-verbs has been updated to version 14.0.0, which adds `HttpClientV2`. See [http-verbs](https://github.com/hmrc/http-verbs) for details.

#### Auditing of truncated payloads

http-verbs's `HookData` has been updated to identify when audit data has been truncated. This should have little impact on clients as long as they rely on `bootstrap-play` to provide compatible versions of `http-verbs` and `play-auditing`.

`AuditFilter`s have been updated to identify when audit data has been truncated. Most clients will be using the provided AuditFilters and should not be impacted.


### Version 5.24.0

Builds update dependencies to Play 2.8.15 and Jackson 2.12.6.  Also drops support for using `bindFormRequest` with `application/json` and
`application/multipart` content types - if this is required, add `with WithUrlEncodedAndMultipartFormBinding` or `with WithDefaultFormBinding` to your
controller.

Note, that in unit tests, you will need to use `withFormUrlEncodedBody` rather than `withJsonBody`. And also ensure you are setting the correct HTTP Method, otherwise you may fall foul of CORS checks. e.g.
```scala
FakeRequest().withMethod("POST").withFormUrlEncodedBody("username" -> username)
```

Please speak to PlatOps if you have issues.

### Version 5.21.0

Builds for Scala 2.13 in addition to 2.12

### Version 5.20.0

Adds the following configuration for `JsonErrorHandler`:

- `bootstrap.errorHandler.suppress4xxErrorMessages`
- `bootstrap.errorHandler.suppress5xxErrorMessages`

It is expected that suppression will be disabled for at least development, since they provide valuable feedback, but verbose messages can be suppressed if required.

### Version 5.19.0

Drops support for Play 2.6 and Play 2.7. Only Play 2.8 is supported.

### Version 5.18.0

Adds `uk.gov.hmrc.play.bootstrap.LoggerModule`.

This has been enabled by default in `backend.conf` and `frontend.conf`, but it can be disabled with `play.modules.disabled += "uk.gov.hmrc.play.bootstrap.LoggerModule"` in your `application.conf` if required.

The LoggerModule will pick up `logger.$loggername` configuration (e.g. `logger.uk.gov.hmrc = DEBUG`) in any configuration file, not just in `System.properties`. The configuration will be applied, regardless of whether they have been defined in your logback file.

e.g. default values will suffice in your logback file without preping to use a System property:

`<logger name="uk.gov" level="${logger.uk.gov:-WARN}"/>`

can be replaced with

`<logger name="uk.gov" level="WARN"/>`

And to enable a bespoke logger, not present in the logback file, you can just turn on with configuration.
e.g. `-Dlogger.uk.gov.test=DEBUG` will set logger `uk.gov.test` to `DEBUG` without having to make a new build with updated logback file first.

### Version 5.0.0

#### auth-client

Added Identity Verification (IV) CL250 support via auth-client 5.6, see [May 2021 Tech Blog Post](https://confluence.tools.tax.service.gov.uk/display/TEC/2021/05) on this.

### Version 4.0.0

#### http-verbs

Http-verbs has been bumped to major version 13.0.0. See [http-verbs]("https://github.com/hmrc/http-verbs") for details.

#### configuration

The following configuration has been renamed, the previous keys are invalid and will need to be updated.

| Invalid config key                      | Should now be                           |
| --- | --- |
| auditing.auditExtraHeaders              | auditing.auditSentHeaders               |

#### Filters

`FrontendFilters` and `BackendFilters` have been deprecated. The preferred way to set filters is via `play.filters.enabled`.

The motivation for this is to allow configuration of default Play filters via `play.filters.enabled` as per the Play documentation, and improve visibility of which filters are actually being used.

If you are using `play.http.filters = uk.gov.hmrc.play.bootstrap.backend.filters.BackendFilters`, you will only need to remove this setting, since backend.conf has defined `play.filters.enabled` to the same filters.

If you are using `play.http.filters = uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters`, you will need to remove this setting, but also change the following configurations:

| Deprecated config key                   | Change
| --- | --- |
| `security.headers.filter.enabled=true`         | Just remove - this is the default
| `security.headers.filter.enabled=false`        | Replace with `play.filters.disabled += "play.filters.headers.SecurityHeadersFilter"`
| `bootstrap.filters.csrf.enabled=true`          | Just remove - this is the default
| `bootstrap.filters.csrf.enabled=false`         | Replace with  `play.filters.disabled += "play.filters.csrf.CSRFFilter"`
| `bootstrap.filters.sessionId.enabled=true`     | Replace with  `play.filters.enabled += "uk.gov.hmrc.play.bootstrap.frontend.filters.SessionIdFilter"`
| `bootstrap.filters.sessionId.enabled=false`    | Just remove - this is the default
| `bootstrap.filters.allowlist.enabled=true`     | Still required
| `bootstrap.filters.allowlist.enabled=false`    | Just remove - this is the default

If you have set `play.http.filters` to a custom filter, we recommend that you remove this, and use `play.filters.enabled` instead. You can append extra filters with `play.filters.enabled += ` or remove filters with `play.filters.disabled += `.

If you are using a custom filter which injects the `play.api.http.EnabledFilters`, or are already using the default `play.api.http.EnabledFilters`, then pay extra attention since the default `play.filters.enabled` has now been defined differently by bootstrap.


### Version 3.0.0

#### http-verbs

Http-verbs has been bumped to major version 12.0.0. See [http-verbs]("https://github.com/hmrc/http-verbs") for details.

#### configuration

The following configuration has been renamed, the previous keys are invalid and will need to be updated.

| Invalid config key                      | Should now be                           |
| --- | --- |
| httpHeadersWhitelist                    | bootstrap.http.headersAllowlist         |
| bootstrap.filters.whitelist.enabled     | bootstrap.filters.allowlist.enabled     |
| bootstrap.filters.whitelist.destination | bootstrap.filters.allowlist.destination |
| bootstrap.filters.whitelist.excluded    | bootstrap.filters.allowlist.excluded    |
| bootstrap.filters.whitelist.ips         | bootstrap.filters.allowlist.ips         |
| auditing.auditExtraHeaders              | auditing.auditSentHeaders               |

### From bootstrap-play-26

#### RunMode was removed

`run.mode` configuration will now have no effect.

Bootstrap configuration which previously supported a `RunMode` prefix now should be specified without the prefix. These are:

| Invalid Prod/Dev/Test prefix | Should now be |
| --- | --- |
| Prod.microservice.services | microservice.services |
| Prod.microservice.metrics  | microservice.metrics  |
| Prod.auditing              | auditing              |

This applies to application.conf and any configuration overrides

Note: `Environment.Mode` still works as before, and some libraries may still use that as a prefix in their keys.

#### CSRFExceptionsFilter was removed

It was deprecated in 2017, and the original use-cases no longer appear valid.

If you still need the functionality offered by this filter, please copy the code from bootstrap-play-26 into your own codebase (including the tests) and take ownership of it.
