import { BodyShort, Heading } from "@navikt/ds-react";
import { VeilderflateArrangor, VirksomhetKontaktperson } from "mulighetsrommet-api-client";
import styles from "./Kontaktinfo.module.scss";

interface ArrangorInfoProps {
  arrangor?: VeilderflateArrangor;
}

const ArrangorInfo = ({ arrangor }: ArrangorInfoProps) => {
  if (!arrangor) {
    return null;
  }

  const { kontaktpersoner } = arrangor;

  return (
    <div className={styles.arrangor_info}>
      <Heading size="small" className={styles.header}>
        Arrangør
      </Heading>

      <div className={styles.container}>
        <BodyShort className={styles.navn} size="small">
          {arrangor.selskapsnavn}
        </BodyShort>
      </div>

      {kontaktpersoner.map((person: VirksomhetKontaktperson) => (
        <div key={person.id} className={styles.container}>
          <BodyShort className={styles.navn} size="small">
            {person.navn}
          </BodyShort>
          {person.beskrivelse && (
            <BodyShort textColor="subtle" size="small">
              {person.beskrivelse}
            </BodyShort>
          )}

          <BodyShort as="div" size="small">
            <div className={styles.infofelt}>
              <div className={styles.kolonne}>
                {person.telefon && <span>Telefon:</span>}
                <span>Epost:</span>
              </div>

              <div className={styles.kolonne}>
                {person.telefon && <span>{person.telefon}</span>}
                <a href={`mailto:${person.epost}`}>{person.epost}</a>
              </div>
            </div>
          </BodyShort>
        </div>
      ))}
    </div>
  );
};
export default ArrangorInfo;
