import { Alert } from "@navikt/ds-react";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTilskuddUtbetalingerByGjennomforing } from "@/api/utbetaling/useTilskuddUtbetalingerByGjennomforing";
import { TilskuddUtbetalingTable } from "@/components/utbetaling/TilskuddUtbetalingTable";

export function TilskuddUtbetalingerForGjennomforingContainer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: utbetalinger } = useTilskuddUtbetalingerByGjennomforing(gjennomforingId);

  return (
    <>
      {utbetalinger.length > 0 ? (
        <TilskuddUtbetalingTable gjennomforingId={gjennomforingId} utbetalinger={utbetalinger} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen utbetalinger for dette tiltaket
        </Alert>
      )}
    </>
  );
}
