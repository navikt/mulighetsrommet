import { InternalHeader } from "@navikt/ds-react";
import { ReactNode } from "react";

interface Props {
  href: string;
  children?: ReactNode;
}

export function ArbeidsmarkedstiltakHeader(props: Props) {
  return (
    <header>
      <InternalHeader>
        <InternalHeader.Title href={props.href}>NAV Arbeidsmarkedstiltak</InternalHeader.Title>
        {props.children}
      </InternalHeader>
    </header>
  );
}
