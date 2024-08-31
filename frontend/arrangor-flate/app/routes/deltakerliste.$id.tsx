import { BodyShort } from "@navikt/ds-react";
import type { LoaderFunction, MetaFunction } from "@remix-run/node";
import { Link, useLoaderData } from "@remix-run/react";

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
      <h1 className="text-3xl">{title}</h1>
      <BodyShort>Her kan vi vise detaljer</BodyShort>
      <Link to="/refusjon">Gå tilbake</Link>
    </div>
  );
}
