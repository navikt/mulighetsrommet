import { BodyShort, GuidePanel, Heading } from "@navikt/ds-react";
import { Lenke } from "../lenke/Lenke";

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
      <Lenke target="_blank" rel="noopener noreferrer" to={url} isExternal>
        {tekst}
      </Lenke>
    </GuidePanel>
  );
}
