import {
  DataElementTextFormat,
  LabeledDataElementType,
  OpprettKravInnsendingsInformasjon,
  GuidePanelType,
  OpprettKravVeiviserSteg,
  Periode,
  TilsagnStatus,
  TilsagnType,
  Tiltakskode,
} from "@api-client";
import { formaterPeriode, subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import {
  arrangorMock,
  gjennomforingAFT,
  gjennomforingAvklaring,
  gjennomforingIdAFT,
  gjennomforingIdAvklaring,
  gjennomforingIdOppfolging,
  gjennomforingOppfolging,
} from "./gjennomforingMocks";

const today: Date = new Date();

export const innsendingsInformasjonAFT: OpprettKravInnsendingsInformasjon = {
  guidePanel: GuidePanelType.INVESTERING_VTA_AFT,
  definisjonsListe: [
    {
      label: "Arrangør",
      type: LabeledDataElementType.INLINE,
      value: {
        value: arrangorMock.navn,
        type: "DATA_ELEMENT_TEXT",
        format: null,
      },
    },
    {
      label: "Tiltaksnavn",
      type: LabeledDataElementType.INLINE,
      value: {
        value: gjennomforingAFT.tiltakNavn,
        type: "DATA_ELEMENT_TEXT",
        format: null,
      },
    },
    {
      label: "Tiltakstype",
      type: LabeledDataElementType.INLINE,
      value: {
        value: gjennomforingAFT.tiltakstypeNavn,
        type: "DATA_ELEMENT_TEXT",
        format: null,
      },
    },
  ],
  tilsagn: [
    {
      id: "df4553e5-6a42-4a21-85a1-e0db8b5cb70a",
      tiltakstype: {
        navn: gjennomforingAFT.tiltakstypeNavn,
        tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
      },
      gjennomforing: {
        id: gjennomforingAFT.gjennomforingId,
        navn: gjennomforingAFT.tiltakNavn,
        lopenummer: "2025/10000",
      },
      arrangor: arrangorMock,
      type: TilsagnType.INVESTERING,
      periode: tilsagnsPeriode(),
      status: TilsagnStatus.GODKJENT,
      bruktBelop: 7455,
      gjenstaendeBelop: 276722,
      beregning: {
        header: null,
        entries: [
          {
            label: "Tilsagnsperiode",
            type: LabeledDataElementType.INLINE,
            value: {
              value: formaterPeriode(tilsagnsPeriode()),
              type: "DATA_ELEMENT_TEXT",
              format: null,
            },
          },
          {
            label: "Antall plasser",
            type: LabeledDataElementType.INLINE,
            value: {
              value: "14",
              type: "DATA_ELEMENT_TEXT",
              format: DataElementTextFormat.NUMBER,
            },
          },
          {
            label: "Sats per tiltaksplass per måned",
            type: LabeledDataElementType.INLINE,
            value: {
              value: "20975",
              type: "DATA_ELEMENT_TEXT",
              format: DataElementTextFormat.NOK,
            },
          },
          {
            label: "Totalbeløp",
            type: LabeledDataElementType.INLINE,
            value: {
              value: "284177",
              type: "DATA_ELEMENT_TEXT",
              format: DataElementTextFormat.NOK,
            },
          },
          {
            label: "Gjenstående beløp",
            type: LabeledDataElementType.INLINE,
            value: {
              value: "276722",
              type: "DATA_ELEMENT_TEXT",
              format: DataElementTextFormat.NOK,
            },
          },
        ],
      },
      bestillingsnummer: "A-2025/12345-1",
      beskrivelse: null,
    },
  ],
  datoVelger: { type: "DatoVelgerRange", maksSluttdato: yyyyMMddFormatting(new Date())! },
  navigering: { tilbake: null, neste: OpprettKravVeiviserSteg.UTBETALING },
};

const innsendingsInformasjonAvklaring: OpprettKravInnsendingsInformasjon = {
  guidePanel: GuidePanelType.AVTALT_PRIS,
  definisjonsListe: [
    {
      label: "Arrangør",
      type: LabeledDataElementType.INLINE,
      value: {
        value: arrangorMock.navn,
        type: "DATA_ELEMENT_TEXT",
        format: null,
      },
    },
    {
      label: "Tiltaksnavn",
      type: LabeledDataElementType.INLINE,
      value: {
        value: gjennomforingAvklaring.tiltakNavn,
        type: "DATA_ELEMENT_TEXT",
        format: null,
      },
    },
    {
      label: "Tiltakstype",
      type: LabeledDataElementType.INLINE,
      value: {
        value: gjennomforingAvklaring.tiltakstypeNavn,
        type: "DATA_ELEMENT_TEXT",
        format: null,
      },
    },
  ],
  tilsagn: [
    {
      id: "b0a3c090-1f8c-44f3-b334-2b22022b3ce9",
      tiltakstype: {
        navn: gjennomforingAvklaring.tiltakstypeNavn,
        tiltakskode: Tiltakskode.AVKLARING,
      },
      gjennomforing: {
        id: gjennomforingAvklaring.gjennomforingId,
        navn: gjennomforingAvklaring.tiltakNavn,
        lopenummer: "2025/10001",
      },
      arrangor: arrangorMock,
      type: TilsagnType.TILSAGN,
      periode: tilsagnsPeriode(),
      status: TilsagnStatus.GODKJENT,
      bruktBelop: 13000,
      gjenstaendeBelop: 351058,
      beregning: {
        header: null,
        entries: [
          {
            type: LabeledDataElementType.INLINE,
            label: "Tilsagnsperiode",
            value: {
              type: "DATA_ELEMENT_PERIODE",
              start: "01.11.2025",
              slutt: "31.12.2025",
            },
          },
          {
            type: LabeledDataElementType.INLINE,
            label: "Totalbeløp",
            value: {
              type: "DATA_ELEMENT_TEXT",
              value: "364058",
              format: DataElementTextFormat.NOK,
            },
          },
          {
            type: LabeledDataElementType.INLINE,
            label: "Gjenstående beløp",
            value: {
              type: "DATA_ELEMENT_TEXT",
              value: "351058",
              format: DataElementTextFormat.NOK,
            },
          },
        ],
      },
      bestillingsnummer: "A-2025/82143-1",
      beskrivelse: null,
    },
  ],
  datoVelger: {
    type: "DatoVelgerRange",
    maksSluttdato: new Date(31, 12, today.getFullYear()).toISOString().slice(0, 8),
  },
  navigering: { tilbake: null, neste: OpprettKravVeiviserSteg.UTBETALING },
};

const innsendingsInformasjonOppfolging: OpprettKravInnsendingsInformasjon = {
  guidePanel: GuidePanelType.TIMESPRIS,
  definisjonsListe: [
    {
      label: "Arrangør",
      type: LabeledDataElementType.INLINE,
      value: {
        value: arrangorMock.navn,
        format: null,
        type: "DATA_ELEMENT_TEXT",
      },
    },
    {
      label: "Tiltaksnavn",
      type: LabeledDataElementType.INLINE,
      value: {
        value: gjennomforingOppfolging.tiltakNavn,
        type: "DATA_ELEMENT_TEXT",
        format: null,
      },
    },
    {
      label: "Tiltakstype",
      type: LabeledDataElementType.INLINE,
      value: {
        value: gjennomforingOppfolging.tiltakstypeNavn,
        type: "DATA_ELEMENT_TEXT",
        format: null,
      },
    },
  ],
  tilsagn: [
    {
      id: "6e716b3b-6a85-4791-a8bf-fa90a5dfb6be",
      tiltakstype: {
        navn: gjennomforingOppfolging.tiltakstypeNavn,
        tiltakskode: Tiltakskode.OPPFOLGING,
      },
      gjennomforing: {
        id: gjennomforingOppfolging.gjennomforingId,
        navn: gjennomforingOppfolging.tiltakNavn,
        lopenummer: "2025/10002",
      },
      arrangor: arrangorMock,
      type: TilsagnType.TILSAGN,
      periode: tilsagnsPeriode(),
      status: TilsagnStatus.GODKJENT,
      bruktBelop: 0,
      gjenstaendeBelop: 30720,
      beregning: {
        header: null,
        entries: [
          {
            type: LabeledDataElementType.INLINE,
            label: "Tilsagnsperiode",
            value: {
              type: "DATA_ELEMENT_PERIODE",
              start: "01.10.2025",
              slutt: "31.12.2025",
            },
          },
          {
            type: LabeledDataElementType.INLINE,
            label: "Antall plasser",
            value: {
              type: "DATA_ELEMENT_TEXT",
              value: "20",
              format: DataElementTextFormat.NUMBER,
            },
          },
          {
            type: LabeledDataElementType.INLINE,
            label: "Pris per time oppfølging",
            value: {
              type: "DATA_ELEMENT_TEXT",
              value: "768",
              format: DataElementTextFormat.NOK,
            },
          },
          {
            type: LabeledDataElementType.INLINE,
            label: "Totalbeløp",
            value: {
              type: "DATA_ELEMENT_TEXT",
              value: "30720",
              format: DataElementTextFormat.NOK,
            },
          },
          {
            type: LabeledDataElementType.INLINE,
            label: "Gjenstående beløp",
            value: {
              type: "DATA_ELEMENT_TEXT",
              value: "30720",
              format: DataElementTextFormat.NOK,
            },
          },
        ],
      },
      bestillingsnummer: "A-2025/9123-1",
      beskrivelse: null,
    },
  ],
  datoVelger: {
    type: "DatoVelgerSelect",
    periodeForslag: utbetalingsPerioder(),
  },
  navigering: { tilbake: null, neste: OpprettKravVeiviserSteg.DELTAKERLISTE },
};

function tilsagnsPeriode(): Periode {
  if (today.getMonth() > 6) {
    return {
      start: yyyyMMddFormatting(new Date(today.getFullYear(), 6, 1))!,
      slutt: yyyyMMddFormatting(new Date(today.getFullYear(), 11, 31))!,
    };
  }

  return {
    start: yyyyMMddFormatting(new Date(today.getFullYear(), 1, 1))!,
    slutt: yyyyMMddFormatting(new Date(today.getFullYear(), 5, 30))!,
  };
}

function utbetalingsPerioder(): Periode[] {
  const ekslusivSluttDato = new Date(today.getFullYear(), today.getMonth(), 1);
  const periodeList = [];
  for (let i = 0; i < 6; i++) {
    const sluttdato = subDuration(ekslusivSluttDato, { months: i });
    periodeList.push({
      start: yyyyMMddFormatting(subDuration(sluttdato, { months: 1 }))!,
      slutt: yyyyMMddFormatting(sluttdato)!,
    });
  }

  return periodeList.sort();
}

export const innsendingsInformasjon: Record<string, OpprettKravInnsendingsInformasjon> = {
  [gjennomforingIdAFT]: innsendingsInformasjonAFT,
  [gjennomforingIdAvklaring]: innsendingsInformasjonAvklaring,
  [gjennomforingIdOppfolging]: innsendingsInformasjonOppfolging,
};
