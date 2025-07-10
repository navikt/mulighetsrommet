import {
  BodyShort,
  Button,
  GuidePanel,
  Heading,
  HStack,
  VStack,
  Link,
  Alert,
} from "@navikt/ds-react";
import {
  ArrangorflateService,
  ArrFlateUtbetaling,
  RelevanteForslag,
  UtbetalingBeregningType,
} from "api-client";
import type { LoaderFunction, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { Environment, getEnvironment } from "~/services/environment";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import { getBeregningDetaljer } from "~/utils/beregning";
import { useOrgnrFromUrl, pathByOrgnr } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import { DeltakelserTable } from "~/components/deltakelse/DeltakelserTable";
import { tekster } from "~/tekster";
import { formaterPeriode } from "~/utils/date";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 2 av 3: Beregning - Godkjenn innsending" },
    {
      name: "description",
      content: "Informasjon om beregning og deltakere",
    },
  ];
};

function deltakerOversiktLenke(env: Environment): string {
  if (env === Environment.DevGcp) {
    return "https://amt.intern.dev.nav.no/deltakeroversikt";
  }
  return "https://nav.no/deltakeroversikt";
}

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
          .{tekster.bokmal.utbetaling.beregning.infotekstDeltakerliste.utro}
        </BodyShort>
        <BodyShort>{tekster.bokmal.utbetaling.beregning.infotekstDeltakerliste.utro}</BodyShort>
      </GuidePanel>
      <Heading level="3" size="medium">
        Deltakere
      </Heading>
      <VStack gap="4">
        {utbetaling.beregning.type === UtbetalingBeregningType.PRIS_PER_MANEDSVERK && (
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
