import { Link, useParams } from "react-router";
import { useAdminGjennomforingById } from "../../../api/gjennomforing/useAdminGjennomforingById";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";
export function OpprettUtbetalingPage() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);

  return (
    <>
      <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger`}>Tilbake</Link>
      <OpprettUtbetalingForm gjennomforing={gjennomforing} />
    </>
  );
}
