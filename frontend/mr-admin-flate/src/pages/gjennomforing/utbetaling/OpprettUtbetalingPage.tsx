import { Link as ReactRouterLink } from "react-router";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useArrangorBetalingsinformasjon } from "@/api/arrangor/useArrangorBetalingsinformasjon";
import { Link } from "@navikt/ds-react";

export function OpprettUtbetalingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, prismodell } = useGjennomforing(gjennomforingId);
  const { data: betalingsinformasjon } = useArrangorBetalingsinformasjon(gjennomforing.arrangor.id);

  return (
    <div className="flex flex-col gap-4">
      <Link as={ReactRouterLink} to={`/gjennomforinger/${gjennomforingId}/utbetalinger`}>
        Tilbake
      </Link>
      <OpprettUtbetalingForm
        gjennomforing={gjennomforing}
        prismodell={prismodell}
        betalingsinformasjon={betalingsinformasjon}
      />
    </div>
  );
}
