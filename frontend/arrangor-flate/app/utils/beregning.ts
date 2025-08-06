import { ArrFlateBeregning } from "@api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Definition } from "~/components/common/Definisjonsliste";

export function getBeregningDetaljer(beregning: ArrFlateBeregning): Definition[] {
  switch (beregning.type) {
    case "ArrFlateBeregningFri":
      return [{ key: "Beløp", value: formaterNOK(beregning.belop) }];
    case "ArrFlateBeregningPrisPerManedsverkMedDeltakelsesmengder":
    case "ArrFlateBeregningPrisPerManedsverk":
      return [
        { key: "Antall månedsverk", value: String(beregning.antallManedsverk) },
        { key: "Sats", value: formaterNOK(beregning.sats) },
        { key: "Beløp", value: formaterNOK(beregning.belop) },
      ];
    case "ArrFlateBeregningPrisPerUkesverk":
      return [
        { key: "Antall ukesverk", value: String(beregning.antallUkesverk) },
        { key: "Sats", value: formaterNOK(beregning.sats) },
        { key: "Beløp", value: formaterNOK(beregning.belop) },
      ];
    case undefined:
      throw new Error('"type" mangler fra beregning');
  }
}
