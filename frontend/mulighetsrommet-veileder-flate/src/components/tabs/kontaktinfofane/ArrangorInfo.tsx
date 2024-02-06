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
            <dl className={styles.definisjonsliste}>
              <dt>Epost:</dt>
              <dd>
                <a href={`mailto:${person.epost}`}>{person.epost}</a>
              </dd>
              {person.telefon ? (
                <>
                  <dt>Telefon:</dt>
                  <dd>
                    <span>{person.telefon}</span>
                  </dd>
                </>
              ) : null}
            </dl>
          </BodyShort>
        </div>
      ))}
    </div>
  );
};
export default ArrangorInfo;
