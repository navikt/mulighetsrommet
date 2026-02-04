import { Alert, BodyLong, BodyShort, Box, ExpansionCard, Link, VStack } from "@navikt/ds-react";
import { Suspense } from "react";
import { Link as ReactRouterLink, MetaFunction } from "react-router";
import { Laster } from "~/components/common/Laster";
import { tekster } from "~/tekster";
import { pathTo, useIdFromUrl, useOrgnrFromUrl } from "~/utils/navigation";
import { PageHeading } from "~/components/common/PageHeading";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";

export const meta: MetaFunction = () => {
  return [
    { title: "Innsendingskvittering" },
    { name: "description", content: "Kvittering for mottatt innsending" },
  ];
};

export default function UtbetalingKvittering() {
  const id = useIdFromUrl();

  return (
    <Suspense fallback={<Laster tekst="Laster kvittering..." size="xlarge" />}>
      <UtbetalingKvitteringContent id={id} />
    </Suspense>
  );
}

function UtbetalingKvitteringContent({ id }: { id: string }) {
  const orgnr = useOrgnrFromUrl();
  const { data: utbetaling } = useArrangorflateUtbetaling(id);

  const mottattDato = utbetaling.innsendtAvArrangorDato;
  if (!mottattDato) {
    throw new Response("Mangler dato for innsending", { status: 400 });
  }

  const utbetalesTidligstDato = utbetaling.utbetalesTidligstDato;
  const kontonummer = utbetaling.betalingsinformasjon?.kontonummer ?? null;

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
