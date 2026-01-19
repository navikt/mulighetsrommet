import { Link } from "react-router";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";
import { useArrangorBetalingsinfo } from "@/api/arrangor/useKontonummerForArrangor";
import { useRequiredParams } from "@/hooks/useRequiredParams";

export function OpprettUtbetalingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);
  const { data: betalingsinfo } = useArrangorBetalingsinfo(gjennomforing.arrangor.id);

  return (
    <div className="flex flex-col gap-4">
      <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger`}>Tilbake</Link>
      <OpprettUtbetalingForm gjennomforing={gjennomforing} betalingsinfo={betalingsinfo} />
    </div>
  );
}
