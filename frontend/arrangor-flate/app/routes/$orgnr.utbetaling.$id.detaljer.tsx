import { FilePdfIcon } from "@navikt/aksel-icons";
import { Box, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { ArrangorflateService, ArrFlateUtbetaling } from "api-client";
import { LoaderFunction, MetaFunction, useLoaderData, useParams } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { PageHeader } from "~/components/PageHeader";
import { Separator } from "~/components/Separator";
import BetalingsInformasjon from "~/components/utbetaling/BetalingsInformasjon";
import { GenerelleDetaljer } from "~/components/utbetaling/GenerelleDetaljer";
import GenerelleUtbetalingDetaljer from "~/components/utbetaling/GenerelleUtbetalingDetaljer";
import InnsendtUtbetalingDetaljer from "~/components/utbetaling/InnsendtUtbetalingDetaljer";
import UtbetalingStatusList from "~/components/utbetaling/UtbetalingStatusList";
import { internalNavigation } from "../internal-navigation";
import { problemDetailResponse, useOrgnrFromUrl } from "../utils";

type UtbetalingDetaljerSideData = {
  utbetaling: ArrFlateUtbetaling;
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

  const [{ data: utbetaling, error: utbetalingError }] = await Promise.all([
    ArrangorflateService.getArrFlateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError || !utbetaling) {
    throw problemDetailResponse(utbetalingError);
  }

  return { utbetaling };
};

export default function UtbetalingDetaljerSide() {
  const { utbetaling } = useLoaderData<UtbetalingDetaljerSideData>();
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
        <InnsendtUtbetalingDetaljer utbetaling={utbetaling} />
        <BetalingsInformasjon utbetaling={utbetaling} />
        <Box
          background="bg-subtle"
          padding="6"
          borderRadius="medium"
          borderColor="border-subtle"
          borderWidth={"2 1 1 1"}
        >
          <VStack gap="6">
            <UtbetalingStatusList utbetaling={utbetaling} />
          </VStack>
        </Box>
      </VStack>
    </>
  );
}
