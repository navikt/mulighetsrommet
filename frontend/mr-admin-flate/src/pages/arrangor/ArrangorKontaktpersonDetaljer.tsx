import { ArrangorKontaktperson } from "mulighetsrommet-api-client";
import styles from "../DetaljerInfo.module.scss";
import { BodyShort } from "@navikt/ds-react";

interface Props {
  kontaktperson: ArrangorKontaktperson;
}

export function ArrangorKontaktpersonDetaljer({ kontaktperson }: Props) {
  const { navn, telefon, epost, beskrivelse } = kontaktperson;
  return (
    <div className={styles.arrangor_kontaktinfo}>
      <BodyShort>{navn}</BodyShort>
      <BodyShort>{telefon}</BodyShort>
      <a href={`mailto:${epost}`}>{epost}</a>
      {beskrivelse && <BodyShort>{beskrivelse}</BodyShort>}
    </div>
  );
}
