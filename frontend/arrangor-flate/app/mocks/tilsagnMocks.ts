import {
  ArrangorflateTilsagnDto,
  DetailsFormat,
  TilsagnStatus,
  TilsagnType,
  Tiltakskode,
} from "api-client";

export const arrangorflateTilsagn: ArrangorflateTilsagnDto[] = [
  {
    id: "ad77762c-eebb-4623-be6d-0c64da79f2dd",
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
    },
    bruktBelop: 51205,
    gjenstaendeBelop: 5234495,
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    },
    type: TilsagnType.TILSAGN,
    periode: {
      start: "2025-01-01",
      slutt: "2025-07-01",
    },
    beregning: {
      entries: [
        {
          key: "Antall plasser",
          value: "42",
          format: null,
        },
        {
          key: "Pris per månedsverk",
          value: "20975",
          format: DetailsFormat.NOK,
        },
        {
          key: "Totalt beløp",
          value: "5285700",
          format: DetailsFormat.NOK,
        },
        {
          key: "Gjenstående beløp",
          value: "5234495",
          format: DetailsFormat.NOK,
        },
      ],
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: TilsagnStatus.GODKJENT,
    bestillingsnummer: "A-2025/11073-1",
  },
  {
    id: "d8ccb57f-b9db-48e1-97f1-cb38426a9389",
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
    },
    bruktBelop: 0,
    gjenstaendeBelop: 123456,
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    },
    type: TilsagnType.INVESTERING,
    periode: {
      start: "2025-01-01",
      slutt: "2025-12-31",
    },
    beregning: {
      entries: [
        {
          key: "Totalt beløp",
          value: "5285700",
          format: DetailsFormat.NOK,
        },
        {
          key: "Gjenstående beløp",
          value: "123456",
          format: DetailsFormat.NOK,
        },
      ],
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: TilsagnStatus.GODKJENT,
    bestillingsnummer: "A-2025/11073-2",
  },
  {
    id: "f8fbc0f7-3280-410b-8387-20ff63896926",
    gjennomforing: {
      id: "70cdc182-8913-48c0-bad9-fa4e74f3288e",
      navn: "Avklaring - Team tiltakspenger - Oslo",
    },
    bruktBelop: 0,
    gjenstaendeBelop: 0,
    tiltakstype: {
      navn: "Avklaring",
      tiltakskode: Tiltakskode.AVKLARING,
    },
    type: TilsagnType.TILSAGN,
    periode: {
      start: "2025-04-01",
      slutt: "2025-05-01",
    },
    beregning: {
      entries: [
        {
          key: "Totalt beløp",
          value: "39000",
          format: DetailsFormat.NOK,
        },
        {
          key: "Gjenstående beløp",
          value: "0",
          format: DetailsFormat.NOK,
        },
      ],
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: TilsagnStatus.ANNULLERT,
    bestillingsnummer: "A-2025/11147-2",
  },
  {
    id: "17a7ff74-648b-4d43-a27d-d26cc2553b3b",
    gjennomforing: {
      id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
      navn: "AFT Foobar",
    },
    bruktBelop: 55,
    gjenstaendeBelop: 0,
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
      tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
    },
    type: TilsagnType.EKSTRATILSAGN,
    periode: {
      start: "2025-03-18",
      slutt: "2025-04-02",
    },
    beregning: {
      entries: [
        {
          key: "Antall plasser",
          value: "114",
          format: null,
        },
        {
          key: "Pris per månedsverk",
          value: "20975",
          format: DetailsFormat.NOK,
        },
        {
          key: "Totalt beløp",
          value: "1147752",
          format: DetailsFormat.NOK,
        },
        {
          key: "Gjenstående beløp",
          value: "0",
          format: DetailsFormat.NOK,
        },
      ],
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: TilsagnStatus.OPPGJORT,
    bestillingsnummer: "A-2025/11073-2",
  },

  {
    id: "27f81471-1c6a-4f68-921e-ba9da68d4e89",
    gjennomforing: { id: "6a760ab8-fb12-4c6e-b143-b711331f63f6", navn: "May rain - VTA " },
    bruktBelop: 0,
    gjenstaendeBelop: 6000,
    tiltakstype: {
      navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
      tiltakskode: Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    },
    type: TilsagnType.TILSAGN,
    periode: { start: "2025-04-01", slutt: "2025-10-01" },
    beregning: {
      entries: [
        {
          key: "Antall plasser",
          value: "30",
          format: null,
        },
        {
          key: "Pris per månedsverk",
          value: "16848",
          format: DetailsFormat.NOK,
        },
        {
          key: "Totalt beløp",
          value: "6000",
          format: DetailsFormat.NOK,
        },
        {
          key: "Gjenstående beløp",
          value: "6000",
          format: DetailsFormat.NOK,
        },
      ],
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: TilsagnStatus.GODKJENT,
    bestillingsnummer: "A-2025/11398-1",
  },
];
