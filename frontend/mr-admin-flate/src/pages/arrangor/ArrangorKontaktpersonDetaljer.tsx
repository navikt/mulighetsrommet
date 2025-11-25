import { MetadataHorisontal } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box } from "@navikt/ds-react";
import {
  ArrangorKontaktperson,
  GjennomforingArrangorKontaktperson,
} from "@tiltaksadministrasjon/api-client";

interface Props {
  kontaktperson: ArrangorKontaktperson | GjennomforingArrangorKontaktperson;
}

export function ArrangorKontaktpersonDetaljer({ kontaktperson }: Props) {
  const { navn, telefon, epost, beskrivelse } = kontaktperson;
  return (
    <Box marginBlock="0 5">
      <dl className="flex flex-col gap-2">
        <MetadataHorisontal label="Navn" value={navn} compact />
        {beskrivelse && <MetadataHorisontal label="Beskrivelse" value={beskrivelse} compact />}
        <MetadataHorisontal label="Epost" value={<a href={`mailto:${epost}`}>{epost}</a>} compact />
        <MetadataHorisontal label="Telefon" value={telefon} compact />
      </dl>
    </Box>
  );
}
