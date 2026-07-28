import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Laster } from "@/components/laster/Laster";
import { Suspense } from "react";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { InformasjonForVeiledere } from "@/components/redaksjoneltInnhold/InformasjonForVeiledere";
import { isGjennomforingAvtaleDetaljer } from "@/api/gjennomforing/utils";

export function GjennomforingRedaksjoneltInnhold() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const detaljer = useGjennomforing(gjennomforingId);
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);

  if (!isGjennomforingAvtaleDetaljer(detaljer)) {
    return null;
  }

  return (
    <Suspense fallback={<Laster tekst="Laster innhold" />}>
      <GjennomforingPageLayout>
        <InformasjonForVeiledere
          tiltakstype={tiltakstype}
          beskrivelse={detaljer.veilederinfo.beskrivelse}
          faneinnhold={detaljer.veilederinfo.faneinnhold}
          kontorstruktur={detaljer.veilederinfo.kontorstruktur}
          kontaktpersoner={detaljer.veilederinfo.kontaktpersoner}
        />
      </GjennomforingPageLayout>
    </Suspense>
  );
}
