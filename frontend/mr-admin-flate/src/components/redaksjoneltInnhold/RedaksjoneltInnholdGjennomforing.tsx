import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";
import { RedaksjoneltInnhold } from "./RedaksjoneltInnholdPreview";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Laster } from "../laster/Laster";
import { Suspense } from "react";

export function RedaksjoneltInnholdGjennomforing() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);

  return (
    <Suspense fallback={<Laster tekst="Laster innhold" />}>
      <GjennomforingPageLayout>
        <RedaksjoneltInnhold
          tiltakstype={gjennomforing.tiltakstype}
          kontorstruktur={gjennomforing.kontorstruktur}
          beskrivelse={gjennomforing.redaksjoneltInnhold.beskrivelse}
          faneinnhold={gjennomforing.redaksjoneltInnhold.faneinnhold}
          kontaktpersoner={gjennomforing.kontaktpersoner}
        />
      </GjennomforingPageLayout>
    </Suspense>
  );
}
