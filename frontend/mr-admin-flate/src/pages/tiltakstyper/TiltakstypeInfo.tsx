import { Bolk } from "@/components/detaljside/Bolk";
import { SANITY_STUDIO_URL } from "@/constants";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { Metadata, Separator } from "@mr/frontend-common/components/datadriven/Metadata";

export function TiltakstypeInfo() {
  const { data: tiltakstype } = useTiltakstypeById();

  const tiltakstypeSanityUrl = `${SANITY_STUDIO_URL}/structure/tiltakstype;${tiltakstype.sanityId}`;

  return (
    <WhitePaddedBox>
      <Bolk>
        <Metadata label="Tiltakstype" value={tiltakstype.navn} />
        <Metadata label="Tiltakskode" value={tiltakstype.arenaKode} />
      </Bolk>
      <Separator />
      <Bolk>
        <Metadata label="Startdato" value={formaterDato(tiltakstype.startDato)} />
        <Metadata
          label="Sluttdato"
          value={tiltakstype.sluttDato ? formaterDato(tiltakstype.sluttDato) : "-"}
        />
      </Bolk>
      {tiltakstype.sanityId && (
        <>
          <Separator />
          <Bolk aria-label="Sanity-dokument">
            <Metadata
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
