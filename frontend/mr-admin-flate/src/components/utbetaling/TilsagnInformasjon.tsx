import { HStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { TilsagnDto } from "@tiltaksadministrasjon/api-client";

interface TilsagnInformasjonProps {
  tilsagn: TilsagnDto;
}

export function TilsagnInformasjon({ tilsagn }: TilsagnInformasjonProps) {
  return (
    <HStack gap="4">
      <Metadata header="Totalbeløp på tilsagn" value={formaterNOK(tilsagn.belop)} />
    </HStack>
  );
}
