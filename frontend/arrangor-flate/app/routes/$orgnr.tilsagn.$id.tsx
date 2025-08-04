import { ArrangorflateService, ArrangorflateTilsagn } from "api-client";
import { LoaderFunction, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { tekster } from "../tekster";
import { VStack } from "@navikt/ds-react";
import css from "../root.module.css";
import { useOrgnrFromUrl, pathByOrgnr } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import { PageHeading } from "~/components/common/PageHeading";

type LoaderData = {
  tilsagn: ArrangorflateTilsagn;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { id, orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
  if (!id) throw Error("Mangler id");

  const { data: tilsagn, error } = await ArrangorflateService.getArrangorflateTilsagn({
    path: { id, orgnr },
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
    <VStack gap="4" className={css.side}>
      <PageHeading
        title={tekster.bokmal.tilsagn.detaljer.headingTitle}
        tilbakeLenke={{
          navn: tekster.bokmal.tilsagn.detaljer.tilbakeLenke,
          url: pathByOrgnr(orgnr).utbetalinger,
        }}
      />
      <TilsagnDetaljer tilsagn={tilsagn} />
    </VStack>
  );
}
