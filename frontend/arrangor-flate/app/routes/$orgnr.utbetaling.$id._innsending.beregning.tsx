import {
  Alert,
  BodyShort,
  Button,
  GuidePanel,
  Heading,
  HStack,
  Link,
  VStack,
} from "@navikt/ds-react";
import { ArrangorflateService, ArrFlateUtbetaling, RelevanteForslag } from "api-client";
import type { LoaderFunction, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { getEnvironment } from "~/services/environment";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { getBeregningDetaljer } from "~/utils/beregning";
import { deltakerOversiktLenke, pathByOrgnr, useOrgnrFromUrl } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import { DeltakelserTable } from "~/components/deltakelse/DeltakelserTable";
import { tekster } from "~/tekster";
import { formaterPeriode } from "@mr/frontend-common/utils/date";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 2 av 3: Beregning - Godkjenn innsending" },
    {
      name: "description",
      content: "Informasjon om beregning og deltakere",
    },
  ];
};

type LoaderData = {
  utbetaling: ArrFlateUtbetaling;
  relevanteForslag: RelevanteForslag[];
  deltakerlisteUrl: string;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());

  const { id } = params;
  if (!id) {
    throw new Response("Mangler id", { status: 400 });
  }

  const [
    { data: utbetaling, error: utbetalingError },
    { data: relevanteForslag, error: relevanteForslagError },
  ] = await Promise.all([
    ArrangorflateService.getArrFlateUtbetaling({
      path: { id },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getRelevanteForslag({
      path: { id },
      headers: await apiHeaders(request),
    }),
  ]);

  if (utbetalingError || !utbetaling) {
    throw problemDetailResponse(utbetalingError);
  }
  if (relevanteForslagError || !relevanteForslag) {
    throw problemDetailResponse(relevanteForslagError);
  }

  return { utbetaling, deltakerlisteUrl, relevanteForslag };
};

export default function UtbetalingBeregning() {
  const orgnr = useOrgnrFromUrl();
  const { utbetaling, deltakerlisteUrl, relevanteForslag } = useLoaderData<LoaderData>();

  return (
    <VStack gap="4">
      <Heading level="2" spacing size="large">
        Beregning
      </Heading>
      <GuidePanel>
        <BodyShort>
          {tekster.bokmal.utbetaling.beregning.infotekstDeltakerliste.intro}{" "}
          <Link as={ReactRouterLink} to={deltakerlisteUrl}>
            Deltakeroversikten
          </Link>
          .
        </BodyShort>
        <BodyShort>{tekster.bokmal.utbetaling.beregning.infotekstDeltakerliste.utro}</BodyShort>
      </GuidePanel>
      <Heading level="3" size="medium">
        Deltakere
      </Heading>
      <VStack gap="4">
        {utbetaling.beregning.type !== "FRI" && (
          <>
            {utbetaling.beregning.stengt.length > 0 && (
              <Alert variant={"info"}>
                {tekster.bokmal.utbetaling.beregning.stengtHosArrangor}
                <ul>
                  {utbetaling.beregning.stengt.map(({ periode, beskrivelse }) => (
                    <li key={periode.start + periode.slutt}>
                      {formaterPeriode(periode)}: {beskrivelse}
                    </li>
                  ))}
                </ul>
              </Alert>
            )}
            <DeltakelserTable
              periode={utbetaling.periode}
              beregning={utbetaling.beregning}
              relevanteForslag={relevanteForslag}
              deltakerlisteUrl={deltakerlisteUrl}
            />
          </>
        )}
        <Definisjonsliste
          definitions={getBeregningDetaljer(utbetaling.beregning)}
          className="my-2"
        />
        <HStack gap="4">
          <Button
            as={ReactRouterLink}
            type="button"
            variant="tertiary"
            to={pathByOrgnr(orgnr).innsendingsinformasjon(utbetaling.id)}
          >
            Tilbake
          </Button>
          <Button
            as={ReactRouterLink}
            className="justify-self-end"
            to={pathByOrgnr(orgnr).oppsummering(utbetaling.id)}
          >
            Neste
          </Button>
        </HStack>
      </VStack>
    </VStack>
  );
}
