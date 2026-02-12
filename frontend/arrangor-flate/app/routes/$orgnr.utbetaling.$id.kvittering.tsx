import { Suspense } from "react";
import { Laster } from "~/components/common/Laster";
import {
  BodyLong,
  BodyShort,
  Box,
  ExpansionCard,
  Link,
  LocalAlert,
  VStack,
} from "@navikt/ds-react";
import { Link as ReactRouterLink, MetaFunction } from "react-router";
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
    throw new Error("Utbetalingskravet er ikke sendt inn");
  }

  const utbetalesTidligstDato = utbetaling.utbetalesTidligstDato;
  const kontonummer = utbetaling.betalingsinformasjon?.kontonummer ?? null;

  return (
    <Box background="default" padding="space-32" borderRadius="8" marginInline="auto">
      <VStack gap="space-20">
        <PageHeading
          title={tekster.bokmal.utbetaling.kvittering.headingTitle}
          tilbakeLenke={{
            navn: tekster.bokmal.tilbakeTilOversikt,
            url: pathTo.utbetalinger,
          }}
        />
        <LocalAlert status="success">
          <LocalAlert.Header>
            <LocalAlert.Title>Innsendingen er mottatt</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            <BodyShort>{tekster.bokmal.utbetaling.kvittering.successMelding}</BodyShort>
          </LocalAlert.Content>
        </LocalAlert>
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
            <VStack gap="space-8">
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
            <VStack gap="space-8">
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
