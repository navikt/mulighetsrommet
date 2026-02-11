import { ChevronRightIcon } from "@navikt/aksel-icons";
import { HStack, Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router";

type Id = string;

export interface Brodsmule {
  tittel: string;
  lenke?:
    | "/"
    | "/tiltakstyper"
    | `/tiltakstyper/${Id}`
    | "/avtaler"
    | `/avtaler/${Id}`
    | "/gjennomforinger"
    | `/gjennomforinger/${Id}`
    | "/arrangorer"
    | `/arrangorer/${Id}`;
}

interface Props {
  brodsmuler: (Brodsmule | undefined)[];
}

export function Brodsmuler({ brodsmuler }: Props) {
  const filtrerteBrodsmuler = brodsmuler.filter((b) => b !== undefined);

  return (
    <nav aria-label="Brødsmulesti" className={"bg-ax-bg-default p-3"}>
      <HStack as="ol" gap="space-4">
        {filtrerteBrodsmuler.map((item, index) => {
          return (
            <li key={index}>
              {item.lenke ? (
                <Link as={ReactRouterLink} to={item.lenke}>
                  {item.tittel}
                  <ChevronRightIcon aria-hidden="true" />
                </Link>
              ) : (
                <span aria-current="page">{item.tittel}</span>
              )}
            </li>
          );
        })}
      </HStack>
    </nav>
  );
}
