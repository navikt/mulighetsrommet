import { ArrangorflateService, RefusjonKravKompakt, RefusjonskravStatus } from "@mr/api-client";
import { Tabs } from "@navikt/ds-react";
import type { LoaderFunctionArgs, MetaFunction } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
import { RefusjonskravTable } from "~/components/refusjonskrav/RefusjonskravTable";
import { TilsagnTable } from "~/components/tilsagn/TilsagnTable";
import { checkValidToken, setupOpenApi } from "../auth/auth.server";
import { PageHeader } from "../components/PageHeader";
import { useTabState } from "../hooks/useTabState";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export type Tabs = "aktive" | "historiske" | "tilsagnsoversikt";

export async function loader({ request, params }: LoaderFunctionArgs) {
  await checkValidToken(request);
  await setupOpenApi(request);
  const { orgnr } = params;
  if (!orgnr) {
    throw new Error("Mangler orgnr");
  }
  const krav = await ArrangorflateService.getAllRefusjonKrav({ orgnr });
  const tilsagn = await ArrangorflateService.getAllArrangorflateTilsagn({ orgnr });

  return { krav, tilsagn };
}

export default function TilsagnDetaljer() {
  const [currentTab, setTab] = useTabState("forside-tab", "aktive");
  const { krav, tilsagn } = useLoaderData<typeof loader>();
  const historiske: RefusjonKravKompakt[] = krav.filter(
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
