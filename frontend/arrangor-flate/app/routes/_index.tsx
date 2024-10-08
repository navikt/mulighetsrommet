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
  // TODO: Vi trenger en måte å velge orgrn på
  // F. eks med bedriftsvelger (eller hva det heter) som min-side-arbeidsgiver bruker
  const krav = await RefusjonskravService.getRefusjonskrav({
    requestBody: {
      orgnr: tilganger.roller.map((rolle) => rolle.organisasjonsnummer),
    },
  });

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
