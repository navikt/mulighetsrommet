import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client-v2";
import { Alert } from "@navikt/ds-react";
import { useLoaderData } from "react-router";
import { UtbetalingerTable } from "../../../components/utbetaling/UtbetalingerTable";
import { utbetalingerForGjennomforingLoader } from "./utbetalingerForGjennomforingLoader";
import { LoaderData } from "@/types/loader";
export function UtbetalingerForGjennomforingContainer() {
  const { gjennomforing, utbetalinger } =
    useLoaderData<LoaderData<typeof utbetalingerForGjennomforingLoader>>();

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    [gjennomforing.tiltakstype.tiltakskode],
  );

  if (!enableOkonomi) {
    return null;
  }

  return (
    <>
      {utbetalinger.length > 0 ? (
        <UtbetalingerTable utbetalinger={utbetalinger} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen utbetalinger for dette tiltaket
        </Alert>
      )}
    </>
  );
}
