import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";
import { RedaksjoneltInnhold } from "./RedaksjoneltInnholdPreview";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Laster } from "../laster/Laster";
import { Suspense } from "react";

export function RedaksjoneltInnholdGjennomforing() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);

  return (
    <Suspense fallback={<Laster tekst="Laster innhold" />}>
      <GjennomforingPageLayout>
        <RedaksjoneltInnhold
          tiltakstype={gjennomforing.tiltakstype}
          kontorstruktur={gjennomforing.kontorstruktur}
          beskrivelse={gjennomforing.beskrivelse ?? null}
          faneinnhold={gjennomforing.faneinnhold}
          kontaktpersoner={gjennomforing.kontaktpersoner}
        />
      </GjennomforingPageLayout>
    </Suspense>
  );
}
