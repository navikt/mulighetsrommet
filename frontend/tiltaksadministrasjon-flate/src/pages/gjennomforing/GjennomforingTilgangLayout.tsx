import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { ManglerTilgangTilPersonAlert } from "@/components/gjennomforing/ManglerTilgangTilPersonAlert";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Box } from "@navikt/ds-react";
import { Outlet } from "react-router";

export function GjennomforingTilgangLayout() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const detaljer = useGjennomforing(gjennomforingId);

  const avvistGrunn = "deltaker" in detaljer ? (detaljer.deltaker?.avvistGrunn ?? null) : null;

  if (avvistGrunn) {
    return (
      <Box padding="space-16" paddingInline="space-24">
        <ManglerTilgangTilPersonAlert avvistGrunn={avvistGrunn} />
      </Box>
    );
  }

  return <Outlet />;
}
