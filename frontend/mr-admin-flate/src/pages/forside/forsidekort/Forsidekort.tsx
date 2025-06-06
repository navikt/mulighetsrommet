import { BodyShort, Heading } from "@navikt/ds-react";
import { Link } from "react-router";
import { kebabCase } from "../../../../../frontend-common/utils/TestUtils";
import { ReactNode } from "react";

export interface ForsideKortProps {
  navn: string;
  ikon: ReactNode;
  url: string;
  apneINyTab?: boolean;
  tekst?: string;
}

export function Forsidekort({ navn, ikon, url, tekst, apneINyTab = false }: ForsideKortProps) {
  return (
    <Link
      key={url}
      className="text-text-default w-[370px] h-[350px] bg-white items-center grid grid-rows-[auto,1fr,1fr] p-12 text-center shadow-md hover:shadow-lg transition-all duration-150 ease-in-out no-underline text-black rounded"
      to={url}
      {...(apneINyTab ? { target: "_blank", rel: "noopener noreferrer" } : {})}
      data-testid={`forsidekort-${kebabCase(navn)}`}
    >
      <span className="flex justify-center items-center w-[100px] h-[100px] rounded-full mx-auto mb-2">
        <div className="[&>svg]:w-16 [&>svg]:h-16">{ikon}</div>
      </span>
      <Heading size="medium" level="3">
        {navn}
      </Heading>
      {tekst ? <BodyShort className="text-gray-600">{tekst}</BodyShort> : null}
    </Link>
  );
}
