import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { List, VStack } from "@navikt/ds-react";
import { Lenke } from "mulighetsrommet-api-client";

interface Props {
  lenker: Lenke[];
}

export function Lenkeliste({ lenker }: Props) {
  return (
    <List as="ul">
      {lenker.map((lenke, index) => (
        <List.Item key={index}>
          <VStack>
            <a
              href={lenke.lenke}
              target={lenke.apneINyFane ? "_blank" : "_self"}
              rel={lenke.apneINyFane ? "noopener noreferrer" : undefined}
            >
              {lenke.lenkenavn} ({lenke.lenke}) {lenke.apneINyFane ? <ExternalLinkIcon /> : null}
            </a>
            {lenke.visKunForVeileder ? <small>Vises kun i Modia</small> : null}
          </VStack>
        </List.Item>
      ))}
    </List>
  );
}
