import { ArrangorflateTilsagnDto } from "api-client";
import { tekster } from "~/tekster";
import { tilsagnStatusElement } from "./TilsagnStatusTag";
import { MetadataHorisontal } from "@mr/frontend-common/components/datadriven/Metadata";
import { Heading, VStack } from "@navikt/ds-react";
import { getDataElement } from "@mr/frontend-common";

interface Props {
  tilsagn: ArrangorflateTilsagnDto;
  minimal?: boolean;
  headingLevel?: "3" | "4";
}

export function TilsagnDetaljer({ tilsagn, headingLevel, minimal = false }: Props) {
  const status = tilsagnStatusElement(tilsagn.status);
  return (
    <VStack gap="1" className="p-4 border-1 border-border-divider rounded-lg w-xl">
      <Heading size={headingLevel == "4" ? "small" : "medium"}>
        {`${tekster.bokmal.tilsagn.tilsagntype(tilsagn.type)} ${tilsagn.bestillingsnummer}`}
      </Heading>
      {!minimal && (
        <VStack gap="1">
          <MetadataHorisontal
            compact
            header="Status"
            value={status ? getDataElement(status) : null}
          />
          <MetadataHorisontal compact header="Tiltakstype" value={tilsagn.tiltakstype.navn} />
          <MetadataHorisontal compact header="Tiltaksnavn" value={tilsagn.gjennomforing.navn} />
        </VStack>
      )}
      {tilsagn.beregning.entries.map((entry) => (
        <MetadataHorisontal
          compact
          header={entry.label}
          value={entry.value ? getDataElement(entry.value) : null}
        />
      ))}
    </VStack>
  );
}
