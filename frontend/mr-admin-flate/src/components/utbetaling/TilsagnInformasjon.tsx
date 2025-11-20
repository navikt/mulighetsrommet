import { HStack } from "@navikt/ds-react";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { TilsagnDto } from "@tiltaksadministrasjon/api-client";
import { Metadata } from "@mr/frontend-common/components/datadriven/Metadata";

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
