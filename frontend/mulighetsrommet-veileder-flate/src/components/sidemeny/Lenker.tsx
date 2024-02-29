import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Box, Heading, Link, List } from "@navikt/ds-react";
import { Lenke } from "mulighetsrommet-api-client";

interface Props {
  lenker?: Lenke[];
}

export function LenkeListe({ lenker }: Props) {
  if (!lenker || lenker.length === 0) return null;

  return (
    <Box background="bg-subtle" padding="5">
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
    </Box>
  );
}
