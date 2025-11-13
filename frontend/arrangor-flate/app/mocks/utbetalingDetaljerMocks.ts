import {
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingStatus,
  DataDrivenTableDto,
  DataDrivenTableDtoColumnAlign,
  DataElementTextFormat,
  DelutbetalingStatus,
  DetailsFormat,
  Tiltakskode,
  TimelineDtoRowPeriodVariant,
} from "api-client";
import { utbetalingType } from "./utbetalingTypeMocks";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";

const deltakelser: DataDrivenTableDto = {
  columns: [
    { key: "navn", label: "Navn", sortable: false, align: DataDrivenTableDtoColumnAlign.LEFT },
    {
      key: "identitetsnummer",
      label: "Fødselsnr.",
      sortable: false,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "tiltakStart",
      label: "Startdato i tiltaket",
      sortable: false,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "periodeStart",
      label: "Startdato i perioden",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "periodeSlutt",
      label: "Sluttdato i perioden",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "faktor",
      label: "Ukesverk",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.RIGHT,
    },
  ],
  rows: [
    {
      cells: {
        navn: {
          type: "no.nav.mulighetsrommet.model.DataElement.Text",
          value: "Nordmann, Ola",
          format: null,
        },
        identitetsnummer: {
          type: "no.nav.mulighetsrommet.model.DataElement.Text",
          value: "27017809100",
          format: null,
        },
        tiltakStart: {
          type: "no.nav.mulighetsrommet.model.DataElement.Text",
          value: "2025-07-29",
          format: DataElementTextFormat.DATE,
        },
        periodeStart: {
          type: "no.nav.mulighetsrommet.model.DataElement.Text",
          value: "2025-09-29",
          format: DataElementTextFormat.DATE,
        },
        periodeSlutt: {
          type: "no.nav.mulighetsrommet.model.DataElement.Text",
          value: "2025-10-27",
          format: DataElementTextFormat.DATE,
        },
        faktor: {
          type: "no.nav.mulighetsrommet.model.DataElement.Text",
          value: "6.0",
          format: DataElementTextFormat.NUMBER,
        },
      },
      content: {
        startDate: "2025-10-01",
        endDate: "2025-10-31",
        rows: [
          {
            periods: [
              {
                key: "0",
                start: "2025-09-29",
                end: "2025-10-14",
                status: TimelineDtoRowPeriodVariant.INFO,
                content: "Pris per uke: 777, Ukesverk: 3.0",
                hover: "Pris per uke: 777, Ukesverk: 3.0, Periode: 01.10.2025 - 31.10.2025",
              },
              {
                key: "1",
                start: "2025-10-15",
                end: "2025-10-27",
                status: TimelineDtoRowPeriodVariant.INFO,
                content: "Pris per uke: 888, Ukesverk: 3.0",
                hover: "Pris per uke: 888, Ukesverk: 3.0, Periode: 01.10.2025 - 31.10.2025",
              },
            ],
            label: "Beregning",
          },
        ],
      },
    },
  ],
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
    deltakelser,
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
    belop: 20000,
    digest: "ca0a6c20",
    detaljer: {
      entries: [
        { key: "Avtalt månedspris per tiltaksplass", value: "10000", format: DetailsFormat.NOK },
        { key: "Antall månedsverk", value: "2.0", format: DetailsFormat.NUMBER },
        { key: "Beløp", value: "20000", format: DetailsFormat.NOK },
      ],
    },
    deltakelser,
    stengt: [],
    displayName: "Avtalt månedspris per tiltaksplass",
  },
  periode: { start: "2025-10-01", slutt: "2025-11-01" },
  type: { displayName: "Innsending", displayNameLong: null, tagName: null },
  linjer: [],
  advarsler: [],
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
  advarsler: [],
  kanViseBeregning: true,
  beregning: {
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
    deltakelser,
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
    deltakelser: null,
    stengt: [],
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
    stengt: [],
    deltakelser: null,
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
    deltakelser,
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
    belop: 197980,
    digest: "28172363",
    detaljer: {
      entries: [
        { key: "Avtalt ukespris per tiltaksplass", value: "9999", format: DetailsFormat.NOK },
        { key: "Antall ukesverk", value: "11.8", format: DetailsFormat.NUMBER },
        { key: "Beløp", value: "117988", format: DetailsFormat.NOK },
      ],
    },
    deltakelser,
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
