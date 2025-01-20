import { BodyShort, Heading } from "@navikt/ds-react";
import { Link } from "react-router";
import { kebabCase } from "@mr/frontend-common/utils/TestUtils";
import { logEvent } from "@/logging/amplitude";
import { ReactNode } from "react";

interface ForsidekortProps {
  navn: string;
  ikon: ReactNode;
  url: string;
  apneINyTab?: boolean;
  tekst?: string;
}

function loggKlikkPaKort(forsidekort: string) {
  logEvent({
    name: "tiltaksadministrasjon.klikk-forsidekort",
    data: {
      forsidekort,
    },
  });
}

export function Forsidekort({ navn, ikon, url, tekst, apneINyTab = false }: ForsidekortProps) {
  return (
    <Link
      key={url}
      onClick={() => loggKlikkPaKort(navn)}
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
