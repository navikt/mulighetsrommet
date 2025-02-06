import { GjennomforingDto } from "@mr/api-client-v2";
import { Link, useLoaderData } from "react-router";
import { OpprettUtbetalingForm } from "./OpprettUtbetalingForm";

interface LoaderData {
  gjennomforing: GjennomforingDto;
}

export function OpprettUtbetalingPage() {
  const { gjennomforing } = useLoaderData<LoaderData>();

  return (
    <>
      <Link to={`/gjennomforinger/${gjennomforing.id}/utbetalinger`}>Tilbake</Link>
      <OpprettUtbetalingForm gjennomforing={gjennomforing} />
    </>
  );
}
