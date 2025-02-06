import { GjennomforingDto } from "@mr/api-client-v2";
import { Link, useLoaderData } from "react-router";
import { UtbetalingInfoFraArrangorForm } from "./UtbetalingInfoFraArrangorForm";

interface LoaderData {
  gjennomforing: GjennomforingDto;
}

export function UtbetalingInfoFraArrangorPage() {
  const { gjennomforing } = useLoaderData<LoaderData>();

  return (
    <>
      <Link to={`/gjennomforinger/${gjennomforing.id}/utbetalinger`}>Tilbake</Link>
      <UtbetalingInfoFraArrangorForm gjennomforing={gjennomforing} />
    </>
  );
}
