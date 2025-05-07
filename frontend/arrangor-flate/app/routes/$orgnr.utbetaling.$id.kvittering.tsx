import { FilePdfIcon } from "@navikt/aksel-icons";
import { Alert, BodyLong, BodyShort, ExpansionCard, Link, VStack } from "@navikt/ds-react";
import { ArrangorflateService } from "api-client";
import {
  Link as ReactRouterLink,
  LoaderFunction,
  MetaFunction,
  useParams,
  useLoaderData,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { internalNavigation } from "../internal-navigation";
import { formaterDatoTid, problemDetailResponse, useOrgnrFromUrl } from "../utils";
import { PageHeader } from "~/components/PageHeader";

type UtbetalingKvitteringData = {
  mottattTidspunkt: string;
  kontonummer: string;
};

export const meta: MetaFunction = () => {
  return [
    { title: "Kvittering for innsending" },
    { name: "description", content: "Kvittering for innsending" },
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

  const [{ data: utbetaling, error: utbetalingError }] = await Promise.all([
    ArrangorflateService.getArrFlateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError || !utbetaling) {
    throw problemDetailResponse(utbetalingError);
  }
  const kontonummer = utbetaling.betalingsinformasjon.kontonummer;
  const mottattTidspunkt = utbetaling.godkjentAvArrangorTidspunkt ?? new Date().toDateString();

  return { mottattTidspunkt, kontonummer };
};

export default function UtbetalingKvittering() {
  const { mottattTidspunkt, kontonummer } = useLoaderData<UtbetalingKvitteringData>();
  const { id } = useParams();
  const orgnr = useOrgnrFromUrl();

  return (
    <>
      <VStack gap="5" className="max-w-[50%] my-5 mx-auto">
        <PageHeader
          title="Innsendingen er mottatt"
          tilbakeLenke={{
            navn: "Tilbake til oversikten",
            url: internalNavigation(orgnr).utbetalinger,
          }}
        />
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
              <BodyShort>Mottatt av Nav: {formaterDatoTid(mottattTidspunkt)}</BodyShort>
              <BodyShort>Orgnummer: {orgnr}</BodyShort>
              <br />
              {id && (
                <>
                  <BodyLong>
                    Her kan du se{" "}
                    <Link as={ReactRouterLink} to={internalNavigation(orgnr).detaljer(id)}>
                      status på utbetalingen.
                    </Link>
                  </BodyLong>
                  <br />
                  <BodyShort>Innsending:</BodyShort>
                  <Link href={`/${orgnr}/utbetaling/${id}/kvittering/lastned`} target="_blank">
                    Innsendingskvittering (åpnes i ny fane) <FilePdfIcon title="Pdf" />
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
              <BodyShort>{kontonummer}</BodyShort>
            </VStack>
          </ExpansionCard.Content>
        </ExpansionCard>
      </VStack>
    </>
  );
}
