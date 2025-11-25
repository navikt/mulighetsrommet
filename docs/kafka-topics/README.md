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
    - Manuell relast av tiltakstyper fra databasen.
- **Log compaction:** Aktivert

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

---

## arena-migrering-tiltaksgjennomforinger-v1

- Egen topic tilrettelagt for Arena.
- Inneholder nok informasjon om gjennomføringer til at Arena kan replikere gjennomføringer fra Tiltaksadministrasjon.
- Hver hendelse på `siste-tiltaksgjennomforinger-v2` publiseres også her, så fremt tiltakskoden har blitt markert for
  migrering.
- **Log compaction:** Aktivert

---

## datavarehus-tiltak-v1

- Egen topic tilrettelagt for datavarehuset.
- Inneholder sentral informasjon om tiltakstype, avtale og gjennomføring.
- Hver hendelse på `siste-tiltaksgjennomforinger-v2` publiseres også her.
- **Log compaction:** Aktivert
