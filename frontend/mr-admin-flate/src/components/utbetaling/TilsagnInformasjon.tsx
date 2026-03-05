import { HStack } from "@navikt/ds-react";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { TilsagnDto } from "@tiltaksadministrasjon/api-client";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { tilsagnTekster } from "../tilsagn/TilsagnTekster";
import { formatTilsagnDeltaker } from "@/utils/Utils";

interface TilsagnInformasjonProps {
  tilsagn: TilsagnDto;
}

export function TilsagnInformasjon({ tilsagn }: TilsagnInformasjonProps) {
  return (
    <HStack gap="space-16">
      <MetadataVStack label="Totalbeløp på tilsagn" value={formaterValutaBelop(tilsagn.pris)} />
      {tilsagn.deltakere.length > 0 && (
        <MetadataVStack
          label={tilsagnTekster.deltakere.label}
          value={
            <ul>
              {tilsagn.deltakere.map((d) => {
                return <li key={d.deltakerId}>{formatTilsagnDeltaker(d)}</li>;
              })}
            </ul>
          }
        />
      )}
    </HStack>
  );
}
