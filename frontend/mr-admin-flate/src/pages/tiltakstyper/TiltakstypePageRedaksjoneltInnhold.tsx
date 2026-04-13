import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { TiltakstypeHandlinger } from "@/pages/tiltakstyper/TiltakstypeHandlinger";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { BodyLong, Heading, HStack, Label, Link, VStack } from "@navikt/ds-react";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { RedaksjoneltInnhold } from "@/components/redaksjoneltInnhold/RedaksjoneltInnhold";

export function TiltakstypePageRedaksjoneltInnhold() {
  const { data: tiltakstype } = useTiltakstypeById();

  return (
    <VStack>
      <TiltakstypeHandlinger />
      <Separator />
      <TiltakstypeRedaksjoneltInnhold tiltakstype={tiltakstype} />
    </VStack>
  );
}

function TiltakstypeRedaksjoneltInnhold({ tiltakstype }: { tiltakstype: TiltakstypeDto }) {
  const { regelverklenker, kanKombineresMed } = tiltakstype;

  return (
    <TwoColumnGrid separator>
      <RedaksjoneltInnhold tiltakstype={tiltakstype} beskrivelse={null} faneinnhold={null} />
      <RedaksjoneltInnholdContainer>
        {kanKombineresMed.length > 0 && (
          <>
            <Heading size="medium">Kan kombineres med</Heading>
            <VStack gap="space-2">
              {kanKombineresMed.map((navn) => (
                <BodyLong key={navn}>{navn}</BodyLong>
              ))}
            </VStack>
          </>
        )}

        {regelverklenker.length > 0 && (
          <>
            <Heading size="medium">Regelverk</Heading>
            <VStack gap="space-2">
              {regelverklenker.map((lenke) => (
                <HStack key={lenke.regelverkUrl} gap="space-4" align="center">
                  <Link href={lenke.regelverkUrl} target="_blank">
                    {lenke.regelverkLenkeNavn ?? lenke.regelverkUrl}
                  </Link>
                  {lenke.beskrivelse && (
                    <Label size="small" as="span">
                      {lenke.beskrivelse}
                    </Label>
                  )}
                </HStack>
              ))}
            </VStack>
          </>
        )}
      </RedaksjoneltInnholdContainer>
    </TwoColumnGrid>
  );
}
