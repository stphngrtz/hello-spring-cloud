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

Routing im Edge-Service mittels Netflix Zuul. Zuerst benötigen wir einen oder mehrere Services. Hier nutze ich einfach den im Centralized Configuration Guide erstellten Greetings-Service. Anschließend will der Edge-Service erstellt werden. Wie nicht anders zu erwarten war, ist das denkbar einfach. Eine Dependency auf `spring-cloud-starter-zuul` hinzufügen, eine Spring-Boot-Application aufsetzen, die zusätzlich mit `@EnableZuulProxy` annotiert ist und anschließend noch die Routen konfigurieren. Dies geschieht in der `application.properties`.

```
server.port=8080
zuul.routes.greetings.url=http://localhost:8090
ribbon.eureka.enabled=false
```

Zuul arbeitet mit sog. Filtern. In die Details will ich im Rahmen dieses Guides nicht eingehen. Vielleicht erstelle ich hierzu demnächt man ein separates Repository - bis dahin muss die [offizielle Dokumentation](https://github.com/Netflix/zuul/wiki) herhalten. Ein solcher Filter sieht jedenfalls wie folgt aus:

```
public class SimpleFilter extends ZuulFilter {

    private static final Logger log = LoggerFactory.getLogger(SimpleFilter.class);

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        log.info("{} request to {}", request.getMethod(), request.getRequestURL().toString());
        return null;
    }
}
```

Startet man die Anwendung, findet man die Endpunkte des Greetings-Service über den Edge-Service hinter der Route `greetings`. So wird zum Beispiel aus http://localhost:8090/greeting ein http://localhost:8080/greetings/greeting.

Siehe auch:
- http://techblog.netflix.com/2013/06/announcing-zuul-edge-service-in-cloud.html
- https://tech.knewton.com/blog/2016/05/api-infrastructure-knewton-whats-edge-service/

## TODO
- https://spring.io/guides/gs/client-side-load-balancing/
- https://spring.io/guides/gs/service-registration-and-discovery/
- https://spring.io/guides/gs/circuit-breaker/
- https://spring.io/guides/tutorials/bookmarks/
