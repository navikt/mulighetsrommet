import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";
import { RedaksjoneltInnhold } from "./RedaksjoneltInnholdPreview";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Laster } from "../laster/Laster";
import { Suspense } from "react";

export function RedaksjoneltInnholdGjennomforing() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { tiltakstype, veilederinfo } = useGjennomforing(gjennomforingId);

  if (!veilederinfo) {
    return null;
  }

  return (
    <Suspense fallback={<Laster tekst="Laster innhold" />}>
      <GjennomforingPageLayout>
        <RedaksjoneltInnhold
          tiltakstype={tiltakstype}
          kontorstruktur={veilederinfo.kontorstruktur}
          beskrivelse={veilederinfo.beskrivelse ?? null}
          faneinnhold={veilederinfo.faneinnhold}
          kontaktpersoner={veilederinfo.kontaktpersoner}
        />
      </GjennomforingPageLayout>
    </Suspense>
  );
}
