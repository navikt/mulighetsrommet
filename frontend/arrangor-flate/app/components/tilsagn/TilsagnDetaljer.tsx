import { ArrangorflateTilsagnDto } from "api-client";
import { tekster } from "~/tekster";
import { TilsagnStatusTag } from "./TilsagnStatusTag";
import {
  MetadataFritekstfelt,
  MetadataHGrid,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Heading, VStack } from "@navikt/ds-react";
import { getDataElement } from "@mr/frontend-common";

interface Props {
  tilsagn: ArrangorflateTilsagnDto;
  minimal?: boolean;
  headingLevel?: "3" | "4";
}

export function TilsagnDetaljer({ tilsagn, headingLevel, minimal = false }: Props) {
  return (
    <Box
      padding="space-16"
      borderColor="neutral-subtle"
      borderWidth="1"
      borderRadius="8"
      maxWidth="max-content"
    >
      <Heading size={headingLevel == "4" ? "small" : "medium"} spacing>
        {`${tekster.bokmal.tilsagn.tilsagntype(tilsagn.type)} ${tilsagn.bestillingsnummer}`}
      </Heading>
      <VStack gap="space-4">
        {tilsagn.beskrivelse && (
          <MetadataFritekstfelt label="Beskrivelse" value={tilsagn.beskrivelse} />
        )}
        {!minimal && (
          <>
            <MetadataHGrid label="Status" value={<TilsagnStatusTag status={tilsagn.status} />} />
            <MetadataHGrid label="Tiltakstype" value={tilsagn.tiltakstype.navn} />
            <MetadataHGrid
              label="Tiltaksnavn"
              value={`${tilsagn.gjennomforing.navn} (${tilsagn.gjennomforing.lopenummer})`}
            />
          </>
        )}
        {tilsagn.beregning.entries.map((entry) => (
          <MetadataHGrid
            key={entry.label}
            label={entry.label}
            value={entry.value ? getDataElement(entry.value) : null}
          />
        ))}
      </VStack>
    </Box>
  );
}
