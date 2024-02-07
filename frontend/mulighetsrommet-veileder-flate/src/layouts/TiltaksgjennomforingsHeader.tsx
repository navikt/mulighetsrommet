import { BodyLong, Heading } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./TiltaksgjennomforingsHeader.module.scss";
import classnames from "classnames";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

const TiltaksgjennomforingsHeader = ({ tiltaksgjennomforing }: Props) => {
  const { navn, beskrivelse, tiltakstype } = tiltaksgjennomforing;
  return (
    <>
      <Heading level="1" size="xlarge" className={styles.tiltaksgjennomforing_title}>
        {navn}
      </Heading>
      {tiltakstype.beskrivelse && (
        <BodyLong size="large" className={classnames(styles.beskrivelse, styles.preWrap)}>
          {tiltakstype.beskrivelse}
        </BodyLong>
      )}
      {beskrivelse && (
        <BodyLong className={styles.preWrap} textColor="subtle" size="medium">
          {beskrivelse}
        </BodyLong>
      )}
    </>
  );
};

export default TiltaksgjennomforingsHeader;
