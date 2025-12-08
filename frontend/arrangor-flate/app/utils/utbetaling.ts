import { formaterDato } from "@mr/frontend-common/utils/date";
import { ArrangorflateUtbetalingDto } from "api-client";
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
