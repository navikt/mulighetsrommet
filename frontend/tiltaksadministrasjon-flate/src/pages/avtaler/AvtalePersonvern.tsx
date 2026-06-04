import { useAvtale } from "@/api/avtaler/useAvtale";
import { usePersonopplysninger } from "@/api/avtaler/usePersonopplysninger";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { AvtaleDto, Personopplysning } from "@tiltaksadministrasjon/api-client";
import { Alert, BodyShort, Box, HelpText, HStack, List, VStack } from "@navikt/ds-react";
import { AvtalePageLayout } from "@/pages/avtaler/AvtalePageLayout";

export function AvtalePersonvern() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);
  const { data: personopplysninger = [] } = usePersonopplysninger();

  return (
    <AvtalePageLayout avtale={avtale}>
      <AvtalePersonvernDetaljer avtale={avtale} personopplysninger={personopplysninger} />
    </AvtalePageLayout>
  );
}

interface Props {
  avtale: AvtaleDto;
  personopplysninger: Personopplysning[];
}

function AvtalePersonvernDetaljer({ avtale, personopplysninger }: Props) {
  if (!avtale.personvernBekreftet) {
    return (
      <Alert style={{ margin: "1rem" }} variant="info">
        Hvilke personopplysninger som kan behandles er ikke bekreftet
      </Alert>
    );
  }

  if (avtale.personopplysninger.length === 0) {
    return (
      <Alert style={{ margin: "1rem" }} variant="info">
        Ingen personopplysninger kan behandles i denne avtalen
      </Alert>
    );
  }

  const checkedPersonopplysninger = personopplysninger.filter((p) =>
    avtale.personopplysninger.map((ap) => ap.type).includes(p.type),
  );

  return (
    <VStack gap="space-4" className="p-6 bg-ax-bg-default max-w-360">
      <BodyShort>Følgende personopplysninger om deltager kan behandles i denne avtalen</BodyShort>
      <Box marginBlock="space-12" asChild>
        <List data-aksel-migrated-v8 size="small" as="ul">
          {checkedPersonopplysninger.map((p: Personopplysning) => (
            <ListWithHelpText helpText={p.helpText} key={p.type}>
              {p.title}
            </ListWithHelpText>
          ))}
        </List>
      </Box>
    </VStack>
  );
}

function ListWithHelpText({
  helpText,
  children,
}: {
  helpText: string | null;
  children: React.ReactNode;
}) {
  return (
    <List.Item>
      <HStack align="center" gap="space-4">
        {children}
        {helpText && <HelpText>{helpText}</HelpText>}
      </HStack>
    </List.Item>
  );
}
