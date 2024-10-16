import { VStack } from "@navikt/ds-react";
import { LoaderFunction } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
import { PageHeader } from "../components/PageHeader";
import { checkValidToken } from "../auth/auth.server";
import { ArrangorflateService, ArrangorflateTilsagn } from "@mr/api-client";

type LoaderData = {
  tilsagn: ArrangorflateTilsagn;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  await checkValidToken(request);

  if (params.id === undefined) throw Error("Mangler id");
  const tilsagn = await ArrangorflateService.getArrangorflateTilsagn({ id: params.id });

  return { tilsagn };
};

export default function TilsagnDetaljer() {
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
      <VStack gap="5">
        <div>{tilsagn.id}</div>
      </VStack>
    </>
  );
}
