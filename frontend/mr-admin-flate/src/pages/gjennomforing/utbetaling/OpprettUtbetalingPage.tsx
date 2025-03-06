import { Link } from "react-router";
import { useAdminGjennomforingById } from "../../../api/gjennomforing/useAdminGjennomforingById";
import { Laster } from "../../../components/laster/Laster";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";

export function OpprettUtbetalingPage() {
  const { data: gjennomforing } = useAdminGjennomforingById();

  if (!gjennomforing) {
    return <Laster tekst="Laster gjennomføring..." />;
  }

  return (
    <>
      <Link to={`/gjennomforinger/${gjennomforing.id}/utbetalinger`}>Tilbake</Link>
      <OpprettUtbetalingForm gjennomforing={gjennomforing} />
    </>
  );
}
