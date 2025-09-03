import { DelutbetalingStatus, TotrinnskontrollDto } from "@tiltaksadministrasjon/api-client";
import { HStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { getAgentDisplayName, isBesluttet } from "@/utils/totrinnskontroll";

interface BehandlerInformasjonProps {
  status: DelutbetalingStatus;
  opprettelse: TotrinnskontrollDto;
}

export function BehandlerInformasjon({ status, opprettelse }: BehandlerInformasjonProps) {
  return (
    <HStack gap="4">
      <Metadata header="Behandlet av" value={getAgentDisplayName(opprettelse.behandletAv)} />
      {status === DelutbetalingStatus.RETURNERT && isBesluttet(opprettelse) ? (
        <Metadata header="Returnert av" value={getAgentDisplayName(opprettelse.besluttetAv)} />
      ) : isBesluttet(opprettelse) &&
        [
          DelutbetalingStatus.GODKJENT,
          DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
          DelutbetalingStatus.UTBETALT,
        ].includes(status) ? (
        <Metadata header="Attestert av" value={getAgentDisplayName(opprettelse.besluttetAv)} />
      ) : null}
    </HStack>
  );
}
