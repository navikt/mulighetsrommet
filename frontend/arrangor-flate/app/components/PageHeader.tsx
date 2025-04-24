import { Heading } from "@navikt/ds-react";
import { LinkWithTabState } from "./LinkWithTabState";

interface Props {
  title: string;
  tilbakeLenke?: {
    navn: string;
    url: string;
  };
}

export function PageHeader({ title, tilbakeLenke }: Props) {
  return (
    <div className="flex flex-col relative">
      <Heading level="1" size="large" className="mb-3" data-testid="header">
        {title}
      </Heading>
      {tilbakeLenke ? (
        <LinkWithTabState className="mb-5 inline hover:underline" to={tilbakeLenke.url}>
          {tilbakeLenke.navn}
        </LinkWithTabState>
      ) : null}
    </div>
  );
}
