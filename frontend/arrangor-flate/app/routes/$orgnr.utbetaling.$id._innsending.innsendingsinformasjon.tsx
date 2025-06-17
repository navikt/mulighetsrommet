import { ArrangorflateService, ArrangorflateTilsagn, ArrFlateUtbetaling } from "api-client";
import { LoaderFunction, useLoaderData, Link as ReactRouterLink } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { internalNavigation } from "../internal-navigation";
import { formaterPeriode, problemDetailResponse, useOrgnrFromUrl } from "../utils";
import { Alert, Box, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { Definisjonsliste } from "~/components/Definisjonsliste";
import { ManglendeMidlerAlert } from "~/components/ManglendeMidlerAlert";

type LoaderData = {
  utbetaling: ArrFlateUtbetaling;
  tilsagn: ArrangorflateTilsagn[];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { id } = params;
  if (!id) throw Error("Mangler orgnr");

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

  return {
    utbetaling,
    tilsagn,
  };
};

export default function TilsagnDetaljerPage() {
  const { utbetaling, tilsagn } = useLoaderData<LoaderData>();
  const orgnr = useOrgnrFromUrl();

  return (
    <>
      <Heading level="2" spacing size="large">
        Innsendingsinformasjon
      </Heading>
      <VStack gap="4">
        <Box>
          <Definisjonsliste
            definitions={[
              {
                key: "Arrangør",
                value: `${utbetaling.arrangor.navn} - ${utbetaling.arrangor.organisasjonsnummer}`,
              },
              { key: "Tiltaksnavn", value: utbetaling.gjennomforing.navn },
              { key: "Tiltakstype", value: utbetaling.tiltakstype.navn },
              { key: "Utbetalingsperiode", value: formaterPeriode(utbetaling.periode) },
              {
                key: "Frist for innsending",
                value: "Kravet må sendes inn senest to måneder etter at tilsagnsperioden går ut.",
              },
            ]}
          />
        </Box>
        <Heading level="3" size="medium">
          Tilgjengelige tilsagn
        </Heading>
        {tilsagn.length < 1 && (
          <Alert variant={"warning"}>
            <Heading spacing level="4" size="small">
              Tilsagn mangler
            </Heading>
            Det finnes ingen godkjente tilsagn tilgjengelig for denne utbetalingen. Dere kan ikke
            sende inn kravet før Nav har godkjent et tilsagn for utbetalingsperioden. Vennligst ta
            kontakt med Nav.
          </Alert>
        )}
        {tilsagn.length === 1 && tilsagn.at(0)!.gjenstaendeBelop < utbetaling.beregning.belop && (
          <ManglendeMidlerAlert />
        )}
        {tilsagn.map((tilsagn) => (
          <TilsagnDetaljer tilsagn={tilsagn} />
        ))}
        {tilsagn.length >= 1 && (
          <HStack gap="4" className="mt-4">
            <Button
              as={ReactRouterLink}
              type="button"
              variant="tertiary"
              to={internalNavigation(orgnr).utbetalinger}
            >
              Avbryt
            </Button>
            <Button
              as={ReactRouterLink}
              aria-label="Neste"
              to={internalNavigation(orgnr).beregning(utbetaling.id)}
            >
              Neste
            </Button>
          </HStack>
        )}
      </VStack>
    </>
  );
}
