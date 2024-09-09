import { VeilederflateTiltak } from "@mr/api-client";
import { TiltaksgjennomforingStatusTag } from "@mr/frontend-common";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { BodyLong, Heading, HStack, VStack } from "@navikt/ds-react";
import styles from "./TiltakHeader.module.scss";
import { erKurstiltak } from "@/utils/Utils";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function TiltakHeader({ tiltak }: Props) {
  const { navn, beskrivelse, tiltakstype, faneinnhold } = tiltak;
  return (
    <>
      <HStack align="center" gap="2" className={styles.tiltaksgjennomforing_title}>
        <Heading level="1" size="xlarge">
          <VStack>
            {erKurstiltak(tiltakstype.tiltakskode, tiltakstype.arenakode) ? (
              <>
                {faneinnhold?.kurstittel ?? navn}
                <BodyLong size="medium" textColor="subtle">
                  {tiltakstype.navn}
                </BodyLong>
              </>
            ) : (
              <>
                {tiltakstype.navn}
                <BodyLong size="medium" textColor="subtle">
                  {navn}
                </BodyLong>
              </>
            )}
          </VStack>
        </Heading>
        {!gjennomforingIsAktiv(tiltak.status.status) && (
          <TiltaksgjennomforingStatusTag status={tiltak.status} />
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
}
