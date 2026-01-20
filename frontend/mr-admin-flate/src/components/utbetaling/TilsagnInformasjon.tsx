import { HStack } from "@navikt/ds-react";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { TilsagnDto } from "@tiltaksadministrasjon/api-client";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";

interface TilsagnInformasjonProps {
  tilsagn: TilsagnDto;
}

export function TilsagnInformasjon({ tilsagn }: TilsagnInformasjonProps) {
  return (
    <HStack gap="4">
      <MetadataVStack
        label="Totalbeløp på tilsagn"
        value={formaterValuta(tilsagn.belop.belop, tilsagn.belop.valuta)}
      />
    </HStack>
  );
}
