import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { Bolk } from "@/components/detaljside/Bolk";
import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { sanityStudioUrl } from "@/constants";
import { TiltakstypeHandlinger } from "@/pages/tiltakstyper/TiltakstypeHandlinger";
import { VStack } from "@navikt/ds-react";

export function DetaljerTiltakstypePage() {
  const { data: tiltakstype } = useTiltakstypeById();
  const tiltakstyperSanityOppskriftUrl = `${sanityStudioUrl()}/structure/tiltakstype;${tiltakstype.sanityId}`;

  return (
    <VStack>
      <TiltakstypeHandlinger />
      <Separator />
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
    </VStack>
  );
}
