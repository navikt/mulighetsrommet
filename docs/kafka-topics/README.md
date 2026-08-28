# Kafka topics for eksterne konsumenter

Dette dokumentet gir en oversikt over Kafka topics som er tilgjengelige fra dette repoet. Noen topics er forbeholdt
spesifikke team/formål, mens andre kan tilgjengeliggjøres til flere konsumenter etter behov.

En del topics har [log compaction](https://docs.confluent.io/kafka/design/log_compaction.html) aktivert, slik at kun
siste
versjon av hver nøkkel beholdes.

---

## siste-tiltakstyper-v3

- Inneholder informasjon om alle tiltakstyper som administreres i Tiltaksadministrasjon.
- Gir en mapping mellom `tiltakskode` som benyttet i Tiltaksadministrasjon og `arenaKode` (som er tiltakskoden fra
  Arena).
- Oppdateres ved:
    - Endringer på tiltakstypen gjort av Nav-ansatte (f.eks. redigering av informasjon for veiledere eller deltakere).
    - Manuell relast av tiltakstyper fra databasen.
- **Log compaction:** Aktivert
- **Modell:** [`TiltakstypeV3Dto`](../../mulighetsrommet-api/contracts/src/main/kotlin/no/nav/mulighetsrommet/api/contracts/tiltakstype/TiltakstypeV3Dto.kt)

---

## siste-tiltaksgjennomforinger-v2

- Inneholder informasjon om alle tiltaksgjennomføringer i Tiltaksadministrasjon, både gruppetiltak og enkeltplasser.
- `tiltakskode` referer til tiltakstypen som gjennomføringen gjelder for. Ekstra informasjon om tiltakstypen er
  tilgjengelig på egen topic for tiltakstyper.
- Oppdateres ved:
    - Endringer gjort av Nav-ansatte på gjennomføringen (f.eks. innhold, datoer).
    - Endringer på avtalen som gjennomføringen tilhører (alle tilknyttede gjennomføringer oppdateres).
    - Automatisk statusoppdatering (f.eks. settes til avsluttet når sluttdato passeres).
    - Manuell relast av gjennomføringer fra databasen.
- **Log compaction:** Aktivert
- **Modell:** [`TiltaksgjennomforingV2Dto`](../../common/domain/src/main/kotlin/no/nav/mulighetsrommet/model/TiltaksgjennomforingV2Dto.kt)

---

## arena-migrering-tiltaksgjennomforinger-v1

- Egen topic tilrettelagt for Arena.
- Inneholder nok informasjon om gjennomføringer til at Arena kan replikere gjennomføringer fra Tiltaksadministrasjon.
- Hver hendelse på `siste-tiltaksgjennomforinger-v2` publiseres også her, så fremt tiltakskoden har blitt markert for
  migrering.
- **Log compaction:** Aktivert
- **Modell:** [`ArenaMigreringTiltaksgjennomforingDto`](../../mulighetsrommet-api/server/src/main/kotlin/no/nav/mulighetsrommet/api/gjennomforing/model/ArenaMigreringTiltaksgjennomforingDto.kt)

---

## datavarehus-tiltak-v1

- Egen topic tilrettelagt for datavarehuset.
- Inneholder sentral informasjon om tiltakstype, avtale og gjennomføring.
- Hver hendelse på `siste-tiltaksgjennomforinger-v2` publiseres også her.
- **Log compaction:** Aktivert
- **Modell:** [`DatavarehusTiltakV1`](../../mulighetsrommet-api/server/src/main/kotlin/no/nav/mulighetsrommet/api/datavarehus/model/DatavarehusTiltakV1.kt)

---

## totrinnskontroll-v1

- Inneholder hendelser om behandling og besluttelse (totrinnskontroll) av entiteter i Tiltaksadministrasjon, bl.a.
  tilsagn og utbetalinger.
- Nøkkelen er ID-en til entiteten (f.eks. tilsagnet eller utbetalingen) som hendelsen gjelder for.
- Oppdateres ved:
    - Entiteten sendes til behandling, settes på vent, godkjennes eller returneres.
- **Log compaction:** Deaktivert (90 dagers retention)
- **Modell:** [`TotrinnskontrollHendelse`](../../mulighetsrommet-api/contracts/src/main/kotlin/no/nav/mulighetsrommet/api/contracts/totrinnskontroll/TotrinnskontrollHendelse.kt)

---

## tilskudd.utbetaling-v1

- Egen topic tilrettelagt for Hel Ved.
- Inneholder engangsutbetalinger (periodetype `EN_GANG`) med månedlig motregning som skal utbetales til bruker, f.eks.
  for skolepenger, studiereise eller eksamensgebyr.
- Oppdateres ved:
    - En utbetaling til bruker blir besluttet/attestert i Tiltaksadministrasjon.
- **Log compaction:** Aktivert (kombinert med 90 dagers retention)
- **Modell:** [`HelVedUtbetaling`](../../mulighetsrommet-api/server/src/main/kotlin/no/nav/mulighetsrommet/api/clients/helved/HelVedUtbetaling.kt)
