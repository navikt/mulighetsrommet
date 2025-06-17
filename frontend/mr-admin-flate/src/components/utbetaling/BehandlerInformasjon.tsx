import { navnEllerIdent } from "@/utils/Utils";
import { DelutbetalingStatus, TotrinnskontrollDto } from "@mr/api-client-v2";
import { HStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";

interface BehandlerInformasjonProps {
  status: DelutbetalingStatus | undefined;
  opprettelse: TotrinnskontrollDto;
}

export function BehandlerInformasjon({ status, opprettelse }: BehandlerInformasjonProps) {
  return (
    <HStack gap="4">
      <Metadata header="Behandlet av" verdi={navnEllerIdent(opprettelse.behandletAv)} />
      {status === DelutbetalingStatus.RETURNERT && opprettelse.type === "BESLUTTET" ? (
        <Metadata header="Returnert av" verdi={navnEllerIdent(opprettelse.besluttetAv)} />
      ) : opprettelse.type === "BESLUTTET" &&
        status &&
        [
          DelutbetalingStatus.GODKJENT,
          DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
          DelutbetalingStatus.UTBETALT,
        ].includes(status) ? (
        <Metadata header="Attestert av" verdi={navnEllerIdent(opprettelse.besluttetAv)} />
      ) : null}
    </HStack>
  );
}
