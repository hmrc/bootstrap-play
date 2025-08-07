
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

See [Logback docs](https://logback.qos.ch/manual/mdc.html) for an explanation of Mapped Diagnostic Context (MDC).

There is a MDC Filter which adds `X-Request-Id`, `X-Session-Id` and `X-Forwarded-For` headers (if present) to the MDC for inclusion in subsequent logs. It can also include the request context (e.g. `handler: GET MyController.method`) if enabled with `bootstrap.mdc.includeHandler`.

They should be available in logs from any Thread serving the request, as long as the injected `ExecutionContext` is used.

If you want to configure a custom execution context, make sure to use `uk.gov.hmrc.play.bootstrap.dispatchers.MdcPropagatingDispatcherConfigurator"` e.g.

```properties
custom-dispatcher {
  type = "uk.gov.hmrc.play.bootstrap.dispatchers.MdcPropagatingDispatcherConfigurator"
  executor = "default-executor"
  thread-pool-executor {
    fixed-pool-size = 32
  }
}
```

This Execution Context has it's MDC restored in `prepare()`, which is also called by `Promise#onComplete`, this means that MDC should flow through to all async (`Future`s) executions, including those that use Promises (e.g. mongo-scala).

There are some known limitations where MDC can still be lost.
  * Using Pekko streams.
  * Ws-client which uses it's own Execution Context
  * When invoked from Play (boundaries between Filters -> Actions -> ErrorHandlers)

In these cases, the MDC can be restored by
  * If a `RequestHeader` is in scope, any MDC for the request can be restored with

    ```scala
    uk.gov.hmrc.mdc.RequestMdc.initMdc(request.id)
    ```

    This works because the `MdcFilter` calls `uk.gov.hmrc.mdc.RequestMdc.add(request.id, mdcData)` to add MDC data, and stores it against the requestId to recover on demand.

    If adding your own MDC data, you should also use the `RequestMdc`

    ```scala
    // To add data to MDC, if you have a `RequestHeader` in scope, rather than this:
    org.slf4j.MDC.put("a" -> "key")
    // Do this instead:
    uk.gov.hmrc.RequestMdc.add(request.id, Map("a" -> "key"))
    ```
  * If no `RequestHeader` is available, you can wrap the Future block with `uk.gov.hmrc.mdc.Mdc.preservingMdc` which will effectively copy the MDC from the caller to the result of the Future block.

    ```scala
    for
      _ <- Mdc.preservingMdc(futureWhichLoosesMdc)
      _ =  logger.debug("log continues to have MDC")
    yield()
    ```

See the [mdc library](https://github.com/hmrc/mdc) for more details.

Note that bootstrap-play already does this for the default `ActionBuilder` and provided `ErrorHandler`s. If you are creating your own, you will need to ensure appropriate calls to `RequestMdc.initMdc`.

If you are creating your own `ActionBuilder` (with `new ActionBuilder`), then either ensure it is composed with the default Action Builder (`defaultActionBuilder.andThen(customActionFunction)`), or call `RequestMdc.initMdc` first thing in `invokeBlock`. Other types of `ActionFunction`s should be fine, since they require composing with a default Action Builder.

### Async activity

It is recommended to explicitly call `org.slf4j.MDC.clear()` at the beginning of any async activity (e.g. schedulers) to ensure there is no left over MDC, which can confuse the logs.

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

## Crypto

Bootstrap adds [crypto](https://github.com/hmrc/crypto) to the classpath for encryption and decryption. For frontends, it also provides `AppliationCrypto`.

When using Crypto for encrypting/decrypting data. Care should be taken to understand who owns the keys and how long the encrypted data should last.

Encryption keys provided by the platform can be rotated at any moment, and should not be used for stored data. Stored data should be encrypted with keys owned by the service itself.

Keys included for Frontends are:

- `cookie.encryption` - this is for encrypting the cookie, and should not be used for stored data.

- `queryParameter.encryption` - this can be used to encrypt query parameters for frontends. Typically callbacks and redirects. Note, it defaults to the same crypto as `cookie.encryption`, which is shared by all frontends. It should be overridden if it should be private to the service.
  The default value is owned by the Platform and could be rotated at any time, so it should not be used for stored data.

- `sso.encryption` - this is owned by the SSO Portal, and should not be used for any other purpose.

If a service requires a private key for encrypting mongo, it must create it's own. We recommend using the key name `mongodb.encryption` for consistency.

If a service requires encryption for inter-service communication, one of the services should own the key, and share it with the required services. They will be responsible for managing any necessary key rotations.

### Rotating keys

  Platform keys should only be used for the designated purpose, and may be rotated as deemed necessary.

  The Crypto library supports fields named `key` and `previousKeys`. The `key` is the key to be used for encryption. Decryption will attempt with `key` first, and fallback on any keys listed in the `previousKeys` array.

  In order to rotate a key without downtime, it is recommended to first add the new key to `previousKeys` array and redeploy.

  ```config
  my.encryption: {
    key: key1
    previousKeys: [key2]
  }
  ```

  This ensures all instances are compatible to decrypt data encrypted with either the old or new key, but still continue to encrypt with the old key.

  Then swap the keys round, and redeploy.

  ```config
  my.encryption: {
    key: key2
    previousKeys: [key1]
  }
  ```

  The instances will start to encrypt with the new key as they pick up the config.

### Dummy key

  A dummy key is provided in bootstrap config to help with local development. It can be used by a service in it's application.conf.

  ```config
  my.encryption = ${dummy.encryption}
  ```

  This avoids any leak-detection considerations, and clearly indicates that the key is only for local development.

  The dummy value is wiped (with `null`) at deployment time. The service must provide it's own encrypted value for deployment.


## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
