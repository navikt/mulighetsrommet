import { LoaderFunction } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
import { PageHeader } from "../components/PageHeader";
import { checkValidToken } from "../auth/auth.server";
import { ArrangorflateService, ArrangorflateTilsagn } from "@mr/api-client";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";

type LoaderData = {
  tilsagn: ArrangorflateTilsagn;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await checkValidToken(request);

  if (params.id === undefined) throw Error("Mangler id");
  const tilsagn = await ArrangorflateService.getArrangorflateTilsagn({ id: params.id });

  return { tilsagn };
};

export default function TilsagnDetaljerPage() {
  const { tilsagn } = useLoaderData<LoaderData>();

  return (
    <>
      <PageHeader
        title="Detaljer for tilsagn"
        tilbakeLenke={{
          navn: "Tilbake til tilsagnsliste",
          url: `/`,
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
    </>
  );
}
