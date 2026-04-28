import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Laster } from "@/components/laster/Laster";
import { Suspense } from "react";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { InformasjonForVeiledere } from "@/components/redaksjoneltInnhold/InformasjonForVeiledere";

export function GjennomforingRedaksjoneltInnhold() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { veilederinfo, ...gjennomforing } = useGjennomforing(gjennomforingId);
  const tiltakstype = useTiltakstype(gjennomforing.tiltakstype.id);

  if (!veilederinfo) {
    return null;
  }

  return (
    <Suspense fallback={<Laster tekst="Laster innhold" />}>
      <GjennomforingPageLayout>
        <InformasjonForVeiledere
          tiltakstype={tiltakstype}
          beskrivelse={veilederinfo.beskrivelse}
          faneinnhold={veilederinfo.faneinnhold}
          kontorstruktur={veilederinfo.kontorstruktur}
          kontaktpersoner={veilederinfo.kontaktpersoner}
        />
      </GjennomforingPageLayout>
    </Suspense>
  );
}
