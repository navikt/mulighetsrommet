import { Alert, HGrid, HStack, HelpText, List, VStack } from "@navikt/ds-react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { Laster } from "../../components/laster/Laster";
import styles from "../DetaljerInfo.module.scss";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";

export function AvtalePersonvern() {
  const { data: avtale, isPending, error } = useAvtale();
  const { data: tiltakstype } = useTiltakstype(avtale?.tiltakstype.id);

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

  const personopplysninger = tiltakstype?.personopplysninger?.filter((p) =>
    avtale.personopplysninger.includes(p.personopplysning),
  );

  return (
    <VStack gap="4" className={styles.info_container}>
      <HGrid columns={2}>
        {personopplysninger && (
          <List
            size="small"
            as="ul"
            title="Følgende personopplysninger om deltager kan behandles i denne avtalen"
          >
            {personopplysninger?.map((p) => (
              <ListWithHelpText hjelpetekst={p.hjelpetekst} key={p.personopplysning}>
                {p.beskrivelse}
              </ListWithHelpText>
            ))}
          </List>
        )}
      </HGrid>
    </VStack>
  );
}

function ListWithHelpText({
  hjelpetekst,
  children,
}: {
  hjelpetekst: String | null;
  children: React.ReactNode;
}) {
  return (
    <List.Item>
      <HStack align="end" gap="1">
        {children}
        {hjelpetekst && <HelpText>{hjelpetekst}</HelpText>}
      </HStack>
    </List.Item>
  );
}
