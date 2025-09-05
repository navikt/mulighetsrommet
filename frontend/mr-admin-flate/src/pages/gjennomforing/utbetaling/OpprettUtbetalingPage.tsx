import { Link } from "react-router";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";
import { useKontonummerForArrangor } from "@/api/arrangor/useKontonummerForArrangor";
import { useRequiredParams } from "@/hooks/useRequiredParams";

export function OpprettUtbetalingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  const { data: kontonummer } = useKontonummerForArrangor(
    gjennomforing.arrangor.organisasjonsnummer,
  );

  return (
    <div className="flex flex-col gap-4">
      <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger`}>Tilbake</Link>
      <OpprettUtbetalingForm gjennomforing={gjennomforing} kontonummer={kontonummer.data} />
    </div>
  );
}
