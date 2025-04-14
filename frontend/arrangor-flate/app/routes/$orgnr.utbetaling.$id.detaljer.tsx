import { FilePdfIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, VStack } from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrangorflateTilsagn,
  ArrFlateUtbetaling,
  ArrFlateUtbetalingStatus,
} from "api-client";
import { LoaderFunction, MetaFunction, useLoaderData, useParams } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { PageHeader } from "~/components/PageHeader";
import { Separator } from "~/components/Separator";
import { internalNavigation } from "../internal-navigation";
import { problemDetailResponse, useOrgnrFromUrl } from "../utils";
import { GenerelleDetaljer } from "~/components/utbetaling/GenerelleDetaljer";
import GenerelleUtbetalingDetaljer from "~/components/utbetaling/GenerelleUtbetalingDetaljer";
import UtbetalingStatusList from "~/components/utbetaling/UtbetalingStatusList";
import InnsendtAvUtbetalingDetaljer from "~/components/utbetaling/InnsendtAvUtbetalingDetaljer";
import BetalingsInformasjon from "~/components/utbetaling/BetalingsInformasjon";
import UtbetalingTilsagnDetaljer from "~/components/utbetaling/UtbetalingTilsagnDetaljer";

type UtbetalingDetaljerSideData = {
  utbetaling: ArrFlateUtbetaling;
  tilsagn: ArrangorflateTilsagn[];
};

export const meta: MetaFunction = () => {
  return [
    { title: "Utbetaling | Detaljer" },
    { name: "description", content: "Arrangørflate for kvittering av krav om utbetaling" },
  ];
};

export const loader: LoaderFunction = async ({
  request,
  params,
}): Promise<UtbetalingDetaljerSideData> => {
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

export default function UtbetalingDetaljerSide() {
  const { utbetaling, tilsagn } = useLoaderData<UtbetalingDetaljerSideData>();
  const { id } = useParams();
  const orgnr = useOrgnrFromUrl();

  return (
    <>
      <PageHeader
        title="Detaljer"
        tilbakeLenke={{
          navn: "Tilbake til utbetalinger",
          url: internalNavigation(orgnr).utbetalinger,
        }}
      />

      <HStack gap="2" justify="space-between">
        <Heading level="2" size="medium">
          Innsending
        </Heading>
        <a href={`/${orgnr}/utbetaling/${id}/kvittering/lastned`} target="_blank">
          <Button variant="tertiary-neutral" size="small">
            <span className="flex gap-2 items-center">
              Last ned som PDF <FilePdfIcon fontSize={35} />
            </span>
          </Button>
        </a>
      </HStack>
      <Separator />

      <VStack gap="6" className="max-w-[1250px] mt-5">
        <GenerelleDetaljer utbetaling={utbetaling} utenTittel />

        <GenerelleUtbetalingDetaljer utbetaling={utbetaling} />
        <InnsendtAvUtbetalingDetaljer />
        <BetalingsInformasjon utbetaling={utbetaling} />
        <Separator />
        <UtbetalingStatusList utbetaling={utbetaling} />
        {utbetaling.status !== ArrFlateUtbetalingStatus.BEHANDLES_AV_NAV && tilsagn.length ? (
          // TODO: spiss mot et tilsagn?
          <UtbetalingTilsagnDetaljer tilsagn={tilsagn[0]} />
        ) : null}
      </VStack>
    </>
  );
}
