import {
  DataElementTextFormat,
  LabeledDataElementType,
  OpprettKravOppsummering,
  OpprettKravOppsummeringRequest,
  OpprettKravVeiviserNavigering,
  OpprettKravVeiviserSteg,
  Periode,
} from "@api-client";
import {
  gjennomforingIdAFT,
  gjennomforingIdAvklaring,
  gjennomforingIdOppfolging,
} from "./gjennomforingMocks";
import { innsendingsInformasjon } from "./innsendingsInformasjonMocks";
import { addDuration, formaterPeriode, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";

const navigering: OpprettKravVeiviserNavigering = {
  tilbake: OpprettKravVeiviserSteg.VEDLEGG,
  neste: null,
};

export const oppsummering: Record<
  string,
  (request: OpprettKravOppsummeringRequest) => OpprettKravOppsummering
> = {
  [gjennomforingIdAFT]: (request: OpprettKravOppsummeringRequest) => {
    const periodeSlutt = request.periodeInklusiv
      ? yyyyMMddFormatting(addDuration(request.periodeSlutt, { days: 1 }))
      : request.periodeSlutt;
    const periode: Periode = {
      start: request.periodeStart,
      slutt: periodeSlutt!,
    };
    const oppsummering: OpprettKravOppsummering = {
      innsendingsInformasjon: innsendingsInformasjon[gjennomforingIdAFT].definisjonsListe,
      utbetalingInformasjon: [
        {
          label: "Utbetalingsperiode",
          type: LabeledDataElementType.INLINE,
          value: { value: formaterPeriode(periode), format: null },
        },
        {
          label: "Kontonummer",
          type: LabeledDataElementType.INLINE,
          value: { value: "12345678910", format: null },
        },
        {
          label: "Beløp",
          type: LabeledDataElementType.INLINE,
          value: { value: request.belop.toString(), format: DataElementTextFormat.NOK },
        },
      ],
      innsendingsData: {
        periode: periode,
        belop: request.belop,
        kidNummer: request.kidNummer,
        minAntallVedlegg: 1,
      },
      navigering: navigering,
    };
    return oppsummering;
  },
  [gjennomforingIdAvklaring]: (request: OpprettKravOppsummeringRequest) => {
    const periode: Periode = {
      start: request.periodeStart,
      slutt: request.periodeSlutt,
    };
    const oppsummering: OpprettKravOppsummering = {
      innsendingsInformasjon: innsendingsInformasjon[gjennomforingIdAvklaring].definisjonsListe,
      utbetalingInformasjon: [
        {
          label: "Utbetalingsperiode",
          type: LabeledDataElementType.INLINE,
          value: { value: formaterPeriode(periode), format: null },
        },
        {
          label: "Kontonummer",
          type: LabeledDataElementType.INLINE,
          value: { value: "12345678910", format: null },
        },
        {
          label: "Beløp",
          type: LabeledDataElementType.INLINE,
          value: { value: request.belop.toString(), format: DataElementTextFormat.NOK },
        },
      ],

      innsendingsData: {
        periode: periode,
        belop: request.belop,
        kidNummer: request.kidNummer,
        minAntallVedlegg: 0,
      },
      navigering: navigering,
    };
    return oppsummering;
  },
  [gjennomforingIdOppfolging]: (request: OpprettKravOppsummeringRequest) => {
    const periodeSlutt = request.periodeInklusiv
      ? yyyyMMddFormatting(addDuration(request.periodeSlutt, { days: 1 }))
      : request.periodeSlutt;
    const periode: Periode = {
      start: request.periodeStart,
      slutt: periodeSlutt!,
    };
    const oppsummering: OpprettKravOppsummering = {
      innsendingsInformasjon: innsendingsInformasjon[gjennomforingIdOppfolging].definisjonsListe,
      utbetalingInformasjon: [
        {
          label: "Utbetalingsperiode",
          type: LabeledDataElementType.INLINE,
          value: { value: formaterPeriode(periode), format: null },
        },
        {
          label: "Kontonummer",
          type: LabeledDataElementType.INLINE,
          value: { value: "12345678910", format: null },
        },
        {
          label: "Beløp",
          type: LabeledDataElementType.INLINE,
          value: { value: request.belop.toString(), format: DataElementTextFormat.NOK },
        },
      ],
      innsendingsData: {
        periode: periode,
        belop: request.belop,
        kidNummer: request.kidNummer,
        minAntallVedlegg: 1,
      },
      navigering: navigering,
    };
    return oppsummering;
  },
};

export const utbetalingMap: Record<string, string> = {
  [gjennomforingIdAFT]: "585a2834-338a-4ac7-82e0-e1b08bfe1408",
  [gjennomforingIdAvklaring]: "585a2834-338a-4ac7-82e0-e1b08bfe1408",
  [gjennomforingIdOppfolging]: "585a2834-338a-4ac7-82e0-e1b08bfe1408",
};
