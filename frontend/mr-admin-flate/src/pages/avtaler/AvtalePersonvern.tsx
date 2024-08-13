import { Alert, HelpText, HStack, List, VStack } from "@navikt/ds-react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { Laster } from "@/components/laster/Laster";
import styles from "./AvtalePersonvern.module.scss";
import { usePersonopplysninger } from "@/api/avtaler/usePersonopplysninger";
import { PersonopplysningData } from "mulighetsrommet-api-client";

export function AvtalePersonvern() {
  const { data: avtale, isPending, error } = useAvtale();
  const { data: personopplysninger } = usePersonopplysninger();

  if (isPending) {
    return <Laster tekst="Laster avtale..." />;
  }

  if (error) {
    return (
      <Alert style={{ margin: "1rem" }} variant="error">
        Klarte ikke hente avtaleinformasjon
      </Alert>
    );
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
    <VStack gap="4" className={styles.info_container}>
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
