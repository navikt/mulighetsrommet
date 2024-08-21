import { ExternalLinkIcon } from "@navikt/aksel-icons";
import "@navikt/ds-css";
import { BodyShort, GuidePanel, Heading, Link } from "@navikt/ds-react";

interface Props {
  url: string;
  tekst: string;
}

export function TilbakemeldingsLenke({ url, tekst }: Props) {
  return (
    <GuidePanel>
      <Heading level="2" size="xsmall">
        Vi vil høre fra deg
      </Heading>
      <BodyShort spacing>Har du innspill til innholdet?</BodyShort>
      <Link target="_blank" rel="noopener noreferrer" href={url}>
        {tekst} <ExternalLinkIcon aria-label="Åpnes i ny fane-ikon" />
      </Link>
    </GuidePanel>
  );
}
