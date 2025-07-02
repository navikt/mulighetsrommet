import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ArrFlateBeregning } from "../../api-client";
import { Definition } from "~/components/Definisjonsliste";

export function getBeregningDetaljer(beregning: ArrFlateBeregning): Definition[] {
  switch (beregning.type) {
    case "AVTALT_PRIS_PER_MANEDSVERK":
      return [
        { key: "Antall månedsverk", value: String(beregning.antallManedsverk) },
        { key: "Beløp", value: formaterNOK(beregning.belop) },
      ];
    case "FRI":
      return [{ key: "Beløp", value: formaterNOK(beregning.belop) }];
  }
}
