import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { SANITY_STUDIO_URL } from "@/constants";
import { formaterDato } from "@/utils/Utils";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Link, useLoaderData } from "react-router";
import { tiltakstypeLoader } from "./tiltakstyperLoaders";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function TiltakstypeInfo() {
  const tiltakstype = useLoaderData<typeof tiltakstypeLoader>();
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
          verdi={tiltakstype.sluttDato ? formaterDato(tiltakstype.sluttDato) : "-"}
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
                  <Link target="_blank" to={tiltakstypeSanityUrl}>
                    Åpne tiltakstypen i Sanity{" "}
                    <ExternalLinkIcon title="Åpner tiltakstypen i Sanity" />
                  </Link>
                </>
              }
            />
          </Bolk>
        </>
      )}
    </WhitePaddedBox>
  );
}
