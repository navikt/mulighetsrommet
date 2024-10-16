import { RefusjonKravAft, RefusjonskravService, RefusjonskravStatus } from "@mr/api-client";
import { Tabs } from "@navikt/ds-react";
import type { LoaderFunctionArgs, MetaFunction } from "@remix-run/node";
import { json } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
import { RefusjonskravTable } from "~/components/refusjonskrav/RefusjonskravTable";
import { checkValidToken, setupOpenApi } from "../auth/auth.server";
import { PageHeader } from "../components/PageHeader";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  await checkValidToken(request);
  await setupOpenApi(request);
  const krav = await RefusjonskravService.getRefusjonskrav();

  return json({ krav });
}

export default function Refusjon() {
  const { krav } = useLoaderData<typeof loader>();
  const historiske: RefusjonKravAft[] = krav.filter(
    (k) => k.status === RefusjonskravStatus.GODKJENT_AV_ARRANGOR,
  );
  const aktive = krav.filter((k) => k.status !== RefusjonskravStatus.GODKJENT_AV_ARRANGOR);

  return (
    <>
      <PageHeader title="Tilgjengelige refusjonskrav" />
      <Tabs defaultValue="aktive">
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
        <Tabs.Panel value="tilsagnsoversikt" className="h-24 w-full bg-gray-50 p-4">
          Tilsagnsoversikt
        </Tabs.Panel>
      </Tabs>
    </>
  );
}
