import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client-v2";
import { Alert } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router";
import { useAdminGjennomforingById } from "../../../api/gjennomforing/useAdminGjennomforingById";
import { Laster } from "../../../components/laster/Laster";
import { UtbetalingerTable } from "../../../components/utbetaling/UtbetalingerTable";
import { utbetalingerByGjennomforingQuery } from "./utbetalingerForGjennomforingLoader";

export function UtbetalingerForGjennomforingContainer() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById();

  const { data: utbetalinger } = useQuery({
    ...utbetalingerByGjennomforingQuery(gjennomforingId),
  });

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    gjennomforing && [gjennomforing.tiltakstype.tiltakskode],
  );

  if (!enableOkonomi) {
    return null;
  }

  if (!utbetalinger) {
    return <Laster tekst="Laster utbetalinger..." />;
  }

  return (
    <>
      {utbetalinger.data.length > 0 ? (
        <UtbetalingerTable utbetalinger={utbetalinger.data} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen utbetalinger for dette tiltaket
        </Alert>
      )}
    </>
  );
}
