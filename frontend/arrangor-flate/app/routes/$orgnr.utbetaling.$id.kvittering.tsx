import { Alert, BodyLong, BodyShort, Box, ExpansionCard, Link, VStack } from "@navikt/ds-react";
import { ArrangorflateService } from "api-client";
import {
  Link as ReactRouterLink,
  LoaderFunction,
  MetaFunction,
  useLoaderData,
  useParams,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { tekster } from "~/tekster";
import { problemDetailResponse } from "~/utils/validering";
import { pathTo, useOrgnrFromUrl } from "~/utils/navigation";
import { PageHeading } from "~/components/common/PageHeading";

type UtbetalingKvitteringData = {
  mottattDato: string;
  utbetalesTidligstDato: string | null;
  kontonummer: string | null;
};

export const meta: MetaFunction = () => {
  return [
    { title: "Innsendingskvittering" },
    { name: "description", content: "Kvittering for mottatt innsending" },
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
    ArrangorflateService.getArrangorflateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError) {
    throw problemDetailResponse(utbetalingError);
  }

  const mottattDato = utbetaling.innsendtAvArrangorDato;
  if (!mottattDato) {
    throw new Response("Mangler dato for innsending", { status: 400 });
  }

  return {
    mottattDato,
    utbetalesTidligstDato: utbetaling.utbetalesTidligstDato,
    kontonummer: utbetaling.betalingsinformasjon?.kontonummer ?? null,
  };
};

export default function UtbetalingKvittering() {
  const { mottattDato, utbetalesTidligstDato, kontonummer } =
    useLoaderData<UtbetalingKvitteringData>();
  const { id } = useParams();
  const orgnr = useOrgnrFromUrl();

  return (
    <Box background="bg-default" padding="8" borderRadius="large" marginInline="auto">
      <VStack gap="5">
        <PageHeading
          title={tekster.bokmal.utbetaling.kvittering.headingTitle}
          tilbakeLenke={{
            navn: tekster.bokmal.tilbakeTilOversikt,
            url: pathTo.utbetalinger,
          }}
        />
        <Alert variant="success">{tekster.bokmal.utbetaling.kvittering.successMelding}</Alert>
        <ExpansionCard
          defaultOpen
          aria-label={tekster.bokmal.utbetaling.kvittering.kvitteringTitle}
        >
          <ExpansionCard.Header>
            <ExpansionCard.Title>
              {tekster.bokmal.utbetaling.kvittering.kvitteringTitle}
            </ExpansionCard.Title>
          </ExpansionCard.Header>
          <ExpansionCard.Content>
            <VStack gap="2">
              <BodyShort>{tekster.bokmal.utbetaling.kvittering.mottattAv(mottattDato)}</BodyShort>
              {utbetalesTidligstDato && (
                <BodyShort spacing>
                  {tekster.bokmal.utbetaling.kvittering.utbetalesTidligstDato(
                    utbetalesTidligstDato,
                  )}
                </BodyShort>
              )}
              <BodyShort spacing>{tekster.bokmal.utbetaling.kvittering.orgnr(orgnr)}</BodyShort>
              {id && (
                <BodyLong>
                  {tekster.bokmal.utbetaling.kvittering.statusLenkeIntro}{" "}
                  <Link as={ReactRouterLink} to={pathTo.detaljer(orgnr, id)}>
                    {tekster.bokmal.utbetaling.kvittering.statusLenkeTekst.toLowerCase()}
                  </Link>
                </BodyLong>
              )}
            </VStack>
          </ExpansionCard.Content>
        </ExpansionCard>
        <ExpansionCard open aria-label={tekster.bokmal.utbetaling.kvittering.kontoTitle}>
          <ExpansionCard.Header>
            <ExpansionCard.Title>
              {tekster.bokmal.utbetaling.kvittering.kontoTitle}
            </ExpansionCard.Title>
          </ExpansionCard.Header>
          <ExpansionCard.Content>
            <VStack gap="2">
              <BodyShort weight="semibold">
                {tekster.bokmal.utbetaling.kvittering.kontonummerRegistrert}
              </BodyShort>
              <BodyShort>{kontonummer}</BodyShort>
            </VStack>
          </ExpansionCard.Content>
        </ExpansionCard>
      </VStack>
    </Box>
  );
}
