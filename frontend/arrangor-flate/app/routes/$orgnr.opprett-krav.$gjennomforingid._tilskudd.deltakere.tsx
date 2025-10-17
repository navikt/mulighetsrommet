import { Alert, BodyShort, GuidePanel, Heading, Link, VStack } from "@navikt/ds-react";
import { ArrangorflateService, OpprettKravDeltakere } from "api-client";
import type { LoaderFunction, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { getEnvironment } from "~/services/environment";
import { Definisjonsliste } from "~/components/common/Definisjonsliste";
import {
  deltakerOversiktLenke,
  getOrgnrGjennomforingIdFrom,
  useGjennomforingIdFromUrl,
  useOrgnrFromUrl,
} from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import { tekster } from "~/tekster";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { getStepTitle } from "./$orgnr.opprett-krav.$gjennomforingid._tilskudd";
import { getSession } from "~/sessions.server";
import { DataDrivenTable } from "~/components/table/DataDrivenTable";
import { OpprettKravVeiviserButtons } from "~/components/OpprettKravVeiviserButtons";

export const meta: MetaFunction = ({ matches }) => {
  return [
    {
      title: getStepTitle(matches),
    },
    {
      name: "description",
      content: "Informasjon om beregning og deltakere",
    },
  ];
};

type LoaderData = {
  deltakselseInfo: OpprettKravDeltakere;
  deltakerlisteUrl: string;
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const deltakerlisteUrl = deltakerOversiktLenke(getEnvironment());
  const { orgnr, gjennomforingId } = getOrgnrGjennomforingIdFrom(params);
  const session = await getSession(request.headers.get("Cookie"));
  const sessionPeriodeStart = session.get("periodeStart");
  const sessionPeriodeSlutt = session.get("periodeSlutt");
  if (!sessionPeriodeStart || !sessionPeriodeSlutt) {
    throw Error("Periode er ikke valgt");
  }

  const [{ data: deltakselseInfo, error: deltakereError }] = await Promise.all([
    ArrangorflateService.getOpprettKravDeltakere({
      path: { orgnr, gjennomforingId },
      query: { periodeStart: sessionPeriodeStart, periodeSlutt: sessionPeriodeSlutt },
      headers: await apiHeaders(request),
    }),
  ]);

  if (deltakereError) {
    throw problemDetailResponse(deltakereError);
  }

  return { deltakselseInfo, deltakerlisteUrl };
};

export default function UtbetalingBeregning() {
  const orgnr = useOrgnrFromUrl();
  const gjennomforingId = useGjennomforingIdFromUrl();
  const { deltakselseInfo, deltakerlisteUrl } = useLoaderData<LoaderData>();

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
        {deltakselseInfo.stengtHosArrangor.length > 0 && (
          <Alert variant={"info"}>
            {tekster.bokmal.utbetaling.beregning.stengtHosArrangor}
            <ul>
              {deltakselseInfo.stengtHosArrangor.map(({ periode, beskrivelse }) => (
                <li key={periode.start + periode.slutt}>
                  {formaterPeriode(periode)}: {beskrivelse}
                </li>
              ))}
            </ul>
          </Alert>
        )}
        <DataDrivenTable data={deltakselseInfo.tabell} />
        <Definisjonsliste definitions={deltakselseInfo.tabellFooter} className="my-2" />
        <OpprettKravVeiviserButtons
          navigering={deltakselseInfo.navigering}
          orgnr={orgnr}
          gjennomforingId={gjennomforingId}
        />
      </VStack>
    </VStack>
  );
}
