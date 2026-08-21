import { formaterDato } from "@mr/frontend-common/utils/date";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { ArrangorflatePris, ArrangorflateUtbetalingDto } from "@arrangor-utbetalinger/api-client";
import { Definition } from "~/components/common/Definisjonsliste";

export function getUtbetalingsdato(utbetaling: ArrangorflateUtbetalingDto): Definition {
  if (utbetaling.innsendtAvArrangorDato) {
    return {
      key: "Dato innsendt",
      value: formaterDato(utbetaling.innsendtAvArrangorDato) ?? "-",
    };
  }

  return {
    key: "Dato opprettet hos Nav",
    value: formaterDato(utbetaling.createdAt) ?? "-",
  };
}

export function formaterArrangorflatePris(pris: ArrangorflatePris): string {
  return pris.type === "BEREGNET" ? formaterValutaBelop(pris.pris) : "Ikke registrert";
}
