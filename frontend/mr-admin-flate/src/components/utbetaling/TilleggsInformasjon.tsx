import { DelutbetalingStatus, TotrinnskontrollDto, UtbetalingLinje } from "@mr/api-client-v2";
import { HStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { navnEllerIdent } from "@/utils/Utils";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

interface TilleggsInformasjonProps {
  linje: UtbetalingLinje;
}

export function TilleggsInformasjon({ linje }: TilleggsInformasjonProps) {
  return (
    <HStack gap="4">
      <Metadata header="Tilsagnsbeløp" verdi={formaterNOK(linje.tilsagn.beregning.output.belop)} />
      {linje.opprettelse && (
        <BehandlerInformasjon status={linje.status} opprettelse={linje.opprettelse} />
      )}
    </HStack>
  );
}

interface BehandlerInformasjonProps {
  status: DelutbetalingStatus | undefined;
  opprettelse: TotrinnskontrollDto;
}

function BehandlerInformasjon({ status, opprettelse }: BehandlerInformasjonProps) {
  return (
    <>
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
    </>
  );
}
