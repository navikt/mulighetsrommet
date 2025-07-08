import {
  ArrangorflateTilsagn,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TilsagnType,
} from "api-client";

export const arrangorflateTilsagn: ArrangorflateTilsagn[] = [
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
    },
    type: TilsagnType.TILSAGN,
    periode: {
      start: "2025-01-01",
      slutt: "2025-07-01",
    },
    beregning: {
      type: "PRIS_PER_MANEDSVERK",
      input: {
        type: "PRIS_PER_MANEDSVERK",
        periode: {
          start: "2025-01-01",
          slutt: "2025-07-01",
        },
        sats: 20975,
        antallPlasser: 42,
      },
      output: {
        type: "PRIS_PER_MANEDSVERK",
        belop: 5285700,
      },
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: {
      status: TilsagnStatus.GODKJENT,
      aarsaker: [],
    },
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
    },
    type: TilsagnType.INVESTERING,
    periode: {
      start: "2025-01-01",
      slutt: "2025-12-31",
    },
    beregning: {
      type: "FRI",
      input: {
        type: "FRI",
        linjer: [
          {
            id: "8890342e-cde8-4680-8122-f4af7d988492",
            beskrivelse: "Investering av avtalt sum",
            belop: 123456,
            antall: 1,
          },
        ],
        prisbetingelser: null,
      },
      output: {
        type: "FRI",
        belop: 5285700,
      },
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: {
      status: TilsagnStatus.GODKJENT,
      aarsaker: [],
    },
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
    },
    type: TilsagnType.TILSAGN,
    periode: {
      start: "2025-04-01",
      slutt: "2025-05-01",
    },
    beregning: {
      type: "FRI",
      input: {
        type: "FRI",
        linjer: [
          {
            id: "1433b63b-0caa-4b53-9c0e-00cd38d94841",
            beskrivelse: "<mangler beskrivelse>",
            belop: 39000,
            antall: 1,
          },
        ],
        prisbetingelser: null,
      },
      output: {
        type: "FRI",
        belop: 39000,
      },
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: {
      status: TilsagnStatus.ANNULLERT,
      aarsaker: [TilsagnTilAnnulleringAarsak.TILTAK_SKAL_IKKE_GJENNOMFORES],
    },
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
    },
    type: TilsagnType.EKSTRATILSAGN,
    periode: {
      start: "2025-03-18",
      slutt: "2025-04-02",
    },
    beregning: {
      type: "PRIS_PER_MANEDSVERK",
      input: {
        type: "PRIS_PER_MANEDSVERK",
        periode: {
          start: "2025-03-18",
          slutt: "2025-04-02",
        },
        sats: 20975,
        antallPlasser: 114,
      },
      output: {
        type: "PRIS_PER_MANEDSVERK",
        belop: 1147752,
      },
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: {
      status: TilsagnStatus.OPPGJORT,
      aarsaker: [],
    },
    bestillingsnummer: "A-2025/11073-2",
  },

  {
    id: "27f81471-1c6a-4f68-921e-ba9da68d4e89",
    gjennomforing: { id: "6a760ab8-fb12-4c6e-b143-b711331f63f6", navn: "May rain - VTA " },
    bruktBelop: 0,
    gjenstaendeBelop: 6000,
    tiltakstype: { navn: "Varig tilrettelagt arbeid i skjermet virksomhet" },
    type: TilsagnType.TILSAGN,
    periode: { start: "2025-04-01", slutt: "2025-10-01" },
    beregning: {
      type: "PRIS_PER_MANEDSVERK",
      input: {
        type: "PRIS_PER_MANEDSVERK",
        periode: { start: "2025-04-01", slutt: "2025-10-01" },
        sats: 16848,
        antallPlasser: 30,
      },
      output: {
        type: "PRIS_PER_MANEDSVERK",
        belop: 6000,
      },
    },
    arrangor: {
      id: "cc04c391-d733-4762-8208-b0dd4387a126",
      organisasjonsnummer: "973674471",
      navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
    },
    status: { status: TilsagnStatus.GODKJENT, aarsaker: [] },
    bestillingsnummer: "A-2025/11398-1",
  },
];
