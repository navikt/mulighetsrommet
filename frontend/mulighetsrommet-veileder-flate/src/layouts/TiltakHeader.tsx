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
    <VStack gap="space-4">
      <BodyShort spacing size="small">
        {tiltak.tiltakstype.navn}
      </BodyShort>
      <HStack gap="space-16" align="center">
        <Heading size="large" spacing>
          {tiltak.navn}
        </Heading>
        {isTiltakGruppe(tiltak) && !isTiltakAktivt(tiltak) && (
          <StatusTag dataColor="neutral">{tiltak.status.beskrivelse}</StatusTag>
        )}
      </HStack>
      {beskrivelse && (
        <BodyLong size="large" spacing>
          {beskrivelse}
        </BodyLong>
      )}
      {tiltakstype.beskrivelse && (
        <>
          <Heading level="2" size="small">
            Generell informasjon
          </Heading>
          <BodyLong size="large">{tiltakstype.beskrivelse}</BodyLong>
        </>
      )}
    </VStack>
  );
}
