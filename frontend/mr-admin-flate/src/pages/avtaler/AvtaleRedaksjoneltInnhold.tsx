import { Suspense } from "react";
import { Laster } from "@/components/laster/Laster";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { AvtalePageLayout } from "@/pages/avtaler/AvtalePageLayout";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { InformasjonForVeiledere } from "@/components/redaksjoneltInnhold/InformasjonForVeiledere";

export function AvtaleRedaksjoneltInnhold() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);
  const tiltakstype = useTiltakstype(avtale.tiltakstype.id);

  return (
    <AvtalePageLayout avtale={avtale}>
      <Suspense fallback={<Laster tekst="Laster innhold" />}>
        <InformasjonForVeiledere
          tiltakstype={tiltakstype}
          beskrivelse={avtale.beskrivelse}
          faneinnhold={avtale.faneinnhold}
          kontorstruktur={avtale.kontorstruktur}
          kontaktpersoner={[]}
        />
      </Suspense>
    </AvtalePageLayout>
  );
}
