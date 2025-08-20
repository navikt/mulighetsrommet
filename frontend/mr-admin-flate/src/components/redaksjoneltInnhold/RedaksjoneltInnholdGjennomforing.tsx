import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";
import { RedaksjoneltInnhold } from "./RedaksjoneltInnholdPreview";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Laster } from "../laster/Laster";

export function RedaksjoneltInnholdGjennomforing() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);

  return (
    <GjennomforingPageLayout>
      return (
      <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
        <RedaksjoneltInnhold
          tiltakstype={gjennomforing.tiltakstype}
          kontorstruktur={gjennomforing.kontorstruktur}
          beskrivelse={gjennomforing.beskrivelse}
          faneinnhold={gjennomforing.faneinnhold}
        />
      </React.Suspense>
      );
    </GjennomforingPageLayout>
  );
}
