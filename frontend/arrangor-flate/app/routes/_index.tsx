import { RefusjonKravAft, RefusjonskravService } from "@mr/api-client";
import { Heading, VStack } from "@navikt/ds-react";
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
  const historiske: RefusjonKravAft[] = [];
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
