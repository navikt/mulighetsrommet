import { ArrFlateUtbetaling } from "api-client";
import { Definisjonsliste } from "../Definisjonsliste";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

interface Props {
  utbetaling: ArrFlateUtbetaling;
}

export default function InnsendtUtbetalingDetaljer({ utbetaling }: Props) {
  return (
    <Definisjonsliste
      title={""}
      headingLevel="3"
      definitions={[
        {
          key: "Innsendt",
          value: "@TODO", // Eksisterer i db modellen
        },
        { key: "Beløp til utbetaling", value: formaterNOK(utbetaling.beregning.belop) },
      ]}
    />
  );
}
