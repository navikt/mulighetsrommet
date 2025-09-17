import { formaterDato } from "@mr/frontend-common/utils/date";
import { ArrangorflateUtbetalingDto } from "api-client";
import { Definition } from "~/components/common/Definisjonsliste";

export function getTimestamp(utbetaling: ArrangorflateUtbetalingDto): Definition {
  if (utbetaling.godkjentAvArrangorTidspunkt) {
    return {
      key: "Dato innsendt",
      value: formaterDato(utbetaling.godkjentAvArrangorTidspunkt) ?? "-",
    };
  }

  return {
    key: "Dato opprettet hos Nav",
    value: formaterDato(utbetaling.createdAt) ?? "-",
  };
}
