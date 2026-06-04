import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { TiltakstypeHandlinger } from "@/pages/tiltakstyper/TiltakstypeHandlinger";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { BodyLong, Heading, HStack, Label, Link, VStack } from "@navikt/ds-react";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import {
  RedaksjoneltInnholdLenke,
  TiltakstypeDto,
  TiltakstypeKombinasjon,
} from "@tiltaksadministrasjon/api-client";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { RedaksjoneltInnhold } from "@/components/redaksjoneltInnhold/RedaksjoneltInnhold";

export function TiltakstypeInformasjonForVeilederePage() {
  const { data: tiltakstype } = useTiltakstypeById();

  return (
    <VStack>
      <TiltakstypeHandlinger />
      <Separator />
      <TiltakstypeInformasjonForVeiledere tiltakstype={tiltakstype} />
    </VStack>
  );
}

function TiltakstypeInformasjonForVeiledere({ tiltakstype }: { tiltakstype: TiltakstypeDto }) {
  const { faglenker, kanKombineresMed } = tiltakstype.veilederinfo;

  return (
    <TwoColumnGrid separator>
      <RedaksjoneltInnhold tiltakstype={tiltakstype} beskrivelse={null} faneinnhold={null} />
      <RedaksjoneltInnholdContainer>
        <TiltakstypeKanKombineresMed kanKombineresMed={kanKombineresMed} />
        <TiltakstypeFaglenker faglenker={faglenker} />
      </RedaksjoneltInnholdContainer>
    </TwoColumnGrid>
  );
}

function TiltakstypeKanKombineresMed({
  kanKombineresMed,
}: {
  kanKombineresMed: TiltakstypeKombinasjon[];
}) {
  return (
    <>
      <Heading size="medium" level="3">
        Kan kombineres med
      </Heading>
      {kanKombineresMed.length > 0 ? (
        <VStack>
          {kanKombineresMed.map(({ id, navn }) => (
            <BodyLong key={id}>{navn}</BodyLong>
          ))}
        </VStack>
      ) : (
        <BodyLong size="small" as="span">
          Ikke registrert
        </BodyLong>
      )}
    </>
  );
}

function TiltakstypeFaglenker({ faglenker }: { faglenker: RedaksjoneltInnholdLenke[] }) {
  return (
    <>
      <Heading size="medium" level="3">
        Regelverk og rutiner
      </Heading>
      {faglenker.length > 0 ? (
        <VStack>
          {faglenker.map((lenke) => (
            <HStack key={lenke.id} gap="space-4" align="center">
              <Link href={lenke.url} target="_blank">
                {lenke.navn ?? lenke.url}
              </Link>
              {lenke.beskrivelse && (
                <Label size="small" as="span">
                  {lenke.beskrivelse}
                </Label>
              )}
            </HStack>
          ))}
        </VStack>
      ) : (
        <BodyLong size="small" as="span">
          Ingen lenker registrert
        </BodyLong>
      )}
    </>
  );
}
