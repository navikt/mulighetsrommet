# IAC (Infrastructure as Code) for Mulighetsrommet

Mulighetsrommet bruker Kafka for å tilgjengeliggjøre data på topics som andre konsumenter kan benytte seg av.
I denne mappen man våre oppsett av kafka-topics og kafka-manager.

## Kafka topics

Topics ligger i mappen `/kafka-topics` og inneholder konfigurasjon for dev-miljø og prod-miljø.

### Jeg vil koble meg på topic lokalt for feilsøking - Hvordan gjør jeg det?

For å koble seg på en topic lokalt kan man bruke [kcat](https://github.com/edenhill/kcat). Kcat trenger en kcat.conf-fil
som man får på følgende måte:

```bash
nais aiven create kafka <username> team-mulighetsrommet -p <pool> -s <some-unique-secretname>
```

Følgende verdier må settes:

- **username** -> Et username du selv
  velger. [Dette må matche med topicens acl-liste](https://doc.nais.io/cli/commands/aiven/#kafka)
- **pool** -> enten nav-prod eller nav-dev
- **some-unique-secretname** -> En eller annen tilfeldig tekststreng

Når kommandoen over er kjørt får du en sti i terminalen du kan bruke for å referere til korrekt kcat.conf-fil.

Eks: `kcat -F path/to/kcat.conf -t team-mulighetsrommet.<topic-navn>` der **topic-navn** er topicene du skal feilsøke.

I tillegg må du konfigurere og deploye topic med oppdatert
acl. [Mer info finner man her](https://doc.nais.io/cli/commands/aiven/#kafka).

Når du er ferdig med feilsøking kjører du `nais aiven tidy` for å slette temp-filer.

__Ressurser__:

- https://docs.aiven.io/docs/products/kafka/howto/kcat.html
- https://doc.nais.io/cli/commands/aiven/#kafka
- https://doc.nais.io/persistence/kafka/

### Deployment av topics

Deployment av topic skjer automatisk via Github Actions når man merger kode relatert til topicens yaml-fil på
main-branchen.

## Kafka manager

Kafka manager er en applikasjon som kan nås på følgende url'er:

**Dev**: https://mulighetsrommet-kafka-manager.intern.dev.nav.no
**Prod**: https://mulighetsrommet-kafka-manager.intern.nav.no

Man kan bruke manageren for å lese meldinger på topics som er definert i konfigurasjonen.

Deployment av kafka-manager skjer via Github Actions ved merge til main-branch.
