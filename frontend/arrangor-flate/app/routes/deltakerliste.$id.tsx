import { BodyShort } from "@navikt/ds-react";
import type { LoaderFunction, MetaFunction } from "@remix-run/node";
import { useLoaderData } from "@remix-run/react";
import { PageHeader } from "../components/PageHeader";

export const meta: MetaFunction = () => {
  return [{ title: "Refusjon" }, { name: "description", content: "Refusjonsdetaljer" }];
};

type LoaderData = {
  title: string;
};

export const loader: LoaderFunction = async ({ params }): Promise<LoaderData> => {
  return {
    title: `Refusjon for ${params.id}`,
  };
};

export default function RefusjonDeltakerlister() {
  const { title } = useLoaderData<LoaderData>();
  return (
    <div className="font-sans p-4">
      <PageHeader title={title} tilbakeLenke={{ navn: "Tilbake til refusjonsliste", url: "/" }} />
      <BodyShort>Her kan vi vise detaljer</BodyShort>
    </div>
  );
}
