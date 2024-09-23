import { RefusjonskravDto, RefusjonskravService } from "@mr/api-client";
import { Heading, VStack } from "@navikt/ds-react";
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
  const krav = await RefusjonskravService.getRefusjonskrav({ orgnr: "123456789" });

  return json({ krav });
}

export default function Refusjon() {
  const { krav } = useLoaderData<typeof loader>();
  const historiske: RefusjonskravDto[] = [];
  const aktive = krav;

  return (
    <>
      <PageHeader title="Tilgjengelige refusjonskrav" />
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
