import { PortableTextTypedObject, VeilderflateArrangor } from "@mr/api-client";
import { BodyLong, BodyShort, Heading } from "@navikt/ds-react";
import { RedaksjoneltInnhold } from "../../RedaksjoneltInnhold";
import styles from "./KontaktinfoFane.module.scss";

interface ArrangorInfoProps {
  arrangor: VeilderflateArrangor;
  faneinnhold?: Array<PortableTextTypedObject>;
}

const ArrangorInfo = ({ arrangor, faneinnhold }: ArrangorInfoProps) => {
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

      {kontaktpersoner.map((person) => (
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
      {faneinnhold && (
        <BodyLong as="div" textColor="subtle" size="small">
          <RedaksjoneltInnhold value={faneinnhold} />
        </BodyLong>
      )}
    </div>
  );
};
export default ArrangorInfo;
