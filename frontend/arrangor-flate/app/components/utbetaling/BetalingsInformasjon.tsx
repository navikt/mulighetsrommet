import { Definisjonsliste } from "../Definisjonsliste";
import { ArrFlateUtbetaling } from "api-client";
import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";

interface Props {
  utbetaling: ArrFlateUtbetaling;
}

export default function BetalingsInformasjon({ utbetaling }: Props) {
  return (
    <Definisjonsliste
      title="Betalingsinformasjon"
      headingLevel="3"
      definitions={[
        {
          key: "Kontonummer",
          value: formaterKontoNummer(utbetaling.betalingsinformasjon.kontonummer),
        },
        {
          key: "KID-nummer",
          value: utbetaling.betalingsinformasjon.kid || "-",
        },
      ]}
    />
  );
}
