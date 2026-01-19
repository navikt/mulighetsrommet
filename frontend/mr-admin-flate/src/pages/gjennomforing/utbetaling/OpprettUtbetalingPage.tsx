import { Link } from "react-router";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useArrangorBankKonto } from "@/api/arrangor/useArrangorBankKonto";

export function OpprettUtbetalingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useGjennomforing(gjennomforingId);
  const { data: bankKonto } = useArrangorBankKonto(gjennomforing.arrangor.id);

  return (
    <div className="flex flex-col gap-4">
      <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger`}>Tilbake</Link>
      <OpprettUtbetalingForm gjennomforing={gjennomforing} bankKonto={bankKonto} />
    </div>
  );
}
