import { Alert } from "@navikt/ds-react";
import { useLoaderData } from "react-router";
import { Toggles } from "@mr/api-client-v2";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { utbetalingskravForGjennomforingLoader } from "./utbetalingKravForGjennomforingLoader";
import { UtbetalingKravTable } from "./UtbetalingKravTable";

export function UtbetalingskravForGjennomforingContainer() {
  const { gjennomforing, refusjonskrav } =
    useLoaderData<typeof utbetalingskravForGjennomforingLoader>();

  const { data: enableOkonomi } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    [gjennomforing.tiltakstype.tiltakskode],
  );

  if (!enableOkonomi) {
    return null;
  }

  return (
    <>
      {refusjonskrav.length > 0 ? (
        <UtbetalingKravTable refusjonskrav={refusjonskrav} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen refusjonskrav for dette tiltaket
        </Alert>
      )}
    </>
  );
}
