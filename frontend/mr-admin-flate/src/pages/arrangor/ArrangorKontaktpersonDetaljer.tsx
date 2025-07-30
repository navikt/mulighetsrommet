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
        <Metadata header="Epost" verdi={<a href={`mailto:${epost}`}>{epost}</a>} />
        <Metadata header="Telefon" verdi={telefon} />
        {beskrivelse && <Metadata header="Beskrivelse" verdi={beskrivelse} />}
      </dl>
    </Box>
  );
}
