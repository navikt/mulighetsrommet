import { Utbetaling } from "@mr/api-client-v2";
import { VStack } from "@navikt/ds-react";

interface Props {
  utbetaling: Utbetaling;
}

export function UtbetalingDetaljer({ utbetaling }: Props) {
  return (
    <VStack>
      <div>Utbetaling detaljer side</div>
      <div>{utbetaling.krav.id}</div>
      <div>Not implemented</div>
    </VStack>
  );
}
