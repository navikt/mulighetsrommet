import {
  DetailsFormat,
  OpprettKravInnsendingsInformasjon,
  OpprettKravInnsendingsInformasjonGuidePanelType,
  OpprettKravVeiviserSteg,
  Periode,
  TilsagnStatus,
  TilsagnType,
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
  guidePanel: OpprettKravInnsendingsInformasjonGuidePanelType.INVESTERING_VTA_AFT,
  definisjonsListe: [
    {
      key: "Arrangør",
      value: arrangorMock.navn,
      format: null,
    },
    { key: "Tiltaksnavn", value: gjennomforingAFT.navn, format: null },
    { key: "Tiltakstype", value: gjennomforingAFT.tiltakstype.navn, format: null },
  ],
  tilsagn: [
    {
      id: "df4553e5-6a42-4a21-85a1-e0db8b5cb70a",
      tiltakstype: gjennomforingAFT.tiltakstype,
      gjennomforing: {
        id: gjennomforingAFT.id,
        navn: gjennomforingAFT.navn,
      },
      arrangor: arrangorMock,
      type: TilsagnType.INVESTERING,
      periode: tilsagnsPeriode(),
      status: TilsagnStatus.GODKJENT,
      bruktBelop: 7455,
      gjenstaendeBelop: 276722,
      beregning: {
        entries: [
          { key: "Tilsagnsperiode", value: formaterPeriode(tilsagnsPeriode()), format: null },
          { key: "Antall plasser", value: "14", format: DetailsFormat.NUMBER },
          { key: "Sats per tiltaksplass per måned", value: "20975", format: DetailsFormat.NOK },
          { key: "Totalbeløp", value: "284177", format: DetailsFormat.NOK },
          { key: "Gjenstående beløp", value: "276722", format: DetailsFormat.NOK },
        ],
      },
      bestillingsnummer: "A-2025/12345-1",
    },
  ],
  datoVelger: { type: "DatoVelgerRange", maksSluttdato: yyyyMMddFormatting(new Date())! },
  navigering: { tilbake: null, neste: OpprettKravVeiviserSteg.UTBETALING },
};

const innsendingsInformasjonAvklaring: OpprettKravInnsendingsInformasjon = {
  guidePanel: OpprettKravInnsendingsInformasjonGuidePanelType.AVTALT_PRIS,
  definisjonsListe: [
    {
      key: "Arrangør",
      value: arrangorMock.navn,
      format: null,
    },
    { key: "Tiltaksnavn", value: gjennomforingAvklaring.navn, format: null },
    { key: "Tiltakstype", value: gjennomforingAvklaring.tiltakstype.navn, format: null },
  ],
  tilsagn: [
    {
      id: "b0a3c090-1f8c-44f3-b334-2b22022b3ce9",
      tiltakstype: gjennomforingAvklaring.tiltakstype,
      gjennomforing: {
        id: gjennomforingAvklaring.id,
        navn: gjennomforingAvklaring.navn,
      },
      arrangor: arrangorMock,
      type: TilsagnType.TILSAGN,
      periode: tilsagnsPeriode(),
      status: TilsagnStatus.GODKJENT,
      bruktBelop: 13000,
      gjenstaendeBelop: 351058,
      beregning: {
        entries: [
          {
            key: "Tilsagnsperiode",
            value: formaterPeriode(tilsagnsPeriode()),
            format: null,
          },
          { key: "Totalbeløp", value: "364058", format: DetailsFormat.NOK },
          { key: "Gjenstående beløp", value: "351058", format: DetailsFormat.NOK },
        ],
      },
      bestillingsnummer: "A-2025/82143-1",
    },
  ],
  datoVelger: { type: "DatoVelgerRange", maksSluttdato: null },
  navigering: { tilbake: null, neste: OpprettKravVeiviserSteg.UTBETALING },
};

const innsendingsInformasjonOppfolging: OpprettKravInnsendingsInformasjon = {
  guidePanel: OpprettKravInnsendingsInformasjonGuidePanelType.TIMESPRIS,
  definisjonsListe: [
    {
      key: "Arrangør",
      value: arrangorMock.navn,
      format: null,
    },
    { key: "Tiltaksnavn", value: gjennomforingOppfolging.navn, format: null },
    { key: "Tiltakstype", value: gjennomforingOppfolging.tiltakstype.navn, format: null },
  ],
  tilsagn: [
    {
      id: "6e716b3b-6a85-4791-a8bf-fa90a5dfb6be",
      tiltakstype: gjennomforingOppfolging.tiltakstype,
      gjennomforing: {
        id: gjennomforingOppfolging.id,
        navn: gjennomforingOppfolging.navn,
      },
      arrangor: arrangorMock,
      type: TilsagnType.TILSAGN,
      periode: tilsagnsPeriode(),
      status: TilsagnStatus.GODKJENT,
      bruktBelop: 0,
      gjenstaendeBelop: 12417720,
      beregning: {
        entries: [
          { key: "Tilsagnsperiode", value: formaterPeriode(tilsagnsPeriode()), format: null },
          { key: "Antall plasser", value: "40", format: DetailsFormat.NUMBER },
          { key: "Pris per time oppfølging", value: "44349", format: DetailsFormat.NOK },
          { key: "Totalbeløp", value: "12417720", format: DetailsFormat.NOK },
          { key: "Gjenstående beløp", value: "12417720", format: DetailsFormat.NOK },
        ],
      },
      bestillingsnummer: "A-2025/9123-1",
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
