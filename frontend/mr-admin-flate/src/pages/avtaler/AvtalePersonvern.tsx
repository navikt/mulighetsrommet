import { usePersonopplysninger } from "@/api/avtaler/usePersonopplysninger";
import { PersonopplysningData } from "@mr/api-client-v2";
import { Alert, HelpText, HStack, List, VStack } from "@navikt/ds-react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Laster } from "../../components/laster/Laster";

export function AvtalePersonvern() {
  const { data: avtale } = useAvtale();
  const { data: personopplysninger } = usePersonopplysninger();

  if (!avtale) {
    return <Laster tekst="Laster avtale..." />;
  }

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
    <VStack gap="4" className="p-[1.5rem] bg-white mw-[1440px]">
      {checkedPersonopplysninger && (
        <List
          size="small"
          as="ul"
          title="Følgende personopplysninger om deltager kan behandles i denne avtalen"
        >
          {checkedPersonopplysninger?.map((p: PersonopplysningData) => (
            <ListWithHelpText hjelpetekst={p.hjelpetekst} key={p.personopplysning}>
              {p.tittel}
            </ListWithHelpText>
          ))}
        </List>
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
      <HStack align="center" gap="1">
        {children}
        {hjelpetekst && <HelpText>{hjelpetekst}</HelpText>}
      </HStack>
    </List.Item>
  );
}
