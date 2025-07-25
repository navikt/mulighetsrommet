import { ArbeidsgiverAvtaleStatus, Deltakelse, DeltakerStatusType, Eierskap } from "@api-client";
import { tiltakAft, tiltakAvklaring, tiltakJobbklubb } from "./mockGjennomforinger";

export const deltakelserAktive: Deltakelse[] = [
  {
    type: "no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse.DeltakelseGruppetiltak",
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAft.id,
    innsoktDato: "2024-03-02",
    sistEndretDato: "2024-03-27",
    status: {
      type: DeltakerStatusType.KLADD,
      visningstekst: "Kladden er ikke delt",
      aarsak: null,
    },
    tiltakstypeNavn: "Arbeidsforberedende trening",
    tittel: "Arbeidsforberedende trening hos Barneverns- og Helsenemnda",
    eierskap: Eierskap.TEAM_KOMET,
    periode: { startDato: null, sluttDato: null },
  },
  {
    type: "no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse.DeltakelseGruppetiltak",
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakAvklaring.id,
    innsoktDato: "2024-02-01",
    sistEndretDato: "2024-03-27",
    status: {
      type: DeltakerStatusType.UTKAST_TIL_PAMELDING,
      visningstekst: "Utkastet er delt og venter på godkjenning",
      aarsak: null,
    },
    tiltakstypeNavn: "Avklaring",
    tittel: "Avklaring hos Fretex AS",
    eierskap: Eierskap.TEAM_KOMET,
    periode: { startDato: null, sluttDato: null },
  },
  {
    type: "no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse.DeltakelseGruppetiltak",
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakJobbklubb.id,
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.VENTER_PA_OPPSTART,
      visningstekst: "Venter på oppstart",
      aarsak: null,
    },
    tiltakstypeNavn: "Jobbklubb",
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    tittel: "Jobbklubb hos Fretex",
    sistEndretDato: null,
    eierskap: Eierskap.TEAM_KOMET,
  },
  {
    type: "no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse.DeltakelseGruppetiltak",
    id: window.crypto.randomUUID(),
    gjennomforingId: tiltakJobbklubb.id,
    innsoktDato: "2024-02-01",
    status: {
      type: DeltakerStatusType.DELTAR,
      visningstekst: "Deltar",
      aarsak: null,
    },
    tiltakstypeNavn: "Jobbklubb",
    periode: {
      startDato: "2023-08-10",
      sluttDato: "2023-09-11",
    },
    sistEndretDato: null,
    tittel: "Jobbklubb hos Fretex",
    eierskap: Eierskap.TEAM_KOMET,
  },
  {
    type: "no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse.DeltakelseArbeidsgiverAvtale",
    id: window.crypto.randomUUID(),
    innsoktDato: "2024-02-01",
    status: {
      type: ArbeidsgiverAvtaleStatus.GJENNOMFORES,
      visningstekst: "Venter på oppstart",
    },
    tiltakstypeNavn: "Arbeidstrening",
    periode: {
      startDato: "2023-08-10",
      sluttDato: null,
    },
    tittel: "Arbeidstrening hos Fretex",
    sistEndretDato: null,
    eierskap: Eierskap.TEAM_TILTAK,
  },
];
