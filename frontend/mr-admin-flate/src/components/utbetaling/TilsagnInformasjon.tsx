import { HStack } from "@navikt/ds-react";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { TilsagnDeltakerDto, TilsagnDto } from "@tiltaksadministrasjon/api-client";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { tilsagnTekster } from "../tilsagn/TilsagnTekster";
import { TilsagnDeltakerCompact } from "../personalia/TilsagnDeltakerCompact";

interface TilsagnInformasjonProps {
  tilsagn: TilsagnDto;
  deltakere: TilsagnDeltakerDto[];
}

export function TilsagnInformasjon({ tilsagn, deltakere }: TilsagnInformasjonProps) {
  return (
    <HStack gap="space-16">
      <MetadataVStack label="Totalbeløp på tilsagn" value={formaterValutaBelop(tilsagn.pris)} />
      {deltakere.length > 0 && (
        <MetadataVStack
          label={tilsagnTekster.deltakere.label}
          value={
            <ul>
              {deltakere.map((d) => {
                return (
                  <li key={d.deltakerId}>
                    <TilsagnDeltakerCompact deltaker={d} />
                  </li>
                );
              })}
            </ul>
          }
        />
      )}
    </HStack>
  );
}
