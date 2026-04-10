import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { TiltakstypeHandlinger } from "@/pages/tiltakstyper/TiltakstypeHandlinger";
import { TiltakstypeFaneinnholdContent } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { VStack } from "@navikt/ds-react";

export function TiltakstypeRedaksjoneltInnholdDetaljer() {
  const { data: tiltakstype } = useTiltakstypeById();

  return (
    <VStack>
      <TiltakstypeHandlinger />
      <Separator />
      <TiltakstypeFaneinnholdContent
        beskrivelse={tiltakstype.beskrivelse ?? null}
        faneinnhold={tiltakstype.faneinnhold ?? null}
        regelverklenker={tiltakstype.regelverklenker ?? []}
        kanKombineresMed={tiltakstype.kanKombineresMed}
      />
    </VStack>
  );
}
