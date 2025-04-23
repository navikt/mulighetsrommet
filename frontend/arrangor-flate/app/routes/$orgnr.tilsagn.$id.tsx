import { ArrangorflateService, ArrangorflateTilsagn } from "api-client";
import { LoaderFunction, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { TilsagnStatusTag } from "~/components/tilsagn/TilsagnStatusTag";
import { PageHeader } from "../components/PageHeader";
import { internalNavigation } from "../internal-navigation";
import { tekster } from "../tekster";
import { problemDetailResponse, useOrgnrFromUrl } from "../utils";

type LoaderData = {
  tilsagn: ArrangorflateTilsagn;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { id } = params;
  if (!id) throw Error("Mangler id");

  const { data: tilsagn, error } = await ArrangorflateService.getArrangorflateTilsagn({
    path: { id },
    headers: await apiHeaders(request),
  });

  if (error || !tilsagn) {
    throw problemDetailResponse(error);
  }
  return { tilsagn };
};

export default function TilsagnDetaljerPage() {
  const { tilsagn } = useLoaderData<LoaderData>();
  const orgnr = useOrgnrFromUrl();

  return (
    <div className="flex flex-col items-start">
      <PageHeader
        title="Detaljer for tilsagn"
        tilbakeLenke={{
          navn: "Tilbake til tilsagnsoversikt",
          url: internalNavigation(orgnr).utbetalinger,
        }}
      />
      <TilsagnStatusTag data={tilsagn.status} />
      <TilsagnDetaljer
        tilsagn={tilsagn}
        ekstraDefinisjoner={[
          { key: "Tiltakstype", value: tilsagn.tiltakstype.navn },
          { key: "Tiltaksnavn", value: tilsagn.gjennomforing.navn },
          { key: "Tilsagnsnummer", value: tilsagn.bestillingsnummer },
          { key: "Tilsagnstype", value: tekster.tilsagn.tilsagntype(tilsagn.type) },
        ]}
      />
    </div>
  );
}
