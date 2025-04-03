import { DelutbetalingStatus, UtbetalingLinje } from "@mr/api-client-v2";
import { HStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";

export function BehandlerInformasjon({ linje }: { linje: UtbetalingLinje }) {
  return (
    linje.opprettelse && (
      <HStack gap="4">
        <Metadata header="Behandlet av" verdi={linje.opprettelse.behandletAv} />
        {linje.status === DelutbetalingStatus.RETURNERT &&
        linje.opprettelse.type === "BESLUTTET" ? (
          <Metadata header="Returnert av" verdi={linje.opprettelse.besluttetAv} />
        ) : linje.opprettelse.type === "BESLUTTET" &&
          linje.status === DelutbetalingStatus.GODKJENT ? (
          <Metadata header="Attestert av" verdi={linje.opprettelse.besluttetAv} />
        ) : null}
      </HStack>
    )
  );
}
