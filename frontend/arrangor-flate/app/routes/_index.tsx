import { RefusjonskravDto, RefusjonskravService } from "@mr/api-client";
import { Heading, Tabs, VStack } from "@navikt/ds-react";
import type { LoaderFunctionArgs, MetaFunction } from "@remix-run/node";
import { json, redirect } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
import { RefusjonskravTable } from "~/components/refusjonskrav/RefusjonskravTable";
import { getTilganger } from "../auth/altinn.server";
import { setupOpenApi } from "../auth/auth.server";
import { PageHeader } from "../components/PageHeader";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  const tilganger = await getTilganger(request);

  if (tilganger.roller.length === 0) {
    throw redirect("/ingen-tilgang");
  }

  await setupOpenApi(request);
  // TODO: Vi trenger en måte å velge orgrn på
  // F. eks med bedriftsvelger (eller hva det heter) som min-side-arbeidsgiver bruker
  const krav = await RefusjonskravService.getRefusjonskrav({
    requestBody: {
      orgnr: tilganger.roller.map((rolle) => rolle.organisasjonsnummer),
    },
  });

  return json({ krav });
}

function calculateRefusjon(krav: RefusjonskravDto[]): {
  historiske: RefusjonskravDto[];
  aktive: RefusjonskravDto[];
} {
  const aktive = krav.filter((k) => {
    const { periodeStart, periodeSlutt } = k;
    const startDate = new Date(periodeStart);
    const endDate = new Date(periodeSlutt);

    console.log(startDate, endDate);

    return Date.now() > endDate.getTime();
  });

  const historiske = krav.filter((k) => k.id);

  return { historiske, aktive };
}

export default function Refusjon() {
  const { krav } = useLoaderData<typeof loader>();
  const { historiske, aktive } = calculateRefusjon(krav);

  console.log(aktive);

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
