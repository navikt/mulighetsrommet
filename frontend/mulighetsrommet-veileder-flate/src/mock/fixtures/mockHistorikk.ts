import { HistorikkForBruker, HistorikkForBrukerStatus } from "mulighetsrommet-api-client";

export const historikk: HistorikkForBruker[] = [
  {
    id: window.crypto.randomUUID(),
    fnr: "12345678910",
    fraDato: "2024.10.15",
    tilDato: "2025.06.10",
    status: HistorikkForBrukerStatus.VENTER,
    tiltaksnavn: "Mentortiltak hos fretex",
    tiltakstype: "Mentor",
    arrangor: {
      navn: "Adecco",
      organisasjonsnummer: "8465732190",
    },
  },
  {
    id: window.crypto.randomUUID(),
    fnr: "12345678910",
    fraDato: "2024.01.01",
    tilDato: "2025.01.01",
    status: HistorikkForBrukerStatus.DELTAR,
    tiltaksnavn: "Avklaring hos noen",
    tiltakstype: "Avklaring",
    arrangor: {
      navn: "Fretex AS",
      organisasjonsnummer: "12345678910",
    },
  },
  {
    id: window.crypto.randomUUID(),
    fnr: "12345678910",
    fraDato: "2023.05.06",
    tilDato: "2023.06.06",
    status: HistorikkForBrukerStatus.IKKE_AKTUELL,
    tiltaksnavn: "Midlertidig lønnstilskudd",
    tiltakstype: "Midlertidig lønnstilskudd",
    arrangor: {
      navn: "AS3",
      organisasjonsnummer: "9174658309",
    },
  },
];
