import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { TiltakstypeHandlinger } from "@/pages/tiltakstyper/TiltakstypeHandlinger";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { BodyLong, BodyShort, Heading, HStack, Label, Link, VStack } from "@navikt/ds-react";

export function TiltakstypeRedaksjoneltInnholdDetaljer() {
  const { data: tiltakstype } = useTiltakstypeById();

  return (
    <VStack>
      <TiltakstypeHandlinger />
      <Separator />
      <VStack gap="space-12" className="max-w-[900px]">
        {tiltakstype.beskrivelse && (
          <VStack gap="space-4">
            <Heading size="small" level="3">
              Beskrivelse
            </Heading>
            <BodyLong style={{ whiteSpace: "pre-wrap" }}>{tiltakstype.beskrivelse}</BodyLong>
          </VStack>
        )}
        {tiltakstype.kanKombineresMed.length > 0 && (
          <VStack gap="space-4">
            <Heading size="small" level="3">
              Kan kombineres med
            </Heading>
            <VStack gap="space-2">
              {tiltakstype.kanKombineresMed.map((navn) => (
                <BodyShort key={navn}>{navn}</BodyShort>
              ))}
            </VStack>
          </VStack>
        )}
        {tiltakstype.regelverklenker && tiltakstype.regelverklenker.length > 0 && (
          <VStack gap="space-4">
            <Heading size="small" level="3">
              Regelverk
            </Heading>
            <VStack gap="space-2">
              {tiltakstype.regelverklenker.map((lenke) => (
                <HStack key={lenke.regelverkUrl} gap="space-4">
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
          </VStack>
        )}
      </VStack>
    </VStack>
  );
}
