import { Alert, BodyShort, Heading, HStack, Tabs } from "@navikt/ds-react";
import {
  ArrangorflateService,
  GjennomforingerTableResponse,
  GjennomforingOversiktType,
} from "api-client";
import { LoaderFunctionArgs, MetaFunction, useLoaderData } from "react-router";
import { apiHeaders } from "~/auth/auth.server";
import { problemDetailResponse } from "~/utils/validering";
import { InnsendingLayout } from "~/components/common/InnsendingLayout";
import { tekster } from "~/tekster";
import { getTabStateOrDefault, useTabState } from "~/hooks/useTabState";
import { useFileStorage } from "~/hooks/useFileStorage";
import { useEffect } from "react";
import { DataDrivenTable } from "@mr/frontend-common";

export const meta: MetaFunction = () => {
  return [
    { title: "Tiltaksoversikt - Opprett krav om utbetaling" },
    {
      name: "description",
      content: "Velg et tiltak for å opprette krav om utbetaling",
    },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  const tabState = getTabStateOrDefault(new URL(request.url));
  const gjennomforingType =
    tabState === "aktive" ? GjennomforingOversiktType.AKTIVE : GjennomforingOversiktType.HISTORISKE;
  const { data: gjennomforingerTabell, error: gjennomforingerError } =
    await ArrangorflateService.getArrangørersTiltakTabell({
      path: { type: gjennomforingType },
      headers: await apiHeaders(request),
    });
  // eslint-disable-next-line no-console
  console.log(gjennomforingerTabell, gjennomforingerError);
  if (gjennomforingerError) {
    throw problemDetailResponse(gjennomforingerError);
  }
  if (gjennomforingType == GjennomforingOversiktType.AKTIVE) {
    return { aktive: gjennomforingerTabell };
  }
  return {
    historiske: gjennomforingerTabell,
  };
}

type Tabs = "aktive" | "historiske";

export default function OpprettKravTiltaksOversikt() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const storage = useFileStorage();
  const { aktive, historiske } = useLoaderData<typeof loader>();

  useEffect(() => {
    storage.clear();
  });

  return (
    <InnsendingLayout contentGap="6">
      <Heading level="2" size="large">
        {tekster.bokmal.gjennomforing.headingTitle}
      </Heading>
      <Tabs defaultValue={currentTab} onChange={(tab) => setTab(tab as Tabs)}>
        <Tabs.List>
          <Tabs.Tab value="aktive" label={tekster.bokmal.gjennomforing.oversiktFaner.aktive} />
          <Tabs.Tab
            value="historiske"
            label={tekster.bokmal.gjennomforing.oversiktFaner.historiske}
          />
        </Tabs.List>
        <Tabs.Panel value="aktive" className="w-full">
          <TabellVisning data={aktive} />
        </Tabs.Panel>
        <Tabs.Panel value="historiske" className="w-full">
          <TabellVisning data={historiske} />
        </Tabs.Panel>
      </Tabs>
    </InnsendingLayout>
  );
}

interface TabellVisningProps {
  data: GjennomforingerTableResponse | undefined;
}

function TabellVisning({ data }: TabellVisningProps) {
  if (!data?.table) {
    return (
      <HStack align="center" justify="center" padding="32">
        <Alert variant="info">
          <BodyShort>
            Det finnes ingen registrerte tiltak du kan sende inn utbetalingskrav for.
          </BodyShort>
          <BodyShort>Ta eventuelt kontakt med Nav ved behov.</BodyShort>
        </Alert>
      </HStack>
    );
  }
  return <DataDrivenTable data={data.table} />;
}
