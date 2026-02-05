import { useAvtale } from "@/api/avtaler/useAvtale";
import { usePersonopplysninger } from "@/api/avtaler/usePersonopplysninger";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { PersonopplysningData } from "@tiltaksadministrasjon/api-client";
import { Alert, BodyShort, HelpText, HStack, List, VStack, Box } from "@navikt/ds-react";

export function AvtalePersonvern() {
  const { data: personopplysninger } = usePersonopplysninger();
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);

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

  const checkedPersonopplysninger = personopplysninger?.filter((p) =>
    avtale.personopplysninger.includes(p.personopplysning),
  );

  return (
    <VStack gap="space-4" className="p-6 bg-white max-w-360">
      <BodyShort>Følgende personopplysninger om deltager kan behandles i denne avtalen</BodyShort>
      {checkedPersonopplysninger && (
        <Box marginBlock="space-12" asChild><List data-aksel-migrated-v8 size="small" as="ul">
            {checkedPersonopplysninger.map((p: PersonopplysningData) => (
              <ListWithHelpText hjelpetekst={p.hjelpetekst} key={p.personopplysning}>
                {p.tittel}
              </ListWithHelpText>
            ))}
          </List></Box>
      )}
    </VStack>
  );
}

function ListWithHelpText({
  hjelpetekst,
  children,
}: {
  hjelpetekst: string | null;
  children: React.ReactNode;
}) {
  return (
    <List.Item>
      <HStack align="center" gap="space-4">
        {children}
        {hjelpetekst && <HelpText>{hjelpetekst}</HelpText>}
      </HStack>
    </List.Item>
  );
}
