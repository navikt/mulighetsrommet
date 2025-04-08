import { DelutbetalingStatus, UtbetalingLinje } from "@mr/api-client-v2";
import { HStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { navnEllerIdent } from "@/utils/Utils";
export function BehandlerInformasjon({ linje }: { linje: UtbetalingLinje }) {
  return (
    linje.opprettelse && (
      <HStack gap="4">
        <Metadata
          header="Behandlet av"
          verdi={navnEllerIdent(linje.opprettelse.behandletAvMetadata)}
        />
        {linje.status === DelutbetalingStatus.RETURNERT &&
        linje.opprettelse.type === "BESLUTTET" ? (
          <Metadata
            header="Returnert av"
            verdi={navnEllerIdent(linje.opprettelse.besluttetAvMetadata)}
          />
        ) : linje.opprettelse.type === "BESLUTTET" &&
          linje.status &&
          [
            DelutbetalingStatus.GODKJENT,
            DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
            DelutbetalingStatus.UTBETALT,
          ].includes(linje.status) ? (
          <Metadata
            header="Attestert av"
            verdi={navnEllerIdent(linje.opprettelse.besluttetAvMetadata)}
          />
        ) : null}
      </HStack>
    )
  );
}
