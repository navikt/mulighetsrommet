import { ArrFlateBeregning } from "api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Definisjonsliste, Definition } from "../Definisjonsliste";

interface Props {
  beregning: ArrFlateBeregning;
}

export function BeregningDetaljer({ beregning }: Props) {
  const beregningDetaljer = getDetaljer(beregning);

  return <Definisjonsliste className="mt-4" definitions={beregningDetaljer} />;
}

function getDetaljer(beregning: ArrFlateBeregning): Definition[] {
  switch (beregning.type) {
    case "FORHANDSGODKJENT":
      return [
        { key: "Antall månedsverk", value: String(beregning.antallManedsverk) },
        { key: "Total utbetaling", value: formaterNOK(beregning.belop) },
      ];
    case "FRI":
      return [{ key: "Beløp til utbetaling", value: formaterNOK(beregning.belop) }];
  }
}
