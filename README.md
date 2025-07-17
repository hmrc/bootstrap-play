
# bootstrap-play


![](https://img.shields.io/github/v/release/hmrc/bootstrap-play)


This library implements basic functionality required by the platform frontend/microservices.

See [CHANGELOG](CHANGELOG.md) for changes.

Built for:
- Play 3.0 - Scala 2.13 and Scala 3
- Play 2.9 - Scala 2.13


## Adding to your build

In your SBT build add:

```scala
resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")

// for frontends:
libraryDependencies += "uk.gov.hmrc" %% "bootstrap-frontend-play-xx" % "x.x.x"

// for backends:
libraryDependencies += "uk.gov.hmrc" %% "bootstrap-backend-play-xx" % "x.x.x"
```
Where `play-xx` is your version of Play (e.g. `play-30`).

You can also add the test module

```scala
libraryDependencies += "uk.gov.hmrc" %% "bootstrap-test-play-xx" % "x.x.x" % Test
```
which will provide the appropriate versions of other test dependencies like `http-verbs-test` and `scalatestplus-play`.


## Configure as a frontend microservice

In your application.conf file, add:

```properties
include "frontend.conf"

appName = "my-frontend"
```

## Configure as a backend microservice

In your application.conf file, add:

```properties
include "backend.conf"

appName = "my-backend"
```

## Default Frontend Error Handling
Frontend services should implement their own ErrorHandler by extending `FrontendErrorHandler` and overriding `standardErrorTemplate` to ensure page titles and content meet the organisation’s guidance - for example, the [HMRC page title guidance](https://design.tax.service.gov.uk/hmrc-design-patterns/page-title/).

## Default HTTP clients

Two http clients are available and can be injected into any connector by enabling the appropriate modules.

### uk.gov.hmrc.http.HttpClient

This is the original http client provided by http-verbs.

To use, enable the following module in your application.conf file:

```properties
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.HttpClientModule"
```

example usage:

```scala
import uk.gov.hmrc.http.HttpClient
import javax.inject.Inject

class SomeConnector @Inject() (httpClient: HttpClient) {

  httpClient.GET[Option[MyCaseClass]]("http://localhost/my-api")
}
```

### uk.gov.hmrc.http.client.HttpClientV2

This is a new http client provided by http-verbs which supports streaming and has a more flexible API, making it simpler to enable Proxies etc.

To use, enable the following modules in your application.conf file:

```properties
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.HttpClientV2Module"
```

example usage:

```scala
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import javax.inject.Inject

class SomeConnector @Inject() (httpClientV2: HttpClientV2) {

  httpClientV2.get(url"http://localhost/my-api").execute[Option[MyCaseClass]]
}
```

Note, you can safely enable both http clients.

For documentation and more usage examples see [http-verbs](https://github.com/hmrc/http-verbs)

## User Authorisation

The library supports user authorisation on microservices

Make sure you have the following modules in your application.conf file:

```properties
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.HttpClientModule"
play.modules.enabled += "uk.gov.hmrc.play.bootstrap.AuthModule"
```

Your controller will look like this:
```scala
class MyController @Inject() (val authConnector: AuthConnector) extends BaseController with AuthorisedFunctions {

   def getSomething(): Action[AnyContent] = Action.async { implicit request =>
       authorised() {
         // your protected logic
       }
   }
 }
```

## Logging

When run with `-Dlogger.resource=/application-json-logger.xml`, as is the case for deployed services, it will use
the referenced log configuration rather than `logback.xml`. This logger logs as XML to the standard out, for integration with log pipelines - see [logback-json-logger](https://github.com/hmrc/logback-json-logger). It also includes Mapped Diagnostic Context (MDC) such as `X-Request-ID` and `X-Session-ID` (populated by `MDCFilter`) to aid tracing journeys through logs. See [MDC Logging](#mdc-logging) for testing this locally.

You can use the `application-json-logger.xml` as provided by bootstrap - since it can be customised by standard application configuration (applied by the `LoggerModule`).

E.g.
```hocon
logger.root                     = WARN # set the root logger level
logger.uk.gov                   = INFO # set the level for all uk.gov loggers
logger.uk.gov.myservice.myclass = DEBUG # set the level for a specific logger. If using DEBUG then be as precise as necessary
```

Note, default logger configurations assume that packages are fully qualified and use `getClass` for name. E.g. `package uk.gov.hmrc.myservice; class MyClass { val logger = play.api.Logger(getClass) }` If you use packages with shorthand names like `package controllers` then it is harder to configure app wide loggers etc. you may get package conflicts, as well as it not being clear where a resource is coming from when importing.

## MDC Logging

By default the logging Mapped Diagnostic Context (MDC) will be passed between threads by a custom `ExecutorService`.

As long as you inject the `ExecutionContext`, MDC should flow through to all logs. However it will be lost at async boundaries - the most notable is when using the mongo driver, which uses Reactive streams - e.g.


```scala
for {
  ...
  logger.log("Message with MDC")
  _ <- mongoCollection.find().toFuture()
  logger.log("Message without MDC") // MDC is lost here
} yield ()
```

This can be wrapped with `uk.gov.hmrc.mdc.Mdc.preservingMdc`, e.g.

```scala
for {
  ...
  logger.log("Message with MDC")
  _ <- MDC.preservingMdc(mongoCollection.find().toFuture())
  logger.log("Message with MDC")
} yield ()
```

Or with

If you are configuring a custom execution context, make sure to use `uk.gov.hmrc.play.bootstrap.dispatchers.MDCPropagatingExecutorServiceConfigurator` e.g.

```properties
custom-dispatcher {
  type = Dispatcher
  executor = "uk.gov.hmrc.play.bootstrap.dispatchers.MDCPropagatingExecutorServiceConfigurator"
  thread-pool-executor {
    fixed-pool-size = 32
  }
}
```

### Testing MDC logging


### Logging MDC loss

There is a `bootstrap.mdcdataloss.warn.enabled` configuration, which can be enabled. It will log a warning if MDC data added by `MdcFilter` is lost by the time the response is returned. Turning this on can help identify if MDC data is going missing. It's probably sufficient to just turn this on in QA. The cause of MDC loss is most likely that `Mdc.preservingMdc` is required across an async boundary.

Given there is always some loss due to thread optimisations in Play (e.g. use of Pekko's FastFuture), alerts will not be generated until they exceed a configured threshold (`bootstrap.mdcdataloss.warn.thresholdPercent`).

## Allow List Filter

The library includes a frontend Filter that can block requests from IP addresses that are not on an _allow list_.

To enable this filter, in your frontend service, add the following line to your application.conf file:

`bootstrap.filters.allowlist.enabled = true`

The filter will check the ip address supplied in the `True-Client-IP` http header against a list of allowed ip addresses.

When enabled the following configuration properties are used:

| Property                                              | Type             | Mandatory | Default        | Desription                                                                                                                                                                                                                                 |
|-------------------------------------------------------|------------------|-----------|----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `bootstrap.filters.allowlist.ips`                     | array of strings | Y         | -              | Allows requests from any one of these ip addresses                                                                                                                                                                                         |
| `bootstrap.filters.allowlist.excluded`                | array of strings | N         | ["/ping/ping"] | Irrespective of IP address the filter will not block access to any of these paths<br/>See example below for defintion.                                                                                                                     |
| `bootstrap.filters.allowlist.redirectUrlWhenDenied`   | string           | Y         | -              | The filter will redirect any denied requests to this URL<br/>Note:<br/>-  **if** this URL is relative to your service it must also be on the excluded list.<br/>- replaces the obsolete property `bootstrap.filters.allowlist.destination` |


### Configuring Exclusions

#### Http Methods
Exclusions by default are defined with the implicit Http `GET` method. If exclusions are required for other methods
they can be defined with the following syntax: `"<method>:/path"`<br/>
for example:

```hocon
"/some/path" # this is the equivalent of "GET:/some/path"
"PUT:/some/path"
"POST:/other/path"
"DELETE:/some/path"
```

#### Wildcard Mathching

Exclusion definitions by default are exact matches. By adding a trailing `/*` then they will match all paths below that level.<br/>
for example: `bootstrap.filters.allowlist.excluded = ["/admin/*"]` matches all Uri starting with `/admin`

### Configuration Example

application.conf

```hocon
bootstrap.filters.allowlist.enabled = true
bootstrap.filters.allowlist.ips = ["192.168.2.1", "10.0.0.1"]
bootstrap.filters.allowlist.excluded +=  "/some/path"
bootstrap.filters.allowlist.excluded +=  "/some/other/path"
bootstrap.filters.allowlist.excluded += "POST:/admin/*"
bootstrap.filters.allowlist.redirectUrlWhenDenied = "http://www.gov.uk"
```

## RedirectUrl

Urls provided as query parameters (redirect urls, callbacks etc.) should be modelled with `RedirecUrl`. This allows configuration of allowed destinations and helps prevent Open Redirects.

e.g.

- `build.sbt` - import model and query binder for use in routes:

```scala
.settings(
  RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
)
```

- `app.routes` - use `RedirectUrl`

```scala
/redirect MyController.redirect(redirectUrl: RedirectUrl)
```

- Controller

The Url provided to controller will be wrapped with `RedirectUrl`, and can be checked against the required policy before using.

E.g. the following will only accept relative urls

```scala
def redirect(redirectUrl: RedirectUrl): Action[AnyContent] =
  Action {
    Redirect(redirectUrl.get(OnlyRelative).url)
  }
```

Or accept configured hostnames

```scala
val redirectUrlPolicy = AbsoluteWithHostnameFromAllowlist("safehost1", "safehost2") // provide from configuration

def redirect(redirectUrl: RedirectUrl): Action[AnyContent] =
  Action {
    Redirect(redirectUrl.get(redirectUrlPolicy).url)
  }
```

Note, if the url does not conform to the policy, this will lead to a 500. To have more control over this behaviour, you can use `getEither` instead.

```scala
def redirect(redirectUrl: RedirectUrl): Action[AnyContent] =
  Action {
    redirectUrl
      .getEither(OnlyRelative)
      .fold(BadRequest("Bad url"))(safeUrl => Redirect(safeUrl.url))
  }
```


## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
