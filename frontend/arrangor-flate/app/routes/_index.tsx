import { BodyShort } from "@navikt/ds-react";
import type { MetaFunction } from "@remix-run/node";
import { Link } from "@remix-run/react";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export default function Refusjon() {
  return (
    <div className="font-sans p-4">
      <h1 className="text-3xl">Arrangørflate</h1>
      <BodyShort>Dette er en oversiktsside for arrangører som skal søke om refusjon.</BodyShort>
      <Link to="/deltakerliste/1">Gå til refusjonssøknad</Link>
    </div>
  );
}
