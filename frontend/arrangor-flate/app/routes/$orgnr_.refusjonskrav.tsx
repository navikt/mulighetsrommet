import {
  ArrangorflateService,
  ArrFlateRefusjonKravKompakt,
  RefusjonskravStatus,
} from "@mr/api-client-v2";
import { Tabs } from "@navikt/ds-react";
import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { useLoaderData } from "react-router";
import { RefusjonskravTable } from "~/components/refusjonskrav/RefusjonskravTable";
import { TilsagnTable } from "~/components/tilsagn/TilsagnTable";
import { PageHeader } from "../components/PageHeader";
import { useTabState } from "../hooks/useTabState";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export type Tabs = "aktive" | "historiske" | "tilsagnsoversikt";

export async function loader({ params }: LoaderFunctionArgs) {
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
  const { data: krav } = await ArrangorflateService.getAllRefusjonKrav({ path: { orgnr } });
  const { data: tilsagn } = await ArrangorflateService.getAllArrangorflateTilsagn({
    path: { orgnr },
  });
  if (!krav || !tilsagn) {
    throw new Error("Error");
  }

  return { krav, tilsagn };
}

export default function TilsagnDetaljer() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const { krav, tilsagn } = useLoaderData<typeof loader>();
  const historiske: ArrFlateRefusjonKravKompakt[] = krav.filter(
    (k) => k.status === RefusjonskravStatus.GODKJENT_AV_ARRANGOR,
  );
  const aktive = krav.filter((k) => k.status !== RefusjonskravStatus.GODKJENT_AV_ARRANGOR);

  return (
    <>
      <PageHeader title="Tilgjengelige refusjonskrav" />
      <Tabs defaultValue={currentTab} onChange={(tab) => setTab(tab as Tabs)}>
        <Tabs.List>
          <Tabs.Tab value="aktive" label="Aktive" />
          <Tabs.Tab value="historiske" label="Historiske" />
          <Tabs.Tab value="tilsagnsoversikt" label="Tilsagnsoversikt" />
        </Tabs.List>
        <Tabs.Panel value="aktive" className="w-full">
          <RefusjonskravTable krav={aktive} />
        </Tabs.Panel>
        <Tabs.Panel value="historiske" className="w-full">
          <RefusjonskravTable krav={historiske} />
        </Tabs.Panel>
        <Tabs.Panel value="tilsagnsoversikt" className="w-full">
          <TilsagnTable tilsagn={tilsagn} />
        </Tabs.Panel>
      </Tabs>
    </>
  );
}
