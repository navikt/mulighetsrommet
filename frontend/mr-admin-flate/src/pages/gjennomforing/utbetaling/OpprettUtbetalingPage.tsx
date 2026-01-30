import { Link } from "react-router";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useArrangorBetalingsinformasjon } from "@/api/arrangor/useArrangorBetalingsinformasjon";

export function OpprettUtbetalingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing, prismodell } = useGjennomforing(gjennomforingId);
  const { data: betalingsinformasjon } = useArrangorBetalingsinformasjon(gjennomforing.arrangor.id);

  return (
    <div className="flex flex-col gap-4">
      <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger`}>Tilbake</Link>
      <OpprettUtbetalingForm
        gjennomforing={gjennomforing}
        prismodell={prismodell}
        betalingsinformasjon={betalingsinformasjon}
      />
    </div>
  );
}
