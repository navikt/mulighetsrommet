import { GjennomforingHandling } from "@tiltaksadministrasjon/api-client";
import { Alert } from "@navikt/ds-react";
import { useGjennomforingHandlinger } from "@/api/gjennomforing/useGjennomforing";
import { useUtbetalingerByGjennomforing } from "@/api/utbetaling/useUtbetalingerByGjennomforing";
import { UtbetalingTable } from "@/components/utbetaling/UtbetalingTable";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Handlinger } from "@/components/handlinger/Handlinger";

export function UtbetalingerForGjennomforingContainer() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const handlinger = useGjennomforingHandlinger(gjennomforingId);
  const { data: utbetalinger } = useUtbetalingerByGjennomforing(gjennomforingId);

  return (
    <>
      <KnapperadContainer>
        <Handlinger
          handlinger={handlinger}
          grupper={[
            {
              items: [
                {
                  label: "Opprett utbetaling for anskaffelse",
                  href: "opprett-utbetaling",
                  handling: GjennomforingHandling.OPPRETT_UTBETALING,
                },
              ],
            },
          ]}
        />
      </KnapperadContainer>
      {utbetalinger.length > 0 ? (
        <UtbetalingTable gjennomforingId={gjennomforingId} utbetalinger={utbetalinger} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen utbetalinger for dette tiltaket
        </Alert>
      )}
    </>
  );
}
