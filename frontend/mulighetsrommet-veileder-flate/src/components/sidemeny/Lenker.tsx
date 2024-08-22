import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { GuidePanel, Heading, Link, List } from "@navikt/ds-react";
import { Lenke } from "@mr/api-client";
import { DokumentIkon } from "../../ikoner/DokumentIkon";

interface Props {
  lenker?: Lenke[];
}

export function LenkeListe({ lenker }: Props) {
  if (!lenker || lenker.length === 0) return null;

  return (
    <GuidePanel poster illustration={<DokumentIkon aria-label="Ikon for dokumenter" />}>
      <Heading level="4" size="small">
        Lenker
      </Heading>
      <List as="ul">
        {lenker.map(({ lenke, apneINyFane, lenkenavn }, index) => (
          <List.Item key={index}>
            <Link
              href={lenke}
              target={apneINyFane ? "_blank" : undefined}
              rel={apneINyFane ? "noopener noreferrer" : undefined}
            >
              {lenkenavn} {apneINyFane ? <ExternalLinkIcon /> : null}
            </Link>
          </List.Item>
        ))}
      </List>
    </GuidePanel>
  );
}
