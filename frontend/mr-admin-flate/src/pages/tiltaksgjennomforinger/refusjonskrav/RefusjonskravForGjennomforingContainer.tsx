import { Alert } from "@navikt/ds-react";
import { useLoaderData } from "react-router-dom";
import { Laster } from "@/components/laster/Laster";
import { InfoContainer } from "@/components/skjema/InfoContainer";
import { Toggles } from "@mr/api-client";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { RefusjonskravTabell } from "../refusjonskrav/RefusjonskravTabell";
import { refusjonskravForGjennomforingLoader } from "./refusjonskravForGjennomforingLoader";

export function RefusjonskravForGjennomforingContainer() {
  const { refusjonskrav } = useLoaderData<typeof refusjonskravForGjennomforingLoader>();
  const { data: enableOpprettTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPPRETT_TILSAGN,
  );

  if (!enableOpprettTilsagn) {
    return null;
  }

  if (!refusjonskrav) {
    return <Laster tekst="Laster refusjonskrav" />;
  }

  return (
    <>
      <InfoContainer>
        {refusjonskrav.length > 0 ? (
          <RefusjonskravTabell refusjonskrav={refusjonskrav} />
        ) : (
          <Alert style={{ marginTop: "1rem" }} variant="info">
            Det finnes ingen refusjonskrav for dette tiltaket
          </Alert>
        )}
      </InfoContainer>
    </>
  );
}
