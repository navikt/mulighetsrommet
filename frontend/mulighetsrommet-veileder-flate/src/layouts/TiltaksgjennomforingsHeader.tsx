import { BodyLong, Heading, HStack, VStack } from "@navikt/ds-react";
import { VeilederflateTiltaksgjennomforing } from "mulighetsrommet-api-client";
import { TiltaksgjennomforingStatusTag } from "mulighetsrommet-frontend-common";
import { gjennomforingIsAktiv } from "mulighetsrommet-frontend-common/utils/utils";
import styles from "./TiltaksgjennomforingsHeader.module.scss";
import { lesbareTiltaksnavn } from "../utils/Utils";

interface Props {
  tiltaksgjennomforing: VeilederflateTiltaksgjennomforing;
}

const TiltaksgjennomforingsHeader = ({ tiltaksgjennomforing }: Props) => {
  const { navn, beskrivelse, tiltakstype, arrangor } = tiltaksgjennomforing;
  return (
    <>
      <HStack align="center" gap="2" className={styles.tiltaksgjennomforing_title}>
        <Heading level="1" size="xlarge">
          <VStack>
            {lesbareTiltaksnavn(navn, tiltakstype, arrangor)}
            <BodyLong size="medium" textColor="subtle">
              {navn}
            </BodyLong>
          </VStack>
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
