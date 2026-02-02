import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { Heading, Link } from "@navikt/ds-react";
import { Link as ReactRouterLink, useSearchParams } from "react-router";

interface Props {
  title: string;
  tilbakeLenke?: {
    navn: string;
    url: string;
  };
}

export function PageHeading({ title, tilbakeLenke }: Props) {
  const [searchParams] = useSearchParams();
  const currentTab = searchParams.get("forside-tab");
  const linkTo = currentTab
    ? `${tilbakeLenke?.url}?${currentTab}=${encodeURIComponent(currentTab)}`
    : (tilbakeLenke?.url ?? "");
  return (
    <div className="flex flex-col relative gap-1">
      <Heading level="2" size="large" className="mb-3" id="innsending-table-header">
        {title}
      </Heading>
      {tilbakeLenke ? (
        <Link as={ReactRouterLink} to={linkTo}>
          <ChevronLeftIcon />
          {tilbakeLenke.navn}
        </Link>
      ) : null}
    </div>
  );
}
