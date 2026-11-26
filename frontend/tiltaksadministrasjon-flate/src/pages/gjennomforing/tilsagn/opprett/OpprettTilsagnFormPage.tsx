import { TilsagnFormContainer } from "@/components/tilsagn/TilsagnFormContainer";
import { TilsagnDefaultsRequest } from "@tiltaksadministrasjon/api-client";
import { useSearchParams } from "react-router";
import { getTilsagnDefaultsRequest, useOpprettTilsagnData } from "./opprettTilsagnLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Suspense } from "react";
import { Laster } from "@/components/laster/Laster";

export function OpprettTilsagnFormPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const [searchParams] = useSearchParams();
  const request = getTilsagnDefaultsRequest(gjennomforingId, searchParams);

  return (
    <Suspense key={`${gjennomforingId}?${searchParams}`} fallback={<Laster tekst="Laster..." />}>
      <OpprettTilsagnForm request={request} />
    </Suspense>
  );
}

function OpprettTilsagnForm({ request }: { request: TilsagnDefaultsRequest }) {
  const { detaljer, defaults } = useOpprettTilsagnData(request);

  return (
    <TilsagnFormContainer
      gjennomforing={detaljer.gjennomforing}
      prismodell={detaljer.prismodell}
      defaults={defaults}
    />
  );
}
