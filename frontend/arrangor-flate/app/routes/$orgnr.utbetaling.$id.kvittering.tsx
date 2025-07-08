import { Alert, BodyLong, BodyShort, ExpansionCard, Link, VStack } from "@navikt/ds-react";
import { ArrangorflateService } from "api-client";
import {
  LoaderFunction,
  MetaFunction,
  Link as ReactRouterLink,
  useLoaderData,
  useParams,
} from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { PageHeader } from "~/components/PageHeader";
import { tekster } from "../tekster";
import { problemDetailResponse } from "~/utils/validering";
import { pathByOrgnr, useOrgnrFromUrl } from "~/utils/navigation";

type UtbetalingKvitteringData = {
  mottattTidspunkt: string;
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
    ArrangorflateService.getArrFlateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError || !utbetaling) {
    throw problemDetailResponse(utbetalingError);
  }
  const kontonummer = utbetaling.betalingsinformasjon.kontonummer;
  const mottattTidspunkt = utbetaling.godkjentAvArrangorTidspunkt;

  if (!mottattTidspunkt) throw new Response("Ugyldig", { status: 400 });
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
          title={tekster.bokmal.utbetaling.kvittering.headingTitle}
          tilbakeLenke={{
            navn: tekster.bokmal.tilbakeTilOversikt,
            url: pathByOrgnr(orgnr).utbetalinger,
          }}
        />
        <Alert variant="success">{tekster.bokmal.utbetaling.kvittering.successMelding}</Alert>
        <ExpansionCard defaultOpen aria-label="Kvittering">
          <ExpansionCard.Header>
            <ExpansionCard.Title>
              {tekster.bokmal.utbetaling.kvittering.kvitteringTitle}
            </ExpansionCard.Title>
          </ExpansionCard.Header>
          <ExpansionCard.Content>
            <VStack gap="2">
              <BodyShort>
                {tekster.bokmal.utbetaling.kvittering.mottattAv(mottattTidspunkt)}
              </BodyShort>
              <BodyShort>{tekster.bokmal.utbetaling.kvittering.orgnr(orgnr)}</BodyShort>
              <br />
              {id && (
                <BodyLong>
                  {tekster.bokmal.utbetaling.kvittering.statusLenkeIntro}{" "}
                  <Link as={ReactRouterLink} to={pathByOrgnr(orgnr).detaljer(id)}>
                    {tekster.bokmal.utbetaling.kvittering.statusLenkeTekst.toLowerCase()}
                  </Link>
                </BodyLong>
              )}
            </VStack>
          </ExpansionCard.Content>
        </ExpansionCard>
        <ExpansionCard open aria-label="Kvittering">
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
    </>
  );
}
