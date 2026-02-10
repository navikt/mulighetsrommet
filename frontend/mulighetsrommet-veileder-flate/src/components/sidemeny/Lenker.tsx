import { Lenke as LenkeComponent } from "@mr/frontend-common/components/lenke/Lenke";
import { GuidePanel, Heading, List, Box } from "@navikt/ds-react";
import { DokumentIkon } from "@/ikoner/DokumentIkon";

export interface Lenke {
  lenkenavn: string;
  lenke: string;
  apneINyFane: boolean;
  visKunForVeileder: boolean;
}

interface Props {
  lenker: Lenke[];
}

export function LenkeListe({ lenker }: Props) {
  if (lenker.length === 0) return null;

  return (
    <GuidePanel illustration={<DokumentIkon aria-label="Ikon for dokumenter" />}>
      <Heading level="4" size="small">
        Lenker
      </Heading>
      <Box marginBlock="space-16" asChild>
        <List data-aksel-migrated-v8 as="ul">
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
      </Box>
    </GuidePanel>
  );
}
