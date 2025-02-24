import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client-v2";
import { Alert } from "@navikt/ds-react";
import { useLoaderData } from "react-router";
import { UtbetalingerTable } from "../../../components/utbetaling/UtbetalingerTable";
import { utbetalingerForGjennomforingLoader } from "./utbetalingerForGjennomforingLoader";
import { UtbetalingerChart } from "./UtbetalingerChart";

export function UtbetalingerForGjennomforingContainer() {
  const { gjennomforing, utbetalinger } =
    useLoaderData<typeof utbetalingerForGjennomforingLoader>();

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    [gjennomforing.tiltakstype.tiltakskode],
  );
  console.log(utbetalinger);

  if (!enableOkonomi) {
    return null;
  }

  return (
    <>
      {utbetalinger.length > 0 ? (
        <>
          <UtbetalingerTable utbetalinger={utbetalinger} />
          <div className="mt-8">
            <UtbetalingerChart utbetalinger={utbetalinger} />
          </div>
        </>
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen utbetalinger for dette tiltaket
        </Alert>
      )}
    </>
  );
}
