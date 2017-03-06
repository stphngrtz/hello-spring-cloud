# hello-spring-cloud
Dieses Projekt ist eine Fortsetzung von [hello-spring-boot](https://github.com/stphngrtz/hello-spring-boot).

Jetzt hat mir doch tatsächlich Spring Boot so gut gefallen, dass ich bei den [Guides](https://spring.io/guides/) schon bei den Cloud-Features angekommen bin. Die klingen für den ersten Moment auch sehr spannend, so dass ich meine Erkundungstour unbedingt fortsetzen möchte. Los geht's!

## Centralized Configuration
https://spring.io/guides/gs/centralized-configuration/

Wie erstelle ich einen Config-Server?

```
@EnableConfigServer
@SpringBootApplication
public class ConfigServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}
```

Anschließend muss nur noch die Quelle der Konfigurationen angegeben werden. Im Guide wird hierzu ein lokales Git Repository verwendet. Der Pfad wird in `application.properties` angegeben.

```
spring.cloud.config.server.git.uri=${HOME}/Desktop/config
```

Dadurch, dass nur eine Git URI konfiguriert wird, weiß Spring, dass dies als Quelle verwendet werden soll. Neben Git wird auch SVN und das Dateisystem ohne Versionsverwaltung unterstützt. Details hierzu sind auf der [Projektseite](https://cloud.spring.io/spring-cloud-config/) zu finden.

Wie lese ich vom Config-Server?

```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

Die `bootstrap.properties` Datei wird geladen, bevor alle anderen Konfigurationen geladen werden. Deshalb ist das der Ort, wo der Config-Server bekannt gemacht werden muss.

```
spring.application.name=a-bootiful-client
spring.cloud.config.uri=http://localhost:8888
```

Die Werte vom Config-Server werden geladen, wenn die Anwendung startet. Wenn man das Aktualisieren von Werten zur Laufzeit erzwingen möchte, dann kommt ein **Actuator** Feature zur Hilfe, und zwar: `refresh`. Häh? Wie folgt:

```
@RefreshScope
@RestController
class MessageRestController {

    @Value("${message:Hello default}")
    private String message;

    @RequestMapping("/message")
    String getMessage() {
        return this.message;
    }
}
```

Ein Bereich der Anwendung wird mit `@RefreshScope` annotiert. Ein `curl http://localhost:8080/message` gibt jetzt *Hello default* zurück. Überschreibt man den Wert anschließend im Config-Server ändert sich hinter der Resource erstmal nichts. Dass der Config-Server die Einstellung mitbekommen hat, kann man über `curl http://localhost:8888/a-bootiful-client/default` validieren. Es fehlt noch das Refresh-Event, das mit `curl -X POST http://localhost:8080/refresh` abgeschickt wird. Anschließend sollte der Message-Endpunkt die im Config-Server hinterlegte Meldung zurückgeben.

## Routing and Filtering
https://spring.io/guides/gs/routing-and-filtering/

...

Siehe auch...
https://github.com/Netflix/zuul/wiki
http://techblog.netflix.com/2013/06/announcing-zuul-edge-service-in-cloud.html
https://tech.knewton.com/blog/2016/05/api-infrastructure-knewton-whats-edge-service/

## TODO
- https://spring.io/guides/gs/routing-and-filtering/
- https://spring.io/guides/gs/client-side-load-balancing/
- https://spring.io/guides/gs/service-registration-and-discovery/
- https://spring.io/guides/gs/circuit-breaker/
- https://spring.io/guides/tutorials/bookmarks/
