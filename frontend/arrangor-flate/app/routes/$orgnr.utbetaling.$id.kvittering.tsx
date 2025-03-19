import { ArrangorflateService, ArrangorflateTilsagn, ArrFlateUtbetaling } from "api-client";
import { formaterKontoNummer } from "@mr/frontend-common/utils/utils";
import { FilePdfIcon } from "@navikt/aksel-icons";
import { Button, VStack } from "@navikt/ds-react";
import { LoaderFunction } from "react-router";
import { useLoaderData, useParams } from "react-router";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { PageHeader } from "~/components/PageHeader";
import { UtbetalingDetaljer } from "~/components/utbetaling/UtbetalingDetaljer";
import { Separator } from "~/components/Separator";
import { internalNavigation } from "../internal-navigation";
import { problemDetailResponse, useOrgnrFromUrl } from "../utils";
import { LinkWithTabState } from "../components/LinkWithTabState";
import { apiHeaders } from "~/auth/auth.server";

type UtbetalingKvitteringData = {
  utbetaling: ArrFlateUtbetaling;
  tilsagn: ArrangorflateTilsagn[];
};

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<UtbetalingKvitteringData> => {
  const { id } = params;
  if (!id) {
    throw new Response("Mangler id", { status: 400 });
  }

  const [{ data: utbetaling, error: utbetalingError }, { data: tilsagn, error: tilsagnError }] =
    await Promise.all([
      ArrangorflateService.getArrFlateUtbetaling({
        path: { id },
        headers: await apiHeaders(request),
      }),
      ArrangorflateService.getArrangorflateTilsagnTilUtbetaling({
        path: { id },
        headers: await apiHeaders(request),
      }),
    ]);

  if (utbetalingError || !utbetaling) {
    throw problemDetailResponse(utbetalingError);
  }
  if (tilsagnError || !tilsagn) {
    throw problemDetailResponse(tilsagnError);
  }

  return { utbetaling, tilsagn };
};

export default function UtbetalingKvittering() {
  const { utbetaling, tilsagn } = useLoaderData<UtbetalingKvitteringData>();
  const { id } = useParams();
  const orgnr = useOrgnrFromUrl();

  return (
    <>
      <PageHeader
        title="Kvittering"
        tilbakeLenke={{
          navn: "Tilbake til utbetalinger",
          url: internalNavigation(orgnr).utbetalinger,
        }}
      />
      <Separator />
      <div className="flex justify-end">
        <a href={`/${orgnr}/utbetaling/${id}/kvittering/lastned`} target="_blank">
          <Button variant="tertiary-neutral" size="small">
            <span className="flex gap-2 items-center">
              Last ned som PDF <FilePdfIcon fontSize={35} />
            </span>
          </Button>
        </a>
      </div>
      <Separator />
      <VStack gap="5" className="max-w-[50%] mt-5">
        <UtbetalingDetaljer utbetaling={utbetaling} tilsagn={tilsagn} />
        <Separator />
        <Definisjonsliste
          title="Betalingsinformasjon"
          definitions={[
            {
              key: "Kontonummer",
              value: formaterKontoNummer(utbetaling.betalingsinformasjon.kontonummer),
            },
            {
              key: "KID-nummer",
              value: utbetaling.betalingsinformasjon.kid!,
            },
          ]}
        />
        <VStack align={"start"}>
          <Button
            as={LinkWithTabState}
            to={internalNavigation(orgnr).utbetalinger}
            variant="secondary"
          >
            Tilbake til utbetalinger
          </Button>
        </VStack>
      </VStack>
    </>
  );
}
