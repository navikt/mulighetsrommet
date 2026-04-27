import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { sanityStudioUrl } from "@/constants";
import { TiltakstypeHandlinger } from "@/pages/tiltakstyper/TiltakstypeHandlinger";
import { VStack } from "@navikt/ds-react";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";

export function DetaljerTiltakstypePage() {
  const { data: tiltakstype } = useTiltakstypeById();
  const tiltakstyperSanityOppskriftUrl = `${sanityStudioUrl()}/structure/tiltakstype;${tiltakstype.sanityId}`;

  return (
    <VStack>
      <TiltakstypeHandlinger />
      <Separator />
      <TwoColumnGrid separator>
        <Definisjonsliste
          title="Tiltakstype"
          definitions={[
            { key: "Navn", value: tiltakstype.navn },
            { key: "Tiltakskode", value: tiltakstype.tiltakskode },
            { key: "Tiltaksgruppe", value: tiltakstype.gruppe },
          ]}
        />
        {tiltakstype.sanityId && (
          <MetadataVStack
            label="Oppskrifter"
            value={
              <Lenke isExternal target="_blank" to={tiltakstyperSanityOppskriftUrl}>
                Rediger oppskrifter i Sanity
              </Lenke>
            }
          />
        )}
      </TwoColumnGrid>
    </VStack>
  );
}
