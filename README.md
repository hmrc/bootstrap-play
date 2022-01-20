
# bootstrap-play


![](https://img.shields.io/github/v/release/hmrc/bootstrap-play)


This library implements basic functionality required by the platform frontend/microservices.


## Adding to your build

In your SBT build add:

```scala
resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")

// for frontends:
libraryDependencies += "uk.gov.hmrc" %% "bootstrap-frontend-play-xx" % "x.x.x"

// for backends:
libraryDependencies += "uk.gov.hmrc" %% "bootstrap-backend-play-xx" % "x.x.x"
```
Where `play-xx` is your version of Play (e.g. `play-28`).

You can also add the test module

```scala
libraryDependencies += "uk.gov.hmrc" %% "bootstrap-test-play-xx" % "x.x.x" % Test
```
which will provide the appropriate version of other test dependencies like `http-verbs-test` and `scalatestplus-play`.


## Configure as a frontend microservice

In your application.conf file, add:

```properties
include "frontend.conf"

# An ApplicationLoader that uses Guice to bootstrap the application.
play.application.loader = "uk.gov.hmrc.play.bootstrap.ApplicationLoader"

# Primary entry point for all HTTP requests on Play applications
play.http.requestHandler = "uk.gov.hmrc.play.bootstrap.http.RequestHandler"

# Provides an implementation of AuditConnector. Use `uk.gov.hmrc.play.audit.AuditModule` or create your own.
# An audit connector must be provided.
play.modules.enabled += "uk.gov.hmrc.play.audit.AuditModule"

# Provides an implementation of MetricsFilter. Use `uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule` or create your own.
# A metric filter must be provided
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule"

# Provides an implementation and configures all filters required by a Platform frontend microservice.
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.frontend.FrontendModule"

```

And in your SBT build add:

```scala
libraryDependencies += "uk.gov.hmrc" %% "play-frontend-hmrc" % "x.x.x"
```

## Configure as a backend microservice

In your application.conf file, add:

```properties
include "backend.conf"

# An ApplicationLoader that uses Guice to bootstrap the application.
play.application.loader = "uk.gov.hmrc.play.bootstrap.ApplicationLoader"

# Primary entry point for all HTTP requests on Play applications
play.http.requestHandler = "uk.gov.hmrc.play.bootstrap.http.RequestHandler"

# Provides an implementation of AuditConnector. Use `uk.gov.hmrc.play.audit.AuditModule` or create your own.
# An audit connector must be provided.
play.modules.enabled += "uk.gov.hmrc.play.audit.AuditModule"

# Provides an implementation of MetricsFilter. Use `uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule` or create your own.
# A metric filter must be provided
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule"

# Provides an implementation and configures all filters required by a Platform backend microservice.
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.backend.BackendModule"

```

## Default HTTP client

A default http client with pre-configured auditing hook can be injected into any connector. The http client uses http-verbs
For more http-verbs examples see https://github.com/hmrc/http-verbs-example


Make sure you have the following modules in your application.conf file:

```properties
play.modules.enabled += "uk.gov.hmrc.play.audit.AuditModule"
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.HttpClientModule"
```


```scala
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import javax.inject.Inject

class SomeConnector @Inject() (client: HttpClient) {

  client.GET[Option[MyCaseClass]]("http://localhost/my-api")
}
```

## User Authorisation

The library supports user authorisation on microservices

Make sure you have the following modules in your application.conf file:

```
play.modules.enabled += "uk.gov.hmrc.play.audit.AuditModule"
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.HttpClientModule"
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.AuthModule"
```

Your controller will look like this:
```scala
class MyController @Inject() (val authConnector: AuthConnector) extends BaseController with AuthorisedFunctions {

   def getSomething(): Action[AnyContent] = Action.async { implicit request ⇒
       authorised() {
         // your protected logic
       }
   }
 }
```

## MDC Logging

By default the logging MDC will be passed between threads by a custom `ExecutorService`.
While this works in both test and production configurations it _does not work_ in `Dev`
mode using the `AkkaHttpServer` in Play 2.6.

If you would like the same functionality in `Dev` mode, you must use the older
`NettyHttpServer`.

* Enable the `PlayNettyServer` plugin in your `build.sbt`
```scala
  .enablePlugins(PlayNettyServer)
```

* Set the `NettyServerProvider` in the `devSettings` of your `build.sbt`
```scala
  PlayKeys.devSettings += "play.server.provider" -> "play.core.server.NettyServerProvider"
```

* Make sure you still use the `AkkaHttpServer` in `Prod` mode by specifying it in `application.conf`
```hocon
play.server.provider = play.core.server.AkkaHttpServerProvider
```

## Changes

### Version 5.20.0

Adds the following configuration for `JsonErrorHandler`:

- `bootstrap.errorHandler.suppress4xxErrorMessages`
- `bootstrap.errorHandler.suppress5xxErrorMessages`

It is expected that these will be enabled for at least development but can be disabled if required.

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
| `bootstrap.filters.allowlist.enabled=true`     | Replace with  `play.filters.enabled += "uk.gov.hmrc.play.bootstrap.frontend.filters.AllowlistFilter"`
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

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
