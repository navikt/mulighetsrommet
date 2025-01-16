import { ArrangorKontaktperson } from "@mr/api-client";
import { BodyShort } from "@navikt/ds-react";
import { Metadata } from "../../components/detaljside/Metadata";

interface Props {
  kontaktperson: ArrangorKontaktperson;
}

export function ArrangorKontaktpersonDetaljer({ kontaktperson }: Props) {
  const { navn, telefon, epost, beskrivelse } = kontaktperson;
  return (
    <div className="my-5">
      <BodyShort className="font-bold mb-2">{navn}</BodyShort>
      <dl className="flex">
        <Metadata header="Epost" verdi={<a href={`mailto:${epost}`}>{epost}</a>} />
        <Metadata header="Telefon" verdi={telefon} />
        {beskrivelse && <Metadata header="Beskrivelse" verdi={beskrivelse} />}
      </dl>
    </div>
  );
}
