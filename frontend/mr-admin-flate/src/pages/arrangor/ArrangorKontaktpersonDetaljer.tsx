import { ArrangorKontaktperson } from "@mr/api-client-v2";
import { Box, Label } from "@navikt/ds-react";
import { Metadata } from "../../components/detaljside/Metadata";

interface Props {
  kontaktperson: ArrangorKontaktperson;
}

export function ArrangorKontaktpersonDetaljer({ kontaktperson }: Props) {
  const { navn, telefon, epost, beskrivelse } = kontaktperson;
  return (
    <Box marginBlock="2">
      <Label as="p" spacing>
        {navn}
      </Label>
      <dl className="flex">
        <Metadata header="Epost" value={<a href={`mailto:${epost}`}>{epost}</a>} />
        <Metadata header="Telefon" value={telefon} />
        {beskrivelse && <Metadata header="Beskrivelse" value={beskrivelse} />}
      </dl>
    </Box>
  );
}
