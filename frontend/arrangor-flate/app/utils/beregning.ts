import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ArrFlateBeregning } from "api-client";
import { Definition } from "~/components/common/Definisjonsliste";

export function getBeregningDetaljer(beregning: ArrFlateBeregning): Definition[] {
  switch (beregning.type) {
    case "FRI":
      return [{ key: "Beløp", value: formaterNOK(beregning.belop) }];
    case "PRIS_PER_MANEDSVERK_MED_DELTAKELSESMENGDER":
    case "PRIS_PER_MANEDSVERK":
      return [
        { key: "Antall månedsverk", value: String(beregning.antallManedsverk) },
        { key: "Sats", value: formaterNOK(beregning.sats) },
        { key: "Beløp", value: formaterNOK(beregning.belop) },
      ];
    case "PRIS_PER_UKESVERK":
      return [
        { key: "Antall ukesverk", value: String(beregning.antallUkesverk) },
        { key: "Sats", value: formaterNOK(beregning.sats) },
        { key: "Beløp", value: formaterNOK(beregning.belop) },
      ];
  }
}
