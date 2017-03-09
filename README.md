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

## Client Side Load Balancing with Ribbon and Spring Cloud
https://spring.io/guides/gs/client-side-load-balancing/

Client Side Load Balancing stellt den klassischen Ansatz in Frage, bei dem eine dedizierte Instanz das Load Balancing übernimmt. Dadurch gibt es den Load Balancer als Bottleneck bzw. Single Point of Failure nicht mehr und grundsätzlich ist eine solche Lösung auch im Punkt Skalierbarkeit eine Naselänger weiter vorne.

Siehe auch:
- https://thenewstack.io/baker-street-avoiding-bottlenecks-with-a-client-side-load-balancer-for-microservices/

Als Beispiel für den Guide ist der Compliments-Service entstanden, der via Ribbon Load-Balancing den Greetings-Service aufruft.

### Konfiguration
In der [application.yml](https://github.com/stphngrtz/hello-spring-cloud/blob/master/compliments-service/src/main/resources/application.yml) werden die Services definiert. In diesem Fall geht die Service Discovery nicht über Eureka sondern über eine fest definierte Liste inkl. Refresh-Intervall. Eureka zu verwenden ist definitiv zu empfehlen, aber dazu komme ich erst im nächsten Guide ;)

```
...
greetings:
  ribbon:
    eureka:
      enabled: false
    listOfServers: localhost:8090,localhost:9092,localhost:9999
    ServerListRefreshInterval: 15000
```

### Anbindung
Angebunden wird Ribbon dort, wo die Webservice-Calls gemacht werden, in meinem Fall ist das [App.java](https://github.com/stphngrtz/hello-spring-cloud/blob/master/compliments-service/src/main/java/de/stphngrtz/spring/cloud/compliments/App.java).

```
@RestController
@SpringBootApplication
@RibbonClient(name = "greetings", configuration = GreetingsConfiguration.class)
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Autowired
    RestTemplate restTemplate;

    @LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @RequestMapping(value = "/compliment")
    public String compliment() {
        String greeting = restTemplate.getForObject("http://greetings/greeting", String.class);
        return String.format("%s. %s!", greeting, "You look amazing");
    }
}
```

Durch `@RibbonClient` wird dem Kontext ein Client bekannt gemacht. Der Name muss zu dem in der Konfiguration passen. Durch `@LoadBalanced` erfährt Spring, dass die Webservice-Calls dieser Schnittstelle durch einen Load Balancer laufen sollen. Schlussendlich muss die URL nur noch durch den Namen des Clients ersetzt werden und wir sind fertig... fast fertig. Dem aufmerksamen Leser wird die `GreetingsConfiguration` nicht entgangen sein. Ribbon ist mit gewissen Defaults vorbelegt. So werden zum Beispiel die URLs nicht geprüft. In Verbindung mit Eureka ist das auch nicht norwendig, aber da wir hier eine feste Liste von URLs angegeben haben wäre schon interessant, ob dahinter denn ein Service verfügbar ist.

```
public class GreetingsConfiguration {

    @Bean
    IPing ribbonPing() {
        return new PingUrl();
    }

    @Bean
    IRule ribbonRule() {
        return new AvailabilityFilteringRule();
    }
}
```

Durch das Codebeispiel wird ein Ping zum Testen der Service URLs, sowie der in Ribbon integrierte Curcuit Breaker für die Verwaltung der Verfügbarkeit aktiviert.

## Service Registration and Discovery
https://spring.io/guides/gs/service-registration-and-discovery/

Bei diesem Guide wurde es doch tatsächlich etwas holprig. Warum? Zwei Gründe:
- Lücken im Guide bei der Konfiguration. Wie erfärt denn der Client, unter welchem Port der Server läuft?
- Verwirrung meinerseits bzgl. der Dependencies.

Auf den zweiten Punkt will ich kurz eingehen. Bereits in den vorherigen Guides war ich mir nicht ganz sicher, wie das mit den Cloud-Dependencies läuft. Brauche ich diese *Dependencies-Dependency*? Zuvor habe ich die letzte Version per Hand eingetragen. Bei Eureka geht das theoretisch auch, aber dann startet der Server nicht. Fehlendes Web-Modul, oder so ähnlich. Ich hätte einfach der [Spring-Cloud Dokumentation](http://projects.spring.io/spring-cloud/) glauben sollen, die Dependencies werden wie folgt eingebunden:

```
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>Camden.SR5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-eureka-server</artifactId>
    </dependency>
</dependencies>
```

So, nachdem das geklärt ist, wie implementiert man einen Eureka-Server bzw. -Client? Die Anwendung bekommt eine `@EnableEurekaServer` Annotation und zusätzlich ein paar Informationen über die Konfigurationsdatei.

```
server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

Schon ist der Server fertig. Der Client ist genauso leicht aufzusetzen. `@EnableDiscoveryClient` an die Anwendung und ebenfalls ein paar Informationen via Konfigurationsdatei.

```
spring:
  application:
    name: eureka-client

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    preferIpAddress: true
```

Der Eureka-Server bringt von Haus aus eine Weboberfläche mit (http://localhost:8761/), auf der der Zustand der Service Registry zu sehen ist. In [ServiceInstanceController.java](https://github.com/stphngrtz/hello-spring-cloud/blob/master/eureka-client/src/main/java/de/stphngrtz/spring/cloud/eureka/ServiceInstanceController.java) sieht man beispielhaft, wie man auf die Service Discovery zugreift. Die Klasse stellt einen REST-Endpunkt bereits, über den man Informationen zu Services abfragen kann (zB. http://localhost:8080/service-instances/eureka-client).

### Fazit
Die Vorteile davon, den Server in eine eigene Anwendung zu wrappen, erschließen sich mir noch nicht ganz. Intuitiv würde ich die Service Registry als dediziertes System laufen lassen. Da ich mich aber über diesen Guide hinaus noch nicht mit Eureka bzw. Spring Cloud Eureka beschäftigt habe, kommt die Erleuchtung vielleicht, wenn ich denn genau dies mache.

Die unterschiedliche Art und Weise der Versionierung finde ich noch nicht ganz glücklich gewählt. Explizite Versionen mit Zahlen bei Spring Boot, eine Import-Dependency mit Codenamen bei Spring Cloud. Wahrscheinlich gibt es gute Gründe dafür - als Einsteiger ist das erstmal etwas gewöhnungsbedürftig.

## TODO
- https://spring.io/guides/gs/circuit-breaker/
- https://spring.io/guides/tutorials/bookmarks/
- https://spring.io/guides/gs/messaging-redis/
- https://spring.io/guides/gs/messaging-rabbitmq/

Siehe auch
- https://blog.de-swaef.eu/the-netflix-stack-using-spring-boot/
