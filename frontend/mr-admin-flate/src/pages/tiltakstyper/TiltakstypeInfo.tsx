import { Bolk } from "@/components/detaljside/Bolk";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { sanityStudioUrl } from "@/constants";

export function TiltakstypeInfo() {
  const { data: tiltakstype } = useTiltakstypeById();

  const tiltakstypeSanityUrl = `${sanityStudioUrl()}/structure/tiltakstype;${tiltakstype.sanityId}`;

  return (
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
  );
}
