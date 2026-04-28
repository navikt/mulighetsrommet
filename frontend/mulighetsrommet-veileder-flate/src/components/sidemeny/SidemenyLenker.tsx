import { Lenke as LenkeComponent } from "@mr/frontend-common/components/lenke/Lenke";
import { Box, GuidePanel, Heading, List } from "@navikt/ds-react";
import { DokumentIkon } from "@/ikoner/DokumentIkon";
import { VeilederflateTiltak } from "@api-client";

interface Props {
  tiltak: VeilederflateTiltak;
  skjulKunForVeileder?: boolean;
}

export function SidemenyLenker({ tiltak, skjulKunForVeileder }: Props) {
  const lenker = [
    ...(tiltak.tiltakstype.faneinnhold?.lenker ?? []),
    ...(tiltak.faneinnhold?.lenker ?? []),
  ].filter((link) => !skjulKunForVeileder || !link.visKunForVeileder);
  if (lenker.length === 0) {
    return null;
  }

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
