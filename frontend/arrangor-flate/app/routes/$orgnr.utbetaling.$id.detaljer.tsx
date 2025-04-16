import { FilePdfIcon } from "@navikt/aksel-icons";
import {
  Accordion,
  Bleed,
  BodyShort,
  Box,
  Button,
  Heading,
  HStack,
  Spacer,
  VStack,
} from "@navikt/ds-react";
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
import InnsendtUtbetalingDetaljer from "~/components/utbetaling/InnsendtUtbetalingDetaljer";
import BetalingsInformasjon from "~/components/utbetaling/BetalingsInformasjon";
import UtbetalingTilsagnDetaljer from "~/components/utbetaling/UtbetalingTilsagnDetaljer";
import AccordionStyles from "~/components/Accordion.module.css";

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

  const visTilsagnsListe =
    utbetaling.status === ArrFlateUtbetalingStatus.UTBETALT && tilsagn.length;
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
          borderWidth={visTilsagnsListe ? "2 1 0 1" : "2 1 1 1"}
        >
          <VStack gap="6">
            <UtbetalingStatusList utbetaling={utbetaling} />
            {visTilsagnsListe ? (
              <Bleed marginInline="6" marginBlock="0 6" asChild>
                <Accordion>
                  {tilsagn.map((it) => {
                    return (
                      <Accordion.Item>
                        <Accordion.Header className={AccordionStyles.fullHeader}>
                          <HStack gap="2" width="100%">
                            <BodyShort>{it.gjennomforing.navn}</BodyShort>
                            <Spacer />
                            <BodyShort>{it.bestillingsnummer}</BodyShort>
                          </HStack>
                        </Accordion.Header>
                        <Accordion.Content>
                          <UtbetalingTilsagnDetaljer tilsagn={it} />
                        </Accordion.Content>
                      </Accordion.Item>
                    );
                  })}
                </Accordion>
              </Bleed>
            ) : null}
          </VStack>
        </Box>
      </VStack>
    </>
  );
}
