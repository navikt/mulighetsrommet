import type { LoaderFunction, MetaFunction } from "@remix-run/node";
import { PageHeader } from "../components/PageHeader";
import { Refusjonskrav, RefusjonskravService, RefusjonskravStatus } from "@mr/api-client";
import { useLoaderData } from "@remix-run/react";
import { RefusjonskravTable } from "~/components/refusjonskrav/RefusjonskravTable";
import { Heading, VStack } from "@navikt/ds-react";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

type LoaderData = {
  krav: Refusjonskrav[];
};

export const loader: LoaderFunction = async ({ params }): Promise<LoaderData> => {
  const krav = await RefusjonskravService.getRefusjonskrav({ orgnr: "123456789" });

  return { krav };
};

export default function Refusjon() {
  const { krav } = useLoaderData<LoaderData>();
  const historiske = krav.filter(k => k.status === RefusjonskravStatus.ATTESTERT);
  const aktive = krav.filter(k => k.status !== RefusjonskravStatus.ATTESTERT);

  return (
    <>
      <PageHeader title="Tilgjengelige refusjonskrav" />
      <VStack align="center" gap="4">
        <RefusjonskravTable krav={aktive} />
        <Heading size="small" as="div">Historiske refusjonskrav</Heading>
        <RefusjonskravTable krav={historiske} />
      </VStack>
    </>
  );
}
