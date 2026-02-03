import {
  ArrangorAvbrytStatus,
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingStatus,
  DataDetails,
  DataElementTextFormat,
  DelutbetalingStatus,
  LabeledDataElementType,
  Tiltakskode,
  Valuta,
} from "api-client";
import { utbetalingType } from "./utbetalingTypeMocks";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";
import {
  arrUkesverkDeltakelse,
  avklaringManedDeltakelse,
  toSatserUkesverkDeltakelse,
  vtaManedDeltakelse,
} from "./deltakelserMocks";

const satsDetaljerForhondsgodkjent: DataDetails[] = [
  {
    header: "Periode 01.01.2025 - 01.02.2025",
    entries: [
      {
        type: LabeledDataElementType.INLINE,
        label: "Sats",
        value: { type: "DATA_ELEMENT_MONEY_AMOUNT", value: "20975", currency: "NOK" },
      },
      {
        type: LabeledDataElementType.INLINE,
        label: "Antall månedsverk",
        value: {
          type: "DATA_ELEMENT_TEXT",
          value: "0.8",
          format: DataElementTextFormat.NUMBER,
        },
      },
    ],
  },
];

const satsDetaljerManedspris: DataDetails[] = [
  {
    header: "Periode 10.10.2025 - 31.10.2025",
    entries: [
      {
        type: LabeledDataElementType.INLINE,
        label: "Avtalt månedspris per tiltaksplass",
        value: { type: "DATA_ELEMENT_MONEY_AMOUNT", value: "10000", currency: "NOK" },
      },
      {
        type: LabeledDataElementType.INLINE,
        label: "Antall månedsverk",
        value: {
          type: "DATA_ELEMENT_TEXT",
          value: "2",
          format: DataElementTextFormat.NUMBER,
        },
      },
    ],
  },
];

const satsDetaljerUkespris: DataDetails[] = [
  {
    header: "Periode 01.07.2025 - 31.07.2025",
    entries: [
      {
        type: LabeledDataElementType.INLINE,
        label: "Avtalt ukespris per tiltaksplass",
        value: {
          type: "DATA_ELEMENT_TEXT",
          value: "4500",
          format: DataElementTextFormat.NUMBER,
        },
      },
      {
        type: LabeledDataElementType.INLINE,
        label: "Antall ukesverk",
        value: {
          type: "DATA_ELEMENT_TEXT",
          value: "11,8",
          format: DataElementTextFormat.NUMBER,
        },
      },
    ],
  },
];

export const aftUtbetalt: ArrangorflateUtbetalingDto = {
  kanRegenereres: false,
  regenerertId: null,
  kanAvbrytes: ArrangorAvbrytStatus.DEACTIVATED,
  avbruttDato: null,
  id: "e48f9b35-855f-43aa-8b4d-a669013df34b",
  status: ArrangorflateUtbetalingStatus.UTBETALT,
  innsendtAvArrangorDato: "2025-05-15",
  utbetalesTidligstDato: "2025-04-01",
  createdAt: "2025-03-17T12:27:55.465944",
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  gjennomforing: {
    id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
    navn: "AFT Foobar",
    lopenummer: "2025/10000",
  },
  arrangor: arrangorMock,
  kanViseBeregning: true,
  valuta: Valuta.NOK,
  beregning: {
    displayName: "Sats per tiltaksplass per måned",
    satsDetaljer: satsDetaljerForhondsgodkjent,
    pris: { belop: 10149, valuta: Valuta.NOK },
    digest: "b3602d2a",
    deltakelser: toSatserUkesverkDeltakelse,
    stengt: [],
  },
  advarsler: [],
  betalingsinformasjon: { kontonummer: "63728787114", kid: "2851777587" },
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
      pris: { belop: 10149, valuta: Valuta.NOK },
    },
  ],
  innsendingsDetaljer: [
    {
      type: LabeledDataElementType.INLINE,
      label: "Dato innsendt",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "2025-05-15",
        format: DataElementTextFormat.DATE,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltaksnavn",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "AFT Foobar",
        format: null,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltakstype",
      value: { type: "DATA_ELEMENT_TEXT", value: "Oppfølging", format: null },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltaksperiode",
      value: { type: "DATA_ELEMENT_TEXT", value: "01.01.2025 - 30.12.2026", format: null },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Løpenummer",
      value: { type: "DATA_ELEMENT_TEXT", value: "2025/11181", format: null },
    },
  ],
};

export const avklaringManedKlarTilGodkjenning: ArrangorflateUtbetalingDto = {
  kanRegenereres: false,
  regenerertId: null,
  kanAvbrytes: ArrangorAvbrytStatus.DEACTIVATED,
  avbruttDato: null,
  id: "a134c0bf-40eb-4124-8f2e-df7b7c51fd44",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  innsendtAvArrangorDato: null,
  kanViseBeregning: true,
  utbetalesTidligstDato: "2025-12-01",
  createdAt: "2025-11-07T10:02:43.989186",
  tiltakstype: { navn: "Avklaring", tiltakskode: Tiltakskode.AVKLARING },
  gjennomforing: {
    id: "70cdc182-8913-48c0-bad9-fa4e74f3288e",
    navn: "Avklaring - avtalt månedspris",
    lopenummer: "2025/10001",
  },
  arrangor: arrangorMock,
  betalingsinformasjon: { kontonummer: "63728787114", kid: "2851777587" },
  valuta: Valuta.NOK,
  beregning: {
    pris: { belop: 20000, valuta: Valuta.NOK },
    digest: "ca0a6c20",
    satsDetaljer: satsDetaljerManedspris,
    deltakelser: avklaringManedDeltakelse,
    stengt: [],
    displayName: "Avtalt månedspris per tiltaksplass",
  },
  periode: { start: "2025-10-01", slutt: "2025-11-01" },
  type: { displayName: "Innsending", displayNameLong: null, tagName: null },
  linjer: [],
  advarsler: [],
  innsendingsDetaljer: [
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltaksnavn",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "Avklaring - avtalt månedspris",
        format: null,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltakstype",
      value: { type: "DATA_ELEMENT_TEXT", value: "Avklaring", format: null },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Løpenummer",
      value: { type: "DATA_ELEMENT_TEXT", value: "2025/10001", format: null },
    },
  ],
};

export const aftKreverEndring: ArrangorflateUtbetalingDto = {
  kanRegenereres: false,
  regenerertId: null,
  kanAvbrytes: ArrangorAvbrytStatus.DEACTIVATED,
  avbruttDato: null,
  id: "a5499e34-9fb4-49d1-a37d-11810f6df19b",
  status: ArrangorflateUtbetalingStatus.KREVER_ENDRING,
  innsendtAvArrangorDato: null,
  utbetalesTidligstDato: null,
  createdAt: "2025-06-17T13:58:12.405867",
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  gjennomforing: {
    id: "b3e1cfbb-bfb5-4b4b-b8a4-af837631ed51",
    navn: "Solrikt AFT",
    lopenummer: "2025/10002",
  },
  arrangor: arrangorMock,
  advarsler: [],
  kanViseBeregning: true,
  valuta: Valuta.NOK,
  beregning: {
    displayName: "Sats per tiltaksplass per måned",
    satsDetaljer: satsDetaljerForhondsgodkjent,
    pris: { belop: 242904, valuta: Valuta.NOK },
    digest: "db0c7c6e",
    deltakelser: toSatserUkesverkDeltakelse,
    stengt: [],
  },
  betalingsinformasjon: { kontonummer: "63728787114", kid: "2851777587" },
  periode: { start: "2025-01-01", slutt: "2025-02-01" },
  type: utbetalingType.INNSENDING,
  linjer: [],
  innsendingsDetaljer: [
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltaksnavn",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "Solrikt AFT",
        format: null,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltakstype",
      value: { type: "DATA_ELEMENT_TEXT", value: "Arbeidsforberedende trening", format: null },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Løpenummer",
      value: { type: "DATA_ELEMENT_TEXT", value: "2025/10002", format: null },
    },
  ],
};

export const aftBehandlesAvNav: ArrangorflateUtbetalingDto = {
  kanRegenereres: false,
  regenerertId: null,
  kanAvbrytes: ArrangorAvbrytStatus.DEACTIVATED,
  avbruttDato: null,
  id: "585a2834-338a-4ac7-82e0-e1b08bfe1408",
  status: ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
  innsendtAvArrangorDato: "2025-06-05",
  utbetalesTidligstDato: null,
  createdAt: "2025-06-05T09:36:11.510957",
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  advarsler: [],
  gjennomforing: {
    id: "d29cb67c-8e68-4ece-90dc-ff21c498aa3f",
    navn: "AFT - Arbeidsforberedende trening - Team tiltakspenger",
    lopenummer: "2025/10003",
  },
  arrangor: arrangorMock,
  kanViseBeregning: false,
  valuta: Valuta.NOK,
  beregning: {
    deltakelser: null,
    stengt: [],
    displayName: "Annen avtalt pris",
    satsDetaljer: [],
    pris: { belop: 234, valuta: Valuta.NOK },
    digest: "000001d4",
  },
  betalingsinformasjon: { kontonummer: "63728787114", kid: "2851777587" },
  periode: { start: "2025-05-01", slutt: "2025-08-02" },
  type: utbetalingType.INVESTERING,
  linjer: [],
  innsendingsDetaljer: [
    {
      type: LabeledDataElementType.INLINE,
      label: "Dato innsendt",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "2025-06-05",
        format: DataElementTextFormat.DATE,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltaksnavn",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "AFT - Arbeidsforberedende trening - Team tiltakspenger",
        format: null,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltakstype",
      value: { type: "DATA_ELEMENT_TEXT", value: "Arbeidsforberedende trening", format: null },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Løpenummer",
      value: { type: "DATA_ELEMENT_TEXT", value: "2025/10003", format: null },
    },
  ],
};

export const avklaringOverfortTilUtbetaling: ArrangorflateUtbetalingDto = {
  kanRegenereres: false,
  regenerertId: null,
  kanAvbrytes: ArrangorAvbrytStatus.DEACTIVATED,
  avbruttDato: null,
  id: "153bc6f0-0c5f-4555-9447-b88ea0cc60f2",
  status: ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
  innsendtAvArrangorDato: null,
  utbetalesTidligstDato: "2025-05-01",
  createdAt: "2025-04-01T12:03:49.756508",
  tiltakstype: {
    navn: "Avklaring",
    tiltakskode: Tiltakskode.AVKLARING,
  },
  gjennomforing: {
    id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
    navn: "AFT Foobar",
    lopenummer: "2025/10004",
  },
  arrangor: arrangorMock,
  kanViseBeregning: false,
  valuta: Valuta.NOK,
  beregning: {
    stengt: [],
    deltakelser: null,
    displayName: "Annen avtalt pris",
    satsDetaljer: [],
    pris: { belop: 500, valuta: Valuta.NOK },
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
      pris: { belop: 500, valuta: Valuta.NOK },
    },
  ],
  advarsler: [],
  innsendingsDetaljer: [
    {
      type: LabeledDataElementType.INLINE,
      label: "Dato opprettet hos Nav",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "2025-04-01",
        format: DataElementTextFormat.DATE,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltaksnavn",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "AFT Foobar",
        format: null,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltakstype",
      value: { type: "DATA_ELEMENT_TEXT", value: "Avklaring", format: null },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Løpenummer",
      value: { type: "DATA_ELEMENT_TEXT", value: "2025/10004", format: null },
    },
  ],
};

export const vtaKlarForGodkjenning: ArrangorflateUtbetalingDto = {
  kanRegenereres: false,
  regenerertId: null,
  kanAvbrytes: ArrangorAvbrytStatus.DEACTIVATED,
  avbruttDato: null,
  id: "fdbb7433-b42e-4cd6-b995-74a8e487329f",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  innsendtAvArrangorDato: null,
  utbetalesTidligstDato: null,
  createdAt: "2025-07-07T05:00:06.924181",
  tiltakstype: {
    navn: "Varig tilrettelagt arbeid i skjermet virksomhet",
    tiltakskode: Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
  },
  gjennomforing: {
    id: "6a760ab8-fb12-4c6e-b143-b711331f63f6",
    navn: "May rain - VTA",
    lopenummer: "2025/10005",
  },
  arrangor: arrangorMock,
  kanViseBeregning: true,
  valuta: Valuta.NOK,
  beregning: {
    displayName: "Sats per tiltaksplass per måned",
    satsDetaljer: satsDetaljerForhondsgodkjent,
    pris: { belop: 16848, valuta: Valuta.NOK },
    digest: "38c07a43",
    deltakelser: vtaManedDeltakelse,
    stengt: [],
  },
  betalingsinformasjon: { kontonummer: "63728787114", kid: "2851777587" },
  periode: { start: "2025-06-01", slutt: "2025-07-01" },
  type: utbetalingType.INNSENDING,
  linjer: [],
  advarsler: [],
  innsendingsDetaljer: [
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltaksnavn",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "May rain - VTA",
        format: null,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltakstype",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "Varig tilrettelagt arbeid i skjermet virksomhet",
        format: null,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Løpenummer",
      value: { type: "DATA_ELEMENT_TEXT", value: "2025/10005", format: null },
    },
  ],
};

export const arrUkesprisKlarTilGodkjenning: ArrangorflateUtbetalingDto = {
  kanAvbrytes: ArrangorAvbrytStatus.DEACTIVATED,
  avbruttDato: null,
  kanRegenereres: false,
  regenerertId: null,
  id: "ba046f93-cb0c-4acf-a724-99a36481f183",
  status: ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
  innsendtAvArrangorDato: null,
  utbetalesTidligstDato: "2025-12-01",
  kanViseBeregning: true,
  createdAt: "2025-10-31T14:41:03.835624",
  tiltakstype: {
    navn: "Arbeidsrettet rehabilitering",
    tiltakskode: Tiltakskode.ARBEIDSRETTET_REHABILITERING,
  },
  gjennomforing: {
    id: "a47092ba-410b-4ca1-9713-36506a039742",
    navn: "Arbeidsrettet rehabilitering - avtalt ukespris",
    lopenummer: "2025/10006",
  },
  arrangor: arrangorMock,
  betalingsinformasjon: { kontonummer: "63728787114", kid: "2851777587" },
  valuta: Valuta.NOK,
  beregning: {
    pris: { belop: 53100, valuta: Valuta.NOK },
    digest: "28172363",
    satsDetaljer: satsDetaljerUkespris,
    deltakelser: arrUkesverkDeltakelse,
    stengt: [],
    displayName: "Avtalt ukespris per tiltaksplass",
  },
  periode: { start: "2025-10-01", slutt: "2025-11-01" },
  type: { displayName: "Innsending", displayNameLong: null, tagName: null },
  linjer: [],
  advarsler: [],
  innsendingsDetaljer: [
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltaksnavn",
      value: {
        type: "DATA_ELEMENT_TEXT",
        value: "Arbeidsrettet rehabilitering - avtalt ukespris",
        format: null,
      },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Tiltakstype",
      value: { type: "DATA_ELEMENT_TEXT", value: "Arbeidsrettet rehabilitering", format: null },
    },
    {
      type: LabeledDataElementType.INLINE,
      label: "Løpenummer",
      value: { type: "DATA_ELEMENT_TEXT", value: "2025/10006", format: null },
    },
  ],
};

export const arrFlateUtbetaling: ArrangorflateUtbetalingDto[] = [
  aftUtbetalt,
  aftKreverEndring,
  aftBehandlesAvNav,
  avklaringOverfortTilUtbetaling,
  vtaKlarForGodkjenning,
  arrUkesprisKlarTilGodkjenning,
  avklaringManedKlarTilGodkjenning,
];

export const klarForGodkjenningIds: string[] = arrFlateUtbetaling
  .filter((utb) => utb.status == ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING)
  .map((utb) => utb.id);
