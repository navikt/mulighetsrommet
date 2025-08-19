import { formaterDato } from "@mr/frontend-common/utils/date";
import { ArrangorflateUtbetalingDto } from "api-client";
import { ArrangorflateBeregning } from "@api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Definition } from "~/components/common/Definisjonsliste";

export function getBeregningDetaljer(beregning: ArrangorflateBeregning): Definition[] {
  switch (beregning.type) {
    case "ArrangorflateBeregningFri":
      return [{ key: "Beløp", value: formaterNOK(beregning.belop) }];
    case "ArrangorflateBeregningPrisPerManedsverkMedDeltakelsesmengder":
    case "ArrangorflateBeregningPrisPerManedsverk":
      return [
        { key: "Antall månedsverk", value: String(beregning.antallManedsverk) },
        { key: "Sats", value: formaterNOK(beregning.sats) },
        { key: "Beløp", value: formaterNOK(beregning.belop) },
      ];
    case "ArrangorflateBeregningPrisPerUkesverk":
      return [
        { key: "Antall ukesverk", value: String(beregning.antallUkesverk) },
        { key: "Sats", value: formaterNOK(beregning.sats) },
        { key: "Beløp", value: formaterNOK(beregning.belop) },
      ];
    case undefined:
      throw new Error('"type" mangler fra beregning');
  }
}

export function getTimestamp(utbetaling: ArrangorflateUtbetalingDto): Definition {
  if (utbetaling.godkjentAvArrangorTidspunkt) {
    return {
      key: "Dato innsendt",
      value: formaterDato(utbetaling.godkjentAvArrangorTidspunkt) ?? "-",
    };
  }

  return {
    key: "Dato opprettet hos Nav",
    value: formaterDato(utbetaling.createdAt) ?? "-",
  };
}
