import { Alert } from "@navikt/ds-react";
import { useLoaderData } from "react-router";
import { Toggles } from "@mr/api-client";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { RefusjonskravTabell } from "../refusjonskrav/RefusjonskravTabell";
import { refusjonskravForGjennomforingLoader } from "./refusjonskravForGjennomforingLoader";

export function RefusjonskravForGjennomforingContainer() {
  const { gjennomforing, refusjonskrav } =
    useLoaderData<typeof refusjonskravForGjennomforingLoader>();

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
        <RefusjonskravTabell refusjonskrav={refusjonskrav} />
      ) : (
        <Alert style={{ marginTop: "1rem" }} variant="info">
          Det finnes ingen refusjonskrav for dette tiltaket
        </Alert>
      )}
    </>
  );
}
