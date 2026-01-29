import { ArrangorflateService, ArrangorflateTilsagnDto } from "api-client";
import { LoaderFunction, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { tekster } from "~/tekster";
import { Box, VStack } from "@navikt/ds-react";
import { pathTo } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import { PageHeading } from "~/components/common/PageHeading";

type LoaderData = {
  tilsagn: ArrangorflateTilsagnDto;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { id } = params;

  if (!id) throw Error("Mangler id");

  const { data: tilsagn, error } = await ArrangorflateService.getArrangorflateTilsagn({
    path: { id },
    headers: await apiHeaders(request),
  });

  if (error) {
    throw problemDetailResponse(error);
  }
  return { tilsagn };
};

export default function TilsagnDetaljerPage() {
  const { tilsagn } = useLoaderData<LoaderData>();

  return (
    <Box background="bg-default" paddingInline="8" paddingBlock="8 16" borderRadius="large">
      <VStack gap="4">
        <PageHeading
          title={tekster.bokmal.tilsagn.detaljer.headingTitle}
          tilbakeLenke={{
            navn: tekster.bokmal.tilsagn.detaljer.tilbakeLenke,
            url: pathTo.utbetalinger,
          }}
        />
        <TilsagnDetaljer tilsagn={tilsagn} />
      </VStack>
    </Box>
  );
}
