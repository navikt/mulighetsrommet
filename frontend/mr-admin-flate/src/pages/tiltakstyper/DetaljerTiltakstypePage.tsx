import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { useMatch } from "react-router";
import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Bolk } from "@/components/detaljside/Bolk";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { sanityStudioUrl } from "@/constants";
import { BodyLong, BodyShort, Heading, HStack, Label, Link, Tabs, VStack } from "@navikt/ds-react";
import { TiltakstypeRedaksjoneltInnholdForm } from "@/components/tiltakstype/TiltakstypeRedaksjoneltInnholdForm";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";

export function DetaljerTiltakstypePage() {
  const { data: tiltakstype } = useTiltakstypeById();
  const queryClient = useQueryClient();
  const tiltakstyperSanityOppskriftUrl = `${sanityStudioUrl()}/structure/tiltakstype;${tiltakstype.sanityId}`;

  const matchAvtaler = useMatch("/tiltakstyper/:tiltakstypeId/avtaler");
  const brodsmuler: (Brodsmule | undefined)[] = [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstype", lenke: matchAvtaler ? `/tiltakstyper/${tiltakstype.id}` : undefined },
    matchAvtaler ? { tittel: "Avtaler" } : undefined,
  ];

  function handleFormSuccess() {
    queryClient.invalidateQueries({ queryKey: QueryKeys.tiltakstyper() });
    queryClient.invalidateQueries({ queryKey: QueryKeys.tiltakstype(tiltakstype.id) });
  }

  return (
    <>
      <title>{`Tiltakstype | ${tiltakstype.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner heading={tiltakstype.navn} ikon={<TiltakstypeIkon />} />
      <WhitePaddedBox>
        <Bolk>
          <MetadataVStack label="Tiltakstype" value={tiltakstype.navn} />
          <MetadataVStack label="Tiltakskode" value={tiltakstype.tiltakskode} />
        </Bolk>
        <Separator />
        <Bolk>
          <MetadataVStack label="Startdato" value={formaterDato(tiltakstype.startDato)} />
          <MetadataVStack
            label="Sluttdato"
            value={tiltakstype.sluttDato ? formaterDato(tiltakstype.sluttDato) : "-"}
          />
        </Bolk>
        {tiltakstype.sanityId && (
          <>
            <Separator />
            <Bolk aria-label="Oppskrifter">
              <MetadataVStack
                label="Oppskrifter"
                value={
                  <Lenke isExternal target="_blank" to={tiltakstyperSanityOppskriftUrl}>
                    Rediger oppskrifter i Sanity
                  </Lenke>
                }
              />
            </Bolk>
          </>
        )}
        <Separator />
        <Tabs defaultValue="redaksjonelt-innhold">
          <Tabs.List>
            <Tabs.Tab value="redaksjonelt-innhold" label="Redaksjonelt innhold" />
            <Tabs.Tab value="rediger" label="Rediger" />
          </Tabs.List>
          <Tabs.Panel value="redaksjonelt-innhold">
            <VStack gap="space-12" className="mt-4 max-w-[900px]">
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
          </Tabs.Panel>
          <Tabs.Panel value="rediger">
            <div className="mt-4">
              <TiltakstypeRedaksjoneltInnholdForm
                tiltakstype={tiltakstype}
                onSuccess={handleFormSuccess}
              />
            </div>
          </Tabs.Panel>
        </Tabs>
      </WhitePaddedBox>
    </>
  );
}
