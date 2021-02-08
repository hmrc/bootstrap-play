
# bootstrap-play



This library implements basic functionality required by the platform frontend/microservices.


## Adding to your build

In your SBT build add:

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

// for frontends:
libraryDependencies += "uk.gov.hmrc" %% "bootstrap-frontend-play-26" % "x.x.x"

// for backends:
libraryDependencies += "uk.gov.hmrc" %% "bootstrap-backend-play-26" % "x.x.x"
```

## Configure as a frontend microservice

In your application.conf file, add:

```properties
include "frontend.conf"

# An ApplicationLoader that uses Guice to bootstrap the application.
play.application.loader = "uk.gov.hmrc.play.bootstrap.ApplicationLoader"

# Primary entry point for all HTTP requests on Play applications
play.http.requestHandler = "uk.gov.hmrc.play.bootstrap.http.RequestHandler"

# Provides an implementation of AuditConnector. Use `uk.gov.hmrc.play.bootstrap.AuditModule` or create your own.
# An audit connector must be provided.
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.AuditModule"

# Provides an implementation of MetricsFilter. Use `uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule` or create your own.
# A metric filter must be provided
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule"

# Provides an implementation and configures all filters required by a Platform frontend microservice.
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.frontend.FrontendModule"

```

And in your SBT build add:

```scala
libraryDependencies += "uk.gov.hmrc" %% "play-ui" % "x.x.x"
libraryDependencies += "uk.gov.hmrc" %% "govuk-template" % "x.x.x"
```

## Configure as a backend microservice

In your application.conf file, add:

```properties
include "backend.conf"

# An ApplicationLoader that uses Guice to bootstrap the application.
play.application.loader = "uk.gov.hmrc.play.bootstrap.ApplicationLoader"

# Primary entry point for all HTTP requests on Play applications
play.http.requestHandler = "uk.gov.hmrc.play.bootstrap.http.RequestHandler"

# Provides an implementation of AuditConnector. Use `uk.gov.hmrc.play.bootstrap.AuditModule` or create your own.
# An audit connector must be provided.
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.AuditModule"

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
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.AuditModule"
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
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.AuditModule"
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

## Migrations

### Version 4.0.0

#### http-verbs

Http-verbs has been bumped to major version 13.0.0. See [http-verbs]("https://github.com/hmrc/http-verbs") for details.

#### Filters

`FrontendFilters` and `BackendFilters` have been deprecated. The preferred way to set filters is via `play.filters.enabled`.

The motivation for this is to allow configuration of default Play filters via `play.filters.enabled` as per the Play documentation, and improve visibility of which filters are actually being used.

If you are using `play.http.filters = uk.gov.hmrc.play.bootstrap.backend.filters.BackendFilters`, you will only need to remove this setting, since backend.conf has defined `play.filters.enabled` to the same filters.

If you are using `play.http.filters = uk.gov.hmrc.play.bootstrap.backend.filters.FrontendFilters`, you will need to remove this setting, but also change the following configurations:

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
