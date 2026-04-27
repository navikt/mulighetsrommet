import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { TiltakstypeHandlinger } from "@/pages/tiltakstyper/TiltakstypeHandlinger";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { BodyLong, BodyShort, Heading, List, VStack } from "@navikt/ds-react";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";

export function TiltakstypeDeltakerRegistreringPage() {
  const { data: tiltakstype } = useTiltakstypeById();

  return (
    <VStack>
      <TiltakstypeHandlinger />
      <Separator />
      <TiltakstypeDeltakerRegistrering tiltakstype={tiltakstype} />
    </VStack>
  );
}

function TiltakstypeDeltakerRegistrering({ tiltakstype }: { tiltakstype: TiltakstypeDto }) {
  const innhold = tiltakstype.deltakerinfo;

  if (!innhold) {
    return <BodyShort size="small">Ikke registrert</BodyShort>;
  }

  return (
    <TwoColumnGrid separator>
      <VStack gap="space-4">
        <Heading size="medium" level="3">
          Ledetekst
        </Heading>
        <BodyLong>{innhold.ledetekst}</BodyLong>
      </VStack>
      <VStack gap="space-4">
        <Heading size="medium" level="3">
          Innholdselementer
        </Heading>
        {innhold.innholdselementer.length > 0 ? (
          <List as="ul">
            {innhold.innholdselementer.map((e) => (
              <List.Item key={e.innholdskode}>{e.tekst}</List.Item>
            ))}
          </List>
        ) : (
          <BodyShort>Ingen innholdselementer valgt</BodyShort>
        )}
      </VStack>
    </TwoColumnGrid>
  );
}
