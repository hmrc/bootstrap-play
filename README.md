
# bootstrap-play


![](https://img.shields.io/github/v/release/hmrc/bootstrap-play)


This library implements basic functionality required by the platform frontend/microservices.

See [CHANGELOG](CHANGELOG.md) for changes.

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
which will provide the appropriate version of other test dependencies like `http-verbs-test` and `scalatestplus-play`.


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
the referenced log configuration rather than `logback.xml`. This logger logs as XML to the standard out, for integration with log pipelines - see [logback-json-logger](https://github.com/hmrc/logback-json-logger). It also includes MDC such as `X-Request-ID` and `X-Session-ID` (populated by `MDCFilter`) to aid tracing journeys through logs. See [MDC Logging](#mdc-logging) for testing this locally.

You can use the `application-json-logger.xml` as provided by bootstrap - since it can be customised by standard application configuration (applied by the `LoggerModule`).

E.g.
```hocon
logger.root                     = WARN # set the root logger level
logger.uk.gov                   = INFO # set the level for all uk.gov loggers
logger.uk.gov.myservice.myclass = DEBUG # set the level for a specific logger. If using DEBUG then be as precise as necessary
```

Note, default logger configurations assume that packages are fully qualified and use `getClass` for name. E.g. `package uk.gov.hmrc.myservice; class MyClass { val logger = play.api.Logger(getClass) }` If you use packages with shorthand names like `package controllers` then it is harder to configure app wide loggers etc. you may get package conflicts, as well as it not being clear where a resource is coming from when importing.

## MDC Logging

By default the logging MDC will be passed between threads by a custom `ExecutorService`.
While this works in both test and production configurations it _does not work_ in `Dev`
mode using the `AkkaHttpServer`.

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

* Note, this will add Netty to the classpath for builds, so you may want to only do this temporarily. If not, make sure you still use the `AkkaHttpServer` in `Prod` mode by specifying it in `application.conf`
```hocon
play.server.provider = play.core.server.AkkaHttpServerProvider
```

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
