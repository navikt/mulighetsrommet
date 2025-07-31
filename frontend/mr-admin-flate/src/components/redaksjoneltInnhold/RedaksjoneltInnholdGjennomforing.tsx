import { GjennomforingPageLayout } from "@/pages/gjennomforing/GjennomforingPageLayout";
import { RedaksjoneltInnholdPreview } from "./RedaksjoneltInnholdPreview";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useRequiredParams } from "@/hooks/useRequiredParams";

export function RedaksjoneltInnholdGjennomforing() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  return (
    <GjennomforingPageLayout>
      <RedaksjoneltInnholdPreview
        tiltakstype={gjennomforing.tiltakstype}
        kontorstruktur={gjennomforing.kontorstruktur}
      />
    </GjennomforingPageLayout>
  );
}
