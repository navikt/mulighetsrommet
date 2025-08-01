import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { SANITY_STUDIO_URL } from "@/constants";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { formaterDato } from "@mr/frontend-common/utils/date";

export function TiltakstypeInfo() {
  const { data: tiltakstype } = useTiltakstypeById();

  const tiltakstypeSanityUrl = `${SANITY_STUDIO_URL}/structure/tiltakstype;${tiltakstype.sanityId}`;

  return (
    <WhitePaddedBox>
      <Bolk>
        <Metadata header="Tiltakstype" verdi={tiltakstype.navn} />
        <Metadata header="Tiltakskode" verdi={tiltakstype.arenaKode} />
      </Bolk>
      <Separator />
      <Bolk>
        <Metadata header="Startdato" verdi={formaterDato(tiltakstype.startDato)} />
        <Metadata
          header="Sluttdato"
          verdi={tiltakstype?.sluttDato ? formaterDato(tiltakstype.sluttDato) : "-"}
        />
      </Bolk>
      {tiltakstype.sanityId && (
        <>
          <Separator />
          <Bolk aria-label="Sanity-dokument">
            <Metadata
              header="Sanity-dokument"
              verdi={
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
