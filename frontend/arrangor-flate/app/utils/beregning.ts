import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ArrFlateBeregning } from "../../api-client";
import { Definition } from "../components/Definisjonsliste";

export function getBeregningDetaljer(beregning: ArrFlateBeregning): Definition[] {
  switch (beregning.type) {
    case "FORHANDSGODKJENT":
      return [
        { key: "Antall månedsverk", value: String(beregning.antallManedsverk) },
        { key: "Beløp til utbetaling", value: formaterNOK(beregning.belop) },
      ];
    case "FRI":
      return [{ key: "Beløp til utbetaling", value: formaterNOK(beregning.belop) }];
  }
}
