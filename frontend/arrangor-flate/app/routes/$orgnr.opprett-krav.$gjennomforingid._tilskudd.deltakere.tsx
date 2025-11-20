import { Alert, BodyShort, GuidePanel, Heading, Link, VStack } from "@navikt/ds-react";
import {
  ArrangorflateService,
  OpprettKravDeltakere,
  OpprettKravDeltakereGuidePanelType,
  StengtPeriode,
} from "api-client";
import type { LoaderFunction, MetaFunction } from "react-router";
import { Link as ReactRouterLink, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { getEnvironment } from "~/services/environment";
import { Definisjonsliste2 } from "~/components/common/Definisjonsliste";
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
import { OpprettKravVeiviserButtons } from "~/components/OpprettKravVeiviserButtons";
import { DataDrivenTable } from "@mr/frontend-common";

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

export default function OpprettKravDeltakerTabell() {
  const orgnr = useOrgnrFromUrl();
  const gjennomforingId = useGjennomforingIdFromUrl();
  const { deltakselseInfo, deltakerlisteUrl } = useLoaderData<LoaderData>();

  return (
    <VStack gap="4">
      <Heading level="2" spacing size="large">
        Oversikt over deltakere
      </Heading>
      <DeltakelseGuidePanel
        deltakerlisteUrl={deltakerlisteUrl}
        guidePanelType={deltakselseInfo.guidePanel}
      />
      <VStack gap="4">
        {deltakselseInfo.stengtHosArrangor.length > 0 && (
          <Alert variant={"info"}>
            {tekster.bokmal.utbetaling.beregning.stengtHosArrangor}
            <ul>
              {deltakselseInfo.stengtHosArrangor.map(({ periode, beskrivelse }: StengtPeriode) => (
                <li key={periode.start + periode.slutt}>
                  {formaterPeriode(periode)}: {beskrivelse}
                </li>
              ))}
            </ul>
          </Alert>
        )}
        <DataDrivenTable data={deltakselseInfo.tabell} />
        <Definisjonsliste2 definitions={deltakselseInfo.tabellFooter} className="my-2" />
        <OpprettKravVeiviserButtons
          navigering={deltakselseInfo.navigering}
          orgnr={orgnr}
          gjennomforingId={gjennomforingId}
        />
      </VStack>
    </VStack>
  );
}

interface DeltakelseGuidePanelProps {
  deltakerlisteUrl: string;
  guidePanelType: OpprettKravDeltakereGuidePanelType;
}

function DeltakelseGuidePanel({ deltakerlisteUrl, guidePanelType }: DeltakelseGuidePanelProps) {
  switch (guidePanelType) {
    case OpprettKravDeltakereGuidePanelType.TIMESPRIS:
      return (
        <GuidePanel>
          <BodyShort>
            Her vises deltakere som er registrert på tiltaket. Det er disse deltakerne det skal
            faktureres for. Kontrollér at deltakelsene stemmer.
          </BodyShort>
        </GuidePanel>
      );
    case OpprettKravDeltakereGuidePanelType.GENERELL:
    default:
      return (
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
      );
  }
}
