import { Heading } from "@navikt/ds-react";
import { LinkWithTabState } from "./LinkWithTabState";

interface Props {
  title: string;
  tilbakeLenke?: {
    navn: string;
    url: string;
  };
}

export function PageHeading({ title, tilbakeLenke }: Props) {
  return (
    <div className="flex flex-col relative gap-1">
      <Heading level="2" size="large" className="mb-3" id="innsending-table-header">
        {title}
      </Heading>
      {tilbakeLenke ? (
        <LinkWithTabState className="inline hover:underline" to={tilbakeLenke.url}>
          {tilbakeLenke.navn}
        </LinkWithTabState>
      ) : null}
    </div>
  );
}
