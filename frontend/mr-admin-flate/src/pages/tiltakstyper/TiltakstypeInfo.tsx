import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Link } from "react-router-dom";
import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata, Separator } from "@/components/detaljside/Metadata";
import { formaterDato } from "@/utils/Utils";
import { SANITY_STUDIO_URL } from "@/constants";
import { DetaljerContainer } from "@/pages/DetaljerContainer";
import { DetaljerInfoContainer } from "@/pages/DetaljerInfoContainer";

export function TiltakstypeInfo() {
  const { data: tiltakstype } = useTiltakstypeById();

  if (!tiltakstype) {
    return null;
  }

  const tiltakstypeSanityUrl = `${SANITY_STUDIO_URL}/structure/tiltakstype;${tiltakstype.sanityId}`;

  return (
    <DetaljerContainer>
      <DetaljerInfoContainer withBorderRight={false}>
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
      </DetaljerInfoContainer>
    </DetaljerContainer>
  );
}
