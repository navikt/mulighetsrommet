import { VeilederflateTiltak } from "@mr/api-client";
import { isTiltakAktivt } from "@/api/queries/useArbeidsmarkedstiltakById";
import { GjennomforingStatusTag } from "@mr/frontend-common";
import { BodyLong, BodyShort, Heading, HStack, VStack } from "@navikt/ds-react";

interface Props {
  tiltak: VeilederflateTiltak;
}

export function TiltakHeader({ tiltak }: Props) {
  const { beskrivelse, tiltakstype } = tiltak;
  return (
    <>
      <VStack gap="2">
        <Heading level="2" size="xlarge">
          <BodyShort spacing size="small">
            {tiltak.tiltakstype.navn}
          </BodyShort>
        </Heading>
        <HStack gap={"2"} align="center">
          <Heading size="large">{tiltak.navn}</Heading>
          {!isTiltakAktivt(tiltak) && <GjennomforingStatusTag status={tiltak.status} />}
        </HStack>
      </VStack>
      {beskrivelse && (
        <BodyLong size="large" spacing style={{ whiteSpace: "pre-wrap", marginTop: "1rem" }}>
          {beskrivelse}
        </BodyLong>
      )}
      {tiltakstype.beskrivelse && (
        <VStack gap={"0"} style={{ marginTop: "1rem" }}>
          <Heading level="2" size="small">
            Generell informasjon
          </Heading>
          <BodyLong size="large" style={{ whiteSpace: "pre-wrap" }}>
            {tiltakstype.beskrivelse}
          </BodyLong>
        </VStack>
      )}
    </>
  );
}
