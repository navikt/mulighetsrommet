import { Link } from "react-router";
import { useAdminGjennomforingById } from "../../../api/gjennomforing/useAdminGjennomforingById";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";

export function OpprettUtbetalingPage() {
  const { data: gjennomforing } = useAdminGjennomforingById();

  return (
    <>
      <Link to={`/gjennomforinger/${gjennomforing.id}/utbetalinger`}>Tilbake</Link>
      <OpprettUtbetalingForm gjennomforing={gjennomforing} />
    </>
  );
}
