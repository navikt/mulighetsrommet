import {
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingStatus,
  DeltakerStatusType,
  DelutbetalingStatus,
  DetailsFormat,
  Tiltakskode,
} from "api-client";
import { utbetalingType } from "./utbetalingTypeMocks";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";

const arrManedKlarTilGodkjenning: ArrangorflateUtbetalingDto = {
  id: "a134c0bf-40eb-4124-8f2e-df7b7c51fd44",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  godkjentAvArrangorTidspunkt: null,
  kanViseBeregning: true,
  createdAt: "2025-11-07T10:02:43.989186",
  tiltakstype: {
    navn: "Arbeidsrettet rehabilitering",
    tiltakskode: Tiltakskode.ARBEIDSRETTET_REHABILITERING,
  },
  gjennomforing: {
    id: "a47092ba-410b-4ca1-9713-36506a039742",
    navn: "Arbeidsrettet rehabilitering - Månedlig",
  },
  arrangor: arrangorMock,
  betalingsinformasjon: { kontonummer: "78029049393", kid: null },
  beregning: {
    type: "ArrangorflateBeregningPrisPerManedsverk",
    belop: 20000,
    digest: "ca0a6c20",
    detaljer: {
      entries: [
        { key: "Avtalt månedspris per tiltaksplass", value: "10000", format: DetailsFormat.NOK },
        { key: "Antall månedsverk", value: "2.0", format: DetailsFormat.NUMBER },
        { key: "Beløp", value: "20000", format: DetailsFormat.NOK },
      ],
    },
    deltakelser: [
      {
        type: "ArrangorflateBeregningDeltakelsePrisPerManedsverk",
        id: "79d75296-9b62-4758-b713-799d517d3826",
        deltakerStartDato: "2025-10-01",
        faktor: 1,
        periode: { start: "2025-10-01", slutt: "2025-11-01" },
        personalia: { navn: "Nordmann, Ola", norskIdent: "27017809100", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
      {
        type: "ArrangorflateBeregningDeltakelsePrisPerManedsverk",
        id: "6568d33c-b1fc-4490-9e39-c6b2b90aad8d",
        deltakerStartDato: "2025-10-01",
        faktor: 1,
        periode: { start: "2025-10-01", slutt: "2025-11-01" },
        personalia: { navn: "Nordmann, Ola", norskIdent: "27017809100", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
    ],
    stengt: [],
    displayName: "Avtalt månedspris per tiltaksplass",
  },
  periode: { start: "2025-10-01", slutt: "2025-11-01" },
  type: { displayName: "Innsending", displayNameLong: null, tagName: null },
  linjer: [],
  advarsler: [],
};

const aftUtbetalt: ArrangorflateUtbetalingDto = {
  id: "e48f9b35-855f-43aa-8b4d-a669013df34b",
  status: ArrangorflateUtbetalingStatus.UTBETALT,
  godkjentAvArrangorTidspunkt: "2025-05-15T11:03:21.959059",
  createdAt: "2025-03-17T12:27:55.465944",
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  gjennomforing: { id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1", navn: "AFT Foobar" },
  arrangor: {
    id: "cc04c391-d733-4762-8208-b0dd4387a126",
    organisasjonsnummer: "973674471",
    navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
  },
  kanViseBeregning: true,
  beregning: {
    type: "ArrangorflateBeregningFastSatsPerTiltaksplassPerManed",
    displayName: "Sats per tiltaksplass per måned",
    detaljer: {
      entries: [
        { key: "Antall månedsverk", value: "0.48", format: DetailsFormat.NUMBER },
        { key: "Sats", value: "129", format: DetailsFormat.NOK },
        { key: "Beløp", value: "10149", format: DetailsFormat.NOK },
      ],
    },
    belop: 10149,
    digest: "b3602d2a",
    deltakelser: [
      {
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        id: "6248f41f-4029-4bce-baba-dc23cfd5a242",
        deltakerStartDato: "2025-01-17",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-01-17", slutt: "2025-02-01" }, deltakelsesprosent: 100.0 },
        ],
        faktor: 0.48387,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
    ],
    stengt: [],
  },
  advarsler: [],
  betalingsinformasjon: { kontonummer: "10002427740", kid: null },
  periode: { start: "2025-01-01", slutt: "2025-02-01" },
  type: utbetalingType.INNSENDING,
  linjer: [
    {
      id: "59b56a35-4406-4c29-b5e6-535a78761044",
      tilsagn: {
        id: "ad77762c-eebb-4623-be6d-0c64da79f2dd",
        bestillingsnummer: "A-2025/11073-1",
      },
      status: DelutbetalingStatus.UTBETALT,
      statusSistOppdatert: "2025-05-15T11:03:22.772767",
      belop: 10149,
    },
  ],
};

const aftKreverEndring: ArrangorflateUtbetalingDto = {
  id: "a5499e34-9fb4-49d1-a37d-11810f6df19b",
  status: ArrangorflateUtbetalingStatus.KREVER_ENDRING,
  godkjentAvArrangorTidspunkt: null,
  createdAt: "2025-06-17T13:58:12.405867",
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  gjennomforing: { id: "b3e1cfbb-bfb5-4b4b-b8a4-af837631ed51", navn: "Solrikt AFT" },
  arrangor: {
    id: "cc04c391-d733-4762-8208-b0dd4387a126",
    organisasjonsnummer: "973674471",
    navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
  },
  advarsler: [
    {
      type: "DeltakerAdvarselFeilSluttDato",
      deltakerId: "ff07c9c2-dff3-4e81-bd5a-40bb19108cc1",
    },
    {
      type: "DeltakerAdvarselRelevanteForslag",
      deltakerId: "ff07c9c2-dff3-4e81-bd5a-40bb19108cc1",
      antallRelevanteForslag: 3,
    },
  ],
  kanViseBeregning: true,
  beregning: {
    type: "ArrangorflateBeregningFastSatsPerTiltaksplassPerManed",
    displayName: "Sats per tiltaksplass per måned",
    detaljer: {
      entries: [
        { key: "Antall månedsverk", value: "11.58", format: DetailsFormat.NUMBER },
        { key: "Sats", value: "129", format: DetailsFormat.NOK },
        { key: "Beløp", value: "242904", format: DetailsFormat.NOK },
      ],
    },
    belop: 242904,
    digest: "db0c7c6e",
    deltakelser: [
      {
        id: "ff07c9c2-dff3-4e81-bd5a-40bb19108cc1",
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        deltakerStartDato: "2024-07-15",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-01-01", slutt: "2025-02-01" }, deltakelsesprosent: 60.0 },
        ],
        faktor: 1.0,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.HAR_SLUTTET,
      },
      {
        id: "f7284cdb-9b8f-4431-9808-b1bcc9ae7494",
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        deltakerStartDato: "2024-07-16",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-01-01", slutt: "2025-02-01" }, deltakelsesprosent: 100.0 },
        ],
        faktor: 1.0,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
      {
        id: "32b05c15-e666-46ad-8fae-ee7a5263fa81",
        deltakerStartDato: "2024-12-19",
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-01-01", slutt: "2025-02-01" }, deltakelsesprosent: 100.0 },
        ],
        faktor: 1.0,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
      {
        id: "a06a5562-78ac-4564-992c-cae22d9707f2",
        deltakerStartDato: "2024-12-19",
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-01-01", slutt: "2025-02-01" }, deltakelsesprosent: 100.0 },
        ],
        faktor: 1.0,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
      {
        id: "44af1d55-33f6-4198-9ebd-3f09e50e9b84",
        deltakerStartDato: "2024-08-29",
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-01-01", slutt: "2025-02-01" }, deltakelsesprosent: 100.0 },
        ],
        faktor: 1.0,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
      {
        id: "7efeb453-805b-4fda-a413-e1723ff93a1b",
        deltakerStartDato: "2024-10-14",
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-01-01", slutt: "2025-01-15" }, deltakelsesprosent: 80.0 },
        ],
        faktor: 0.45161,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
      {
        id: "ec1c4596-cbd9-4fb0-8aa7-0c7b41ced5de",
        deltakerStartDato: "2024-11-08",
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-01-01", slutt: "2025-02-01" }, deltakelsesprosent: 100.0 },
        ],
        faktor: 1.0,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
      {
        id: "b08e99ff-4406-4ceb-9299-b6e9f1afaa30",
        deltakerStartDato: "2025-01-17",
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-01-17", slutt: "2025-02-01" }, deltakelsesprosent: 100.0 },
        ],
        faktor: 0.48387,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
    ],
    stengt: [],
  },
  betalingsinformasjon: { kontonummer: "10002427740", kid: null },
  periode: { start: "2025-01-01", slutt: "2025-02-01" },
  type: utbetalingType.INNSENDING,
  linjer: [],
};

const aftBehandlesAvNav: ArrangorflateUtbetalingDto = {
  id: "585a2834-338a-4ac7-82e0-e1b08bfe1408",
  status: ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
  godkjentAvArrangorTidspunkt: "2025-06-05T09:36:11.510229",
  createdAt: "2025-06-05T09:36:11.510957",
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  advarsler: [],
  gjennomforing: {
    id: "d29cb67c-8e68-4ece-90dc-ff21c498aa3f",
    navn: "AFT - Arbeidsforberedende trening - Team tiltakspenger",
  },
  arrangor: {
    id: "cc04c391-d733-4762-8208-b0dd4387a126",
    organisasjonsnummer: "973674471",
    navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
  },
  kanViseBeregning: true,
  beregning: {
    type: "ArrangorflateBeregningFri",
    displayName: "Annen avtalt pris",
    detaljer: {
      entries: [{ key: "Beløp", value: "242904", format: DetailsFormat.NOK }],
    },
    belop: 234,
    digest: "000001d4",
  },
  betalingsinformasjon: { kontonummer: "10002427740", kid: null },
  periode: { start: "2025-05-01", slutt: "2025-08-02" },
  type: utbetalingType.INVESTERING,
  linjer: [],
};

const avklaringOverfortTilUtbetaling: ArrangorflateUtbetalingDto = {
  id: "153bc6f0-0c5f-4555-9447-b88ea0cc60f2",
  status: ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
  godkjentAvArrangorTidspunkt: null,
  createdAt: "2025-04-01T12:03:49.756508",
  tiltakstype: {
    navn: "Avklaring",
    tiltakskode: Tiltakskode.AVKLARING,
  },
  gjennomforing: { id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1", navn: "AFT Foobar" },
  arrangor: {
    id: "cc04c391-d733-4762-8208-b0dd4387a126",
    organisasjonsnummer: "973674471",
    navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
  },
  kanViseBeregning: true,
  beregning: {
    type: "ArrangorflateBeregningFri",
    displayName: "Annen avtalt pris",
    detaljer: {
      entries: [{ key: "Beløp", value: "500", format: DetailsFormat.NOK }],
    },
    belop: 500,
    digest: "000003e8",
  },
  betalingsinformasjon: { kontonummer: "63728787114", kid: "2851777587" },
  periode: { start: "2025-01-01", slutt: "2025-02-01" },
  type: utbetalingType.KORRIGERING,
  linjer: [
    {
      id: "b1a3727a-8bf7-4470-a9b6-71803c99846e",
      tilsagn: {
        id: "ad77762c-eebb-4623-be6d-0c64da79f2dd",
        bestillingsnummer: "A-2025/11073-1",
      },
      status: DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
      statusSistOppdatert: null,
      belop: 500,
    },
  ],
  advarsler: [],
};

const vtaKlarForGodkjenning: ArrangorflateUtbetalingDto = {
  id: "fdbb7433-b42e-4cd6-b995-74a8e487329f",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  godkjentAvArrangorTidspunkt: null,
  createdAt: "2025-07-07T05:00:06.924181",
  tiltakstype: {
    navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
    tiltakskode: Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
  },
  gjennomforing: { id: "6a760ab8-fb12-4c6e-b143-b711331f63f6", navn: "May rain - VTA " },
  arrangor: {
    id: "cc04c391-d733-4762-8208-b0dd4387a126",
    organisasjonsnummer: "973674471",
    navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
  },
  kanViseBeregning: true,
  beregning: {
    type: "ArrangorflateBeregningFastSatsPerTiltaksplassPerManed",
    displayName: "Sats per tiltaksplass per måned",
    detaljer: {
      entries: [
        { key: "Antall månedsverk", value: "1.0", format: DetailsFormat.NUMBER },
        { key: "Sats", value: "129", format: DetailsFormat.NOK },
        { key: "Beløp", value: "16848", format: DetailsFormat.NOK },
      ],
    },
    belop: 16848,
    digest: "38c07a43",
    deltakelser: [
      {
        id: "2e9d8070-b74f-455b-b8ae-df7b61c871aa",
        deltakerStartDato: "2025-05-20",
        type: "ArrangorflateBeregningDeltakelseFastSatsPerTiltaksplassPerManed",
        periode: { start: "2025-01-01", slutt: "2025-01-31" },
        perioderMedDeltakelsesmengde: [
          { periode: { start: "2025-06-01", slutt: "2025-06-12" }, deltakelsesprosent: 100.0 },
          { periode: { start: "2025-06-12", slutt: "2025-07-01" }, deltakelsesprosent: 50.0 },
        ],
        faktor: 1.0,
        personalia: { navn: "Nordmann, Ola", norskIdent: "01010199999", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
    ],
    stengt: [],
  },
  betalingsinformasjon: { kontonummer: "10002427740", kid: "123123123" },
  periode: { start: "2025-06-01", slutt: "2025-07-01" },
  type: utbetalingType.INNSENDING,
  linjer: [],
  advarsler: [],
};

const avklaringManedKlarTilInnsending: ArrangorflateUtbetalingDto = {
  id: "ba046f93-cb0c-4acf-a724-99a36481f183",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  godkjentAvArrangorTidspunkt: null,
  kanViseBeregning: true,
  createdAt: "2025-10-31T14:41:03.835624",
  tiltakstype: { navn: "Avklaring", tiltakskode: Tiltakskode.AVKLARING },
  gjennomforing: {
    id: "70cdc182-8913-48c0-bad9-fa4e74f3288e",
    navn: "Avklaring - Avtalt ukespris per tiltaksplass",
  },
  arrangor: {
    id: "cc04c391-d733-4762-8208-b0dd4387a126",
    organisasjonsnummer: "973674471",
    navn: "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
  },
  betalingsinformasjon: { kontonummer: "10002427740", kid: null },
  beregning: {
    type: "ArrangorflateBeregningPrisPerUkesverk",
    belop: 197980,
    digest: "28172363",
    detaljer: {
      entries: [
        { key: "Avtalt ukespris per tiltaksplass", value: "9999", format: DetailsFormat.NOK },
        { key: "Antall ukesverk", value: "11.8", format: DetailsFormat.NUMBER },
        { key: "Beløp", value: "117988", format: DetailsFormat.NOK },
      ],
    },
    deltakelser: [
      {
        type: "ArrangorflateBeregningDeltakelsePrisPerUkesverk",
        id: "48596046-3e0c-4746-b959-ecbe07e4beef",
        deltakerStartDato: "2025-08-04",
        faktor: 4,
        periode: { start: "2025-11-01", slutt: "2025-12-01" },
        personalia: { navn: "Nordmann, Ola", norskIdent: "27017809100", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
      {
        type: "ArrangorflateBeregningDeltakelsePrisPerUkesverk",
        id: "80833e40-7b13-4106-8c35-128f431bd3b0",
        deltakerStartDato: "2025-08-11",
        faktor: 4,
        periode: { start: "2025-11-01", slutt: "2025-12-01" },
        personalia: { navn: "Nordmann, Ola", norskIdent: "27017809100", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
      {
        type: "ArrangorflateBeregningDeltakelsePrisPerUkesverk",
        id: "8af3af05-eb1d-4df8-9bd9-3be96e1c90f5",
        deltakerStartDato: "2025-08-01",
        faktor: 3.8,
        periode: { start: "2025-11-01", slutt: "2025-11-28" },
        personalia: { navn: "Nordmann, Ola", norskIdent: "27017809100", erSkjermet: false },
        status: DeltakerStatusType.DELTAR,
      },
    ],
    stengt: [],
    displayName: "Avtalt ukespris per tiltaksplass",
  },
  periode: { start: "2025-11-01", slutt: "2025-12-01" },
  type: { displayName: "Innsending", displayNameLong: null, tagName: null },
  linjer: [],
  advarsler: [],
};

export const arrFlateUtbetaling: ArrangorflateUtbetalingDto[] = [
  aftUtbetalt,
  aftKreverEndring,
  aftBehandlesAvNav,
  avklaringOverfortTilUtbetaling,
  vtaKlarForGodkjenning,
  avklaringManedKlarTilInnsending,
  arrManedKlarTilGodkjenning,
];
