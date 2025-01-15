import { Lenke } from "@mr/api-client";
import { Lenke as LenkeComponent } from "@mr/frontend-common/components/lenke/Lenke";
import { GuidePanel, Heading, List } from "@navikt/ds-react";
import { DokumentIkon } from "../../ikoner/DokumentIkon";

interface Props {
  lenker?: Lenke[];
}

export function LenkeListe({ lenker }: Props) {
  if (!lenker || lenker.length === 0) return null;

  return (
    <GuidePanel illustration={<DokumentIkon aria-label="Ikon for dokumenter" />}>
      <Heading level="4" size="small">
        Lenker
      </Heading>
      <List as="ul">
        {lenker.map(({ lenke, apneINyFane, lenkenavn }, index) => (
          <List.Item key={index}>
            <LenkeComponent
              to={lenke}
              target={apneINyFane ? "_blank" : undefined}
              rel={apneINyFane ? "noopener noreferrer" : undefined}
              isExternal={apneINyFane}
            >
              {lenkenavn}
            </LenkeComponent>
          </List.Item>
        ))}
      </List>
    </GuidePanel>
  );
}
