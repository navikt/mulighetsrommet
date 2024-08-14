import { BodyLong, Heading, HStack } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "@mr/api-client";
import styles from "./TiltaksgjennomforingsHeader.module.scss";
import { TiltaksgjennomforingStatusTag } from "@mr/frontend-common";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";

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
        <BodyLong size="large" className={styles.beskrivelse} style={{ whiteSpace: "pre-wrap" }}>
          {tiltakstype.beskrivelse}
        </BodyLong>
      )}
      {beskrivelse && (
        <BodyLong style={{ whiteSpace: "pre-wrap" }} textColor="subtle" size="medium">
          {beskrivelse}
        </BodyLong>
      )}
    </>
  );
};

export default TiltaksgjennomforingsHeader;
