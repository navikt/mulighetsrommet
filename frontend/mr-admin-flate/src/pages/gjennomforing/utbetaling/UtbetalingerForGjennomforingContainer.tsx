import { GjennomforingHandling } from "@tiltaksadministrasjon/api-client";
import { ActionMenu, Alert } from "@navikt/ds-react";
import { useNavigate } from "react-router";
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

  const navigate = useNavigate();

  return (
    <>
      <KnapperadContainer>
        <Handlinger>
          {handlinger.includes(GjennomforingHandling.OPPRETT_UTBETALING) && (
            <ActionMenu.Item
              onClick={() => {
                navigate("opprett-utbetaling");
              }}
            >
              Opprett utbetaling for anskaffelse
            </ActionMenu.Item>
          )}
        </Handlinger>
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
