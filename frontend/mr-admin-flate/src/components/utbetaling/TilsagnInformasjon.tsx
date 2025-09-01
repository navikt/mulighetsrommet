import { TilsagnDto } from "@mr/api-client-v2";
import { HStack } from "@navikt/ds-react";
import { Metadata } from "../detaljside/Metadata";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

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
