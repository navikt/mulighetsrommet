import { RefusjonskravService, RefusjonskravStatus } from "@mr/api-client";
import { Heading, VStack } from "@navikt/ds-react";
import type { LoaderFunctionArgs, MetaFunction } from "@remix-run/node";
import { json, redirect } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
import { RefusjonskravTable } from "~/components/refusjonskrav/RefusjonskravTable";
import { getTilganger } from "../auth/altinn.server";
import { oboExchange } from "../auth/auth.server";
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

  await oboExchange(request);
  const krav = await RefusjonskravService.getRefusjonskrav({ orgnr: "123456789" });

  return json({ krav, tilganger });
}

export default function Refusjon() {
  const { krav, tilganger } = useLoaderData<typeof loader>();
  const historiske = krav.filter((k) => k.status === RefusjonskravStatus.ATTESTERT);
  const aktive = krav.filter((k) => k.status !== RefusjonskravStatus.ATTESTERT);

  return (
    <>
      <PageHeader title="Tilgjengelige refusjonskrav" />
      <pre>{JSON.stringify(tilganger, null, 2)}</pre>
      <VStack align="center" gap="4">
        <RefusjonskravTable krav={aktive} />
        <Heading size="small" as="div">
          Historiske refusjonskrav
        </Heading>
        <RefusjonskravTable krav={historiske} />
      </VStack>
    </>
  );
}
