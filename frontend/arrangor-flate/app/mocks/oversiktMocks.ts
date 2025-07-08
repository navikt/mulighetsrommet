import { ArrFlateUtbetalingKompakt, ArrFlateUtbetalingStatus, UtbetalingType } from "api-client";

export const mockArrFlateUtbetalingKompakt: ArrFlateUtbetalingKompakt[] = [
  {
    id: "e48f9b35-855f-43aa-8b4d-a669013df34b",
    status: ArrFlateUtbetalingStatus.UTBETALT,
    godkjentAvArrangorTidspunkt: "2025-05-15T11:03:21.959059",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
      slettet: false,
    },
    periode: {
      start: "2025-01-01",
      slutt: "2025-02-01",
    },
    belop: 10149,
  },
  {
    id: "db6f1689-6446-4f1b-900d-b06c469851f8",
    status: ArrFlateUtbetalingStatus.BEHANDLES_AV_NAV,
    godkjentAvArrangorTidspunkt: "2025-05-02T14:51:13.772419",
    tiltakstype: {
      navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
    },
    gjennomforing: {
      id: "e6dfb756-3499-4d15-bd42-f25e01411443",
      navn: "November rain - VTA ",
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
      slettet: false,
    },
    periode: {
      start: "2025-01-01",
      slutt: "2025-02-01",
    },
    belop: 75816,
  },
  {
    id: "a5499e34-9fb4-49d1-a37d-11810f6df19b",
    status: ArrFlateUtbetalingStatus.VENTER_PA_ENDRING,
    godkjentAvArrangorTidspunkt: null,
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: "b3e1cfbb-bfb5-4b4b-b8a4-af837631ed51",
      navn: "Solrikt AFT",
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
      slettet: false,
    },
    periode: {
      start: "2025-01-01",
      slutt: "2025-02-01",
    },
    belop: 242904,
  },
  {
    id: "585a2834-338a-4ac7-82e0-e1b08bfe1408",
    status: ArrFlateUtbetalingStatus.BEHANDLES_AV_NAV,
    godkjentAvArrangorTidspunkt: "2025-06-05T09:36:11.510229",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: "d29cb67c-8e68-4ece-90dc-ff21c498aa3f",
      navn: "AFT - Arbeidsforberedende trening - Team tiltakspenger",
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
      slettet: false,
    },
    periode: {
      start: "2025-05-01",
      slutt: "2025-08-02",
    },
    belop: 234,
    type: UtbetalingType.INVESTERING,
  },
  {
    id: "153bc6f0-0c5f-4555-9447-b88ea0cc60f2",
    status: ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
    godkjentAvArrangorTidspunkt: null,
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: "6d71a9c5-c920-4d56-bc3b-2da07e4b6100",
      navn: "AFT - Arbeidsforberedende trening - Team tiltakspenger",
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
      slettet: false,
    },
    periode: {
      start: "2025-01-01",
      slutt: "2025-02-01",
    },
    belop: 500,
    type: UtbetalingType.KORRIGERING,
  },
];
