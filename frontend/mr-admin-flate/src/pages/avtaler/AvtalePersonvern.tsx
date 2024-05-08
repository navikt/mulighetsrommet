import { Alert, HGrid, List, VStack } from "@navikt/ds-react";
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

  if (avtale.personopplysninger.length === 0) {
    return (
      <Alert style={{ margin: "1rem" }} variant="info">
        Hvilke personopplysninger som kan behandles er ikke bekreftet
      </Alert>
    );
  }

  const alltid =
    tiltakstype?.personopplysninger?.ALLTID.filter((p) =>
      avtale.personopplysninger.includes(p.personopplysning),
    ) ?? [];
  const ofte =
    tiltakstype?.personopplysninger?.OFTE.filter((p) =>
      avtale.personopplysninger.includes(p.personopplysning),
    ) ?? [];
  const sjelden =
    tiltakstype?.personopplysninger?.SJELDEN.filter((p) =>
      avtale.personopplysninger.includes(p.personopplysning),
    ) ?? [];

  return (
    <VStack gap="4" className={styles.info_container}>
      <HGrid columns={2}>
        {alltid.length > 0 && (
          <List size="small" as="ul" title="Opplysninger om brukeren som alltid kan/må behandles">
            {alltid.map((p) => (
              <List.Item key={p.personopplysning}>{p.beskrivelse}</List.Item>
            ))}
          </List>
        )}
        <VStack justify="start">
          {ofte.length > 0 && (
            <List
              size="small"
              as="ul"
              title="Opplysninger om brukeren som ofte er nødvendig og relevant å behandle"
            >
              {ofte.map((p) => (
                <List.Item key={p.personopplysning}>{p.beskrivelse}</List.Item>
              ))}
            </List>
          )}
          {sjelden.length > 0 && (
            <List
              size="small"
              as="ul"
              title="Opplysninger om brukeren som sjelden eller i helt spesielle tilfeller er nødvendig og relevant å behandle"
            >
              {sjelden.map((p) => (
                <List.Item key={p.personopplysning}>{p.beskrivelse}</List.Item>
              ))}
            </List>
          )}
        </VStack>
      </HGrid>
    </VStack>
  );
}
