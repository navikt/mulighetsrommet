import { MetadataHGrid } from "@mr/frontend-common/components/datadriven/Metadata";
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
        <MetadataHGrid label="Navn" value={navn} compact />
        {beskrivelse && <MetadataHGrid label="Beskrivelse" value={beskrivelse} compact />}
        <MetadataHGrid label="Epost" value={<a href={`mailto:${epost}`}>{epost}</a>} compact />
        <MetadataHGrid label="Telefon" value={telefon} compact />
      </dl>
    </Box>
  );
}
