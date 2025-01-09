import { ArrangorflateService, ArrangorflateTilsagn } from "@mr/api-client-v2";
import { LoaderFunction } from "react-router";
import { useLoaderData } from "react-router";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { PageHeader } from "../components/PageHeader";
import { internalNavigation } from "../internal-navigation";
import { useOrgnrFromUrl } from "../utils";
import { apiHeaders } from "~/auth/auth.server";

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
    throw error;
  }

  return { tilsagn };
};

export default function TilsagnDetaljerPage() {
  const { tilsagn } = useLoaderData<LoaderData>();
  const orgnr = useOrgnrFromUrl();

  return (
    <div className="max-w-[50%]">
      <PageHeader
        title="Detaljer for tilsagn"
        tilbakeLenke={{
          navn: "Tilbake til tilsagnsoversikt",
          url: internalNavigation(orgnr).root,
        }}
      />
      <Definisjonsliste
        className="mt-4"
        definitions={[
          { key: "Tiltakstype", value: tilsagn.tiltakstype.navn },
          { key: "Tiltaksnavn", value: tilsagn.gjennomforing.navn },
        ]}
      />
      <TilsagnDetaljer tilsagn={tilsagn} />
    </div>
  );
}
