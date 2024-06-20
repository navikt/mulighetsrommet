import { BodyLong, HStack, Heading } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import styles from "./TiltaksgjennomforingsHeader.module.scss";
import classnames from "classnames";
import { TiltaksgjennomforingStatusTag } from "mulighetsrommet-frontend-common";
import { gjennomforingIsAktiv } from "mulighetsrommet-frontend-common/utils/utils";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

const TiltaksgjennomforingsHeader = ({ tiltaksgjennomforing }: Props) => {
  const { navn, beskrivelse, tiltakstype } = tiltaksgjennomforing;
  return (
    <>
      <HStack align="center" gap="2" className={styles.tiltaksgjennomforing_title}>
        <Heading level="1" size="xlarge">
          {navn}
        </Heading>
        {!gjennomforingIsAktiv(tiltaksgjennomforing.status.status) && (
          <TiltaksgjennomforingStatusTag status={tiltaksgjennomforing.status} />
        )}
      </HStack>
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
