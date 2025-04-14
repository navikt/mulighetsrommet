import { ArrFlateUtbetaling } from "api-client";
import { Definisjonsliste } from "../Definisjonsliste";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { formaterDato } from "~/utils";

interface Props {
  utbetaling: ArrFlateUtbetaling;
}

export default function InnsendtUtbetalingDetaljer({ utbetaling }: Props) {
  const innsendtTidspunkt = utbetaling.godkjentAvArrangorTidspunkt
    ? formaterDato(utbetaling.godkjentAvArrangorTidspunkt)
    : "-";
  return (
    <Definisjonsliste
      title={""}
      headingLevel="3"
      definitions={[
        {
          key: "Innsendt",
          value: innsendtTidspunkt,
        },
        { key: "Beløp til utbetaling", value: formaterNOK(utbetaling.beregning.belop) },
      ]}
    />
  );
}
