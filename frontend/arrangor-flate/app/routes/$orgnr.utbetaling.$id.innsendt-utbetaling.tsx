import { FilePdfIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  ExpansionCard,
  Heading,
  HStack,
  Link,
  VStack,
} from "@navikt/ds-react";
import { ArrangorflateService, ArrangorflateTilsagn, ArrFlateUtbetaling } from "api-client";
import { Link as ReactRouterLink, LoaderFunction, MetaFunction, useParams } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { internalNavigation } from "../internal-navigation";
import { problemDetailResponse, useOrgnrFromUrl } from "../utils";

type UtbetalingKvitteringData = {
  utbetaling: ArrFlateUtbetaling;
  tilsagn: ArrangorflateTilsagn[];
};

export const meta: MetaFunction = () => {
  return [
    { title: "Innsendt utbetaling" },
    { name: "description", content: "Arrangørflate for innsendt utbetaling" },
  ];
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
  const { id } = useParams();
  const orgnr = useOrgnrFromUrl();

  return (
    <>
      <VStack gap="5" className="max-w-[50%] mt-5 mx-auto">
        <Heading size="large" level="2">
          Innsendingen er mottatt
        </Heading>
        <Alert variant="success">
          Vi har mottatt ditt krav om utbetaling, og utbetalingen er nå til behandling hos Nav. Vi
          vil ta kontakt med deg dersom vi trenger mer informasjon.
        </Alert>
        <ExpansionCard open aria-label="Kvittering">
          <ExpansionCard.Header>
            <ExpansionCard.Title>Kvittering for innsending</ExpansionCard.Title>
          </ExpansionCard.Header>
          <ExpansionCard.Content>
            <VStack gap="2">
              <BodyShort>Mottatt av Nav: 01.05.2025</BodyShort>
              {id && (
                <>
                  <BodyLong>
                    Du kan se status for utbetalingen{" "}
                    <Link as={ReactRouterLink} to={internalNavigation(orgnr).detaljer(id)}>
                      her
                    </Link>
                    .
                  </BodyLong>
                  <br />
                  <BodyShort>Innsending:</BodyShort>
                  <Link href={`/${orgnr}/utbetaling/${id}/kvittering/lastned`} target="_blank">
                    Krav om utbetaling (åpnes i ny fane) <FilePdfIcon title="Pdf" />
                  </Link>
                </>
              )}
            </VStack>
          </ExpansionCard.Content>
        </ExpansionCard>
        <ExpansionCard open aria-label="Kvittering">
          <ExpansionCard.Header>
            <ExpansionCard.Title>Konto for utbetaling</ExpansionCard.Title>
          </ExpansionCard.Header>
          <ExpansionCard.Content>
            <VStack gap="2">
              <BodyShort weight="semibold">Vi har registrert følgende kontonummer:</BodyShort>
              <BodyShort>2313.2123.12</BodyShort>
            </VStack>
          </ExpansionCard.Content>
        </ExpansionCard>
        <HStack gap="4">
          <Button
            as={ReactRouterLink}
            to={internalNavigation(orgnr).utbetalinger}
            variant="secondary"
          >
            Tilbake til utbetalinger
          </Button>
        </HStack>
      </VStack>
    </>
  );
}
