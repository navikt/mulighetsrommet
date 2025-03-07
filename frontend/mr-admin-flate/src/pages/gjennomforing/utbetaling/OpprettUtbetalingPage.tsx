import { Link } from "react-router";
import { useParams } from "react-router";
import { useAdminGjennomforingById } from "../../../api/gjennomforing/useAdminGjennomforingById";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";
import { Laster } from "../../../components/laster/Laster";
export function OpprettUtbetalingPage() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById();

  if (!gjennomforing) {
    return <Laster tekst="Laster gjennomføring..." />;
  }

  return (
    <>
      <Link to={`/gjennomforinger/${gjennomforingId}/utbetalinger`}>Tilbake</Link>
      <OpprettUtbetalingForm gjennomforing={gjennomforing} />
    </>
  );
}
