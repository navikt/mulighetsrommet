import { ArrangorflateService, ArrangorflateTilsagn, ArrFlateUtbetaling } from "api-client";
import { LoaderFunction, useLoaderData, Link as ReactRouterLink, MetaFunction } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { ManglendeMidlerAlert } from "~/components/utbetaling/ManglendeMidlerAlert";
import { UtbetalingManglendeTilsagnAlert } from "~/components/utbetaling/UtbetalingManglendeTilsagnAlert";
import { useOrgnrFromUrl, pathByOrgnr } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import { formaterPeriode } from "@mr/frontend-common/utils/date";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 1 av 3: Innsendingsinformasjon - Godkjenn innsending" },
    {
      name: "description",
      content: "Grunnleggende informasjon om innsendingen",
    },
  ];
};

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

  const harTilsagn = tilsagn.length > 0;

  return (
    <>
      <Heading level="2" spacing size="large">
        Innsendingsinformasjon
      </Heading>
      <VStack gap="4">
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
        <Heading level="3" size="medium">
          Tilgjengelige tilsagn
        </Heading>
        {!harTilsagn && <UtbetalingManglendeTilsagnAlert />}
        <ManglendeMidlerAlert tilsagn={tilsagn} belopTilUtbetaling={utbetaling.beregning.belop} />
        {tilsagn.map((tilsagn) => (
          <TilsagnDetaljer
            key={tilsagn.bestillingsnummer}
            tilsagn={tilsagn}
            headingLevel="4"
            minimal
          />
        ))}
        {harTilsagn && (
          <HStack gap="4" className="mt-4">
            <Button
              as={ReactRouterLink}
              type="button"
              variant="tertiary"
              to={pathByOrgnr(orgnr).utbetalinger}
            >
              Avbryt
            </Button>
            <Button
              as={ReactRouterLink}
              aria-label="Neste"
              to={pathByOrgnr(orgnr).beregning(utbetaling.id)}
            >
              Neste
            </Button>
          </HStack>
        )}
      </VStack>
    </>
  );
}
