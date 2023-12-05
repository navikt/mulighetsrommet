import { BodyShort, Heading } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./Kontaktinfo.module.scss";

interface ArrangorInfoProps {
  data: VeilederflateTiltaksgjennomforing;
}

const ArrangorInfo = ({ data }: ArrangorInfoProps) => {
  const { arrangor } = data;

  if (!arrangor) {
    return null;
  }

  const { kontaktperson } = arrangor;

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

      {kontaktperson && (
        <div className={styles.container}>
          <BodyShort className={styles.navn} size="small">
            {kontaktperson.navn}
          </BodyShort>

          <BodyShort as="div" size="small">
            <div className={styles.infofelt}>
              <div className={styles.kolonne}>
                <span>Telefon:</span>
                <span>Epost:</span>
              </div>

              <div className={styles.kolonne}>
                <span>{kontaktperson.telefon}</span>
                <a href={`mailto:${kontaktperson.epost}`}>{kontaktperson.epost}</a>
              </div>
            </div>
          </BodyShort>
        </div>
      )}
    </div>
  );
};
export default ArrangorInfo;
