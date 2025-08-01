import { VeilederflateTiltak } from "@api-client";
import { isTiltakAktivt, isTiltakGruppe } from "@/api/queries/useArbeidsmarkedstiltakById";
import { BodyLong, BodyShort, Heading, HStack, VStack } from "@navikt/ds-react";
import { StatusTag } from "@mr/frontend-common";

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
          {isTiltakGruppe(tiltak) && !isTiltakAktivt(tiltak) && (
            <StatusTag variant={"neutral"}>{tiltak.status.beskrivelse}</StatusTag>
          )}
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
