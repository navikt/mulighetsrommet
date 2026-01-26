import { HStack } from "@navikt/ds-react";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { TilsagnDto } from "@tiltaksadministrasjon/api-client";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";

interface TilsagnInformasjonProps {
  tilsagn: TilsagnDto;
}

export function TilsagnInformasjon({ tilsagn }: TilsagnInformasjonProps) {
  return (
    <HStack gap="4">
      <MetadataVStack label="Totalbeløp på tilsagn" value={formaterValutaBelop(tilsagn.pris)} />
    </HStack>
  );
}
