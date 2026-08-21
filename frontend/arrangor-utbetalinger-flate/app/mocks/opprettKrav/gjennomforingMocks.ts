import {
  ArrangorflateArrangorDto,
  ArrangorflateTiltakRadDto,
  PaginatedResponseArrangorflateTiltakRadDto,
  Tiltakskode,
} from "@arrangor-utbetalinger/api-client";

export const gjennomforingIdAFT = "54d0d2af-f329-480d-a427-30de446fea10";
export const gjennomforingIdAvklaring = "54d0d2af-f329-480d-a427-30de446fea11";
export const gjennomforingIdOppfolging = "54d0d2af-f329-480d-a427-30de446fea12";

const today: Date = new Date();

export const arrangorMock: ArrangorflateArrangorDto = {
  id: "cc04c391-d733-4762-8208-b0dd4387a126",
  navn: "Arrangørens navn",
  organisasjonsnummer: "123456789",
};

export const gjennomforingAFT: ArrangorflateTiltakRadDto = {
  gjennomforing: {
    id: gjennomforingIdAFT,
    navn: "Et AFT-tiltak Investering",
    lopenummer: "2024/12345",
  },
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  arrangor: arrangorMock,
  startDato: new Date(today.getFullYear() - 5, 7, 1).toISOString().slice(0, 10),
  sluttDato: null,
};

export const gjennomforingAvklaring: ArrangorflateTiltakRadDto = {
  gjennomforing: {
    id: gjennomforingIdAvklaring,
    navn: "Et avklaringstiltak med annen avtalt pris",
    lopenummer: "2025/54321",
  },
  tiltakstype: { navn: "Avklaring", tiltakskode: Tiltakskode.AVKLARING },
  arrangor: arrangorMock,
  startDato: new Date(today.getFullYear() - 1, 1, 1).toISOString().slice(0, 10),
  sluttDato: new Date(today.getFullYear() + 1, 11, 31).toISOString().slice(0, 10),
};

export const gjennomforingOppfolging: ArrangorflateTiltakRadDto = {
  gjennomforing: {
    id: gjennomforingIdOppfolging,
    navn: "Et oppfølgingstiltak med avtalt timespris",
    lopenummer: "2025/12354",
  },
  tiltakstype: { navn: "Oppfølging", tiltakskode: Tiltakskode.OPPFOLGING },
  arrangor: arrangorMock,
  startDato: new Date(today.getFullYear() - 1, 1, 1).toISOString().slice(0, 10),
  sluttDato: new Date(today.getFullYear() + 1, 11, 31).toISOString().slice(0, 10),
};

export const oversiktAktiveGjennomforinger: PaginatedResponseArrangorflateTiltakRadDto = {
  pagination: {
    totalCount: 3,
    pageSize: 25,
    totalPages: 1,
  },
  data: [gjennomforingAFT, gjennomforingAvklaring, gjennomforingOppfolging],
};
