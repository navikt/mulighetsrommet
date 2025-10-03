import { Heading, Tabs } from "@navikt/ds-react";
import { ArrangorflateService, GjennomforingerTableResponse } from "api-client";
import { LoaderFunction, MetaFunction, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { problemDetailResponse } from "~/utils/validering";
import { DataDrivenTable } from "~/components/table/DataDrivenTable";
import { InnsendingLayout } from "~/components/common/InnsendingLayout";
import { tekster } from "~/tekster";
import { useTabState } from "~/hooks/useTabState";

type LoaderData = {
  gjennomforingerTabeller: GjennomforingerTableResponse;
  orgnr: string;
  arrangor: string;
};

export const meta: MetaFunction = () => {
  return [
    { title: "Tiltaksoversikt - Opprett krav om utbetaling" },
    {
      name: "description",
      content: "Velg et tiltak for å opprette krav om utbetaling",
    },
  ];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<LoaderData> => {
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
  const [
    { data: gjennomforingerTabeller, error: gjennomforingerError },
    { data: arrangortilganger, error: arrangorError },
  ] = await Promise.all([
    ArrangorflateService.getArrangorflateGjennomforingerTabeller({
      path: { orgnr },
      headers: await apiHeaders(request),
    }),
    ArrangorflateService.getArrangorerInnloggetBrukerHarTilgangTil({
      headers: await apiHeaders(request),
    }),
  ]);

  if (gjennomforingerError) {
    throw problemDetailResponse(gjennomforingerError);
  }
  if (!arrangortilganger) {
    throw problemDetailResponse(arrangorError);
  }

  const arrangor = arrangortilganger.find((a) => a.organisasjonsnummer === orgnr)?.navn;
  if (!arrangor) throw new Error("Finner ikke arrangør");

  return {
    orgnr,
    arrangor,
    gjennomforingerTabeller,
  };
};

type Tabs = "aktive" | "historiske";

export default function OpprettKravTiltaksOversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const { gjennomforingerTabeller } = useLoaderData<LoaderData>();

  return (
    <InnsendingLayout contentGap="6">
      <Heading level="2" size="large">
        Opprett krav om utbetaling
      </Heading>
      <Tabs defaultValue={currentTab} onChange={(tab) => setTab(tab as Tabs)}>
        <Tabs.List>
          <Tabs.Tab value="aktive" label={tekster.bokmal.utbetaling.oversiktFaner.aktive} />
          <Tabs.Tab value="historiske" label={tekster.bokmal.utbetaling.oversiktFaner.historiske} />
        </Tabs.List>
        <Tabs.Panel value="aktive" className="w-full">
          <DataDrivenTable data={gjennomforingerTabeller.aktive} zebraStripes />
        </Tabs.Panel>
        <Tabs.Panel value="historiske" className="w-full">
          <DataDrivenTable data={gjennomforingerTabeller.historiske} zebraStripes />
        </Tabs.Panel>
      </Tabs>
    </InnsendingLayout>
  );
}
