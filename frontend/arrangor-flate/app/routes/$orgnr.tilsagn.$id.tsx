import { ArrangorflateService, ArrangorflateTilsagn } from "api-client";
import { LoaderFunction, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { TilsagnStatusTag } from "~/components/tilsagn/TilsagnStatusTag";
import { PageHeader } from "../components/PageHeader";
import { tekster } from "../tekster";
import { VStack } from "@navikt/ds-react";
import css from "../root.module.css";
import { useOrgnrFromUrl, pathByOrgnr } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";

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
    <VStack gap="4" className={css.side}>
      <PageHeader
        title={tekster.bokmal.tilsagn.detaljer.headingTitle}
        tilbakeLenke={{
          navn: tekster.bokmal.tilsagn.detaljer.tilbakeLenke,
          url: pathByOrgnr(orgnr).utbetalinger,
        }}
      />
      <TilsagnDetaljer
        tilsagn={tilsagn}
        ekstraDefinisjoner={[
          { key: "Status", value: <TilsagnStatusTag data={tilsagn.status} /> },
          { key: "Tiltakstype", value: tilsagn.tiltakstype.navn },
          { key: "Tiltaksnavn", value: tilsagn.gjennomforing.navn },
          { key: "Tilsagnsnummer", value: tilsagn.bestillingsnummer },
        ]}
      />
    </VStack>
  );
}
