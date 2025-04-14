import { formaterDato, formaterPeriode } from "~/utils";
import { Definisjonsliste } from "../Definisjonsliste";
import { ArrFlateUtbetaling } from "api-client";

interface Props {
  utbetaling: ArrFlateUtbetaling;
  utenTittel?: boolean;
}

export default function GenerelleUtbetalingDetaljer({ utbetaling, utenTittel }: Props) {
  return (
    <Definisjonsliste
      title={utenTittel ? "" : "Utbetaling"}
      headingLevel="3"
      definitions={[
        {
          key: "Utbetalingsperiode",
          value: formaterPeriode(utbetaling.periode),
        },
        { key: "Frist for innsending", value: formaterDato(utbetaling.fristForGodkjenning) },
      ]}
    />
  );
}
