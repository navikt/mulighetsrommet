import {
  AmtDeltakerStatusAarsak,
  AmtDeltakerStatusType,
  ArenaDeltakerStatus,
  TiltakshistorikkAdminDto,
} from "@mr/api-client";

export const historikk: TiltakshistorikkAdminDto[] = [
  {
    id: window.crypto.randomUUID(),
    startDato: "2024.10.15",
    sluttDato: "2025.06.10",
    status: ArenaDeltakerStatus.VENTELISTE,
    opphav: "ARENA",
    tiltakNavn: "Mentortiltak hos fretex",
    tiltakstypeNavn: "Mentor",
    arrangor: {
      navn: "Adecco",
      organisasjonsnummer: "8465732190",
    },
  },
  {
    id: window.crypto.randomUUID(),
    startDato: "2024.01.01",
    sluttDato: "2025.01.01",
    status: ArenaDeltakerStatus.GJENNOMFORES,
    opphav: "ARENA",
    tiltakNavn: "Avklaring hos noen",
    tiltakstypeNavn: "Avklaring",
    arrangor: {
      navn: "Fretex AS",
      organisasjonsnummer: "12345678910",
    },
  },
  {
    id: window.crypto.randomUUID(),
    startDato: "2023.05.06",
    sluttDato: "2023.06.06",
    status: {
      type: AmtDeltakerStatusType.AVBRUTT,
      aarsak: AmtDeltakerStatusAarsak.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT,
      opprettetDato: "2023.06.06",
    },
    opphav: "TEAM_KOMET",
    tiltakNavn: "Gruppe amo tiltak nr 12",
    tiltakstypeNavn: "Gruppe AMO",
    arrangor: {
      navn: "AS3",
      organisasjonsnummer: "9174658309",
    },
  },
];
