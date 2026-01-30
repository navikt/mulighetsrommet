import { ArrangorflateArrangorDto, ArrangorInnsendingRadDto } from "@api-client";

export const gjennomforingIdAFT = "54d0d2af-f329-480d-a427-30de446fea10";
export const gjennomforingIdAvklaring = "54d0d2af-f329-480d-a427-30de446fea11";
export const gjennomforingIdOppfolging = "54d0d2af-f329-480d-a427-30de446fea12";

const today: Date = new Date();

export const arrangorMock: ArrangorflateArrangorDto = {
  id: "cc04c391-d733-4762-8208-b0dd4387a126",
  navn: "Arrangørens navn",
  organisasjonsnummer: "123456789",
};

export const gjennomforingAFT: ArrangorInnsendingRadDto = {
  gjennomforingId: gjennomforingIdAFT,
  utbetalingId: null,
  tiltakNavn: "Et AFT-tiltak Investering",
  tiltakstypeNavn: "Arbeidsforberedende trening",
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  startDato: new Date(today.getFullYear() - 5, 7, 1).toISOString().slice(0, 10),
  sluttDato: null,
  lopenummer: "2024/12345",
  belop: null,
  type: null,
  status: null,
};

export const gjennomforingAvklaring: ArrangorInnsendingRadDto = {
  gjennomforingId: gjennomforingIdAvklaring,
  utbetalingId: null,
  tiltakNavn: "Et avklaringstiltak med annen avtalt pris",
  tiltakstypeNavn: "Avklaring",
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  startDato: new Date(today.getFullYear() - 1, 1, 1).toISOString().slice(0, 10),
  sluttDato: new Date(today.getFullYear() + 1, 11, 31).toISOString().slice(0, 10),
  lopenummer: "2025/54321",
  belop: null,
  type: null,
  status: null,
};

export const gjennomforingOppfolging: ArrangorInnsendingRadDto = {
  gjennomforingId: gjennomforingIdOppfolging,
  utbetalingId: null,
  tiltakNavn: "Et oppfølgingstiltak med avtalt timespris",
  tiltakstypeNavn: "Oppfølging",
  arrangorNavn: arrangorMock.navn,
  organisasjonsnummer: arrangorMock.organisasjonsnummer,
  startDato: new Date(today.getFullYear() - 1, 1, 1).toISOString().slice(0, 10),
  sluttDato: new Date(today.getFullYear() + 1, 11, 31).toISOString().slice(0, 10),
  lopenummer: "2025/12354",
  belop: null,
  type: null,
  status: null,
};

export const oversiktAktiveGjennomforinger: ArrangorInnsendingRadDto[] = [
  gjennomforingAFT,
  gjennomforingAvklaring,
  gjennomforingOppfolging,
];
