import { Box } from "@navikt/ds-react";
import { MetadataHorisontal } from "../../components/detaljside/Metadata";
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
    <Box marginBlock="5">
      <dl className="flex flex-col gap-2">
        <MetadataHorisontal header="Navn" value={navn} compact />
        {beskrivelse && <MetadataHorisontal header="Beskrivelse" value={beskrivelse} compact />}
        <MetadataHorisontal
          header="Epost"
          value={<a href={`mailto:${epost}`}>{epost}</a>}
          compact
        />
        <MetadataHorisontal header="Telefon" value={telefon} compact />
      </dl>
    </Box>
  );
}
