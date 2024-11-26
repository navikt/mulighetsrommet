import { VeilederflateTiltak } from "@mr/api-client";
import { isTiltakAktivt } from "@/api/queries/useArbeidsmarkedstiltakById";
import { GjennomforingStatusTag } from "@mr/frontend-common";
import { BodyLong, Heading, HStack, VStack } from "@navikt/ds-react";
import styles from "./TiltakHeader.module.scss";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function TiltakHeader({ tiltak }: Props) {
  const { beskrivelse, tiltakstype } = tiltak;
  return (
    <>
      <HStack align="center" gap="2" className={styles.tiltaksgjennomforing_title}>
        <Heading level="1" size="xlarge">
          <VStack>
            {tiltak.tittel}
            <BodyLong size="medium" textColor="subtle">
              {tiltak.underTittel}
            </BodyLong>
          </VStack>
        </Heading>
        {!isTiltakAktivt(tiltak) && <GjennomforingStatusTag status={tiltak.status} />}
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
