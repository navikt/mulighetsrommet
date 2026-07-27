import { BodyShort, HStack } from "@navikt/ds-react";
import { TilsagnDeltakerDto } from "@tiltaksadministrasjon/api-client";
import { NavnOgGradering } from "./NavnOgGradering";

interface Props {
  deltaker: TilsagnDeltakerDto;
}

export function TilsagnDeltakerCompact({ deltaker }: Props) {
  return (
    <HStack gap="space-8" align="start" wrap={false}>
      <NavnOgGradering
        navn={deltaker.navn}
        gradering={deltaker.gradering}
        norskIdent={deltaker.norskIdent}
      />
      <BodyShort>{` / ${deltaker.oppfolgingEnhet?.navn ?? "-"}`}</BodyShort>
    </HStack>
  );
}
