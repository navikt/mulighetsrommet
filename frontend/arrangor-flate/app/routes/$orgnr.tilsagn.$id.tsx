import { ArrangorflateService, ArrangorflateTilsagn } from "@mr/api-client";
import { LoaderFunction } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { checkValidToken } from "../auth/auth.server";
import { PageHeader } from "../components/PageHeader";
import { internalNavigation } from "../internal-navigation";
import { useOrgnrFromUrl } from "../utils";

type LoaderData = {
  tilsagn: ArrangorflateTilsagn;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await checkValidToken(request);

  const { id } = params;
  if (!id) {
    throw Error("Mangler id");
  }
  const tilsagn = await ArrangorflateService.getArrangorflateTilsagn({ id });

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
