import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
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

export function DetaljerTiltakstypePage() {
  const { data: tiltakstype } = useTiltakstypeById();
  const tiltakstypeSanityUrl = `${sanityStudioUrl()}/structure/tiltakstype;${tiltakstype.sanityId}`;

  const matchAvtaler = useMatch("/tiltakstyper/:tiltakstypeId/avtaler");
  const brodsmuler: (Brodsmule | undefined)[] = [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstype", lenke: matchAvtaler ? `/tiltakstyper/${tiltakstype.id}` : undefined },
    matchAvtaler ? { tittel: "Avtaler" } : undefined,
  ];

  return (
    <>
      <title>{`Tiltakstype | ${tiltakstype.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner heading={tiltakstype.navn} ikon={<TiltakstypeIkon />} />
      <ContentBox>
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
              <Bolk aria-label="Sanity-dokument">
                <MetadataVStack
                  label="Sanity-dokument"
                  value={
                    <>
                      <Lenke isExternal target="_blank" to={tiltakstypeSanityUrl}>
                        Åpne tiltakstypen i Sanity
                      </Lenke>
                    </>
                  }
                />
              </Bolk>
            </>
          )}
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
