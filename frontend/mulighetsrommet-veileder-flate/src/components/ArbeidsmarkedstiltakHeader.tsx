import { InternalHeader } from "@navikt/ds-react";

interface Props {
  href: string;
}

export const ArbeidsmarkedstiltakHeader = (props: Props) => {
  return (
    <header>
      <InternalHeader>
        <InternalHeader.Title href={props.href}>NAV Arbeidsmarkedstiltak</InternalHeader.Title>
      </InternalHeader>
    </header>
  );
};
