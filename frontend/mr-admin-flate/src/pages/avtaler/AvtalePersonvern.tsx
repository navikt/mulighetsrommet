import { Alert, HGrid, List, VStack } from "@navikt/ds-react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { Laster } from "../../components/laster/Laster";
import styles from "../DetaljerInfo.module.scss";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { personopplysningToTekst } from "@/utils/Utils";

export function AvtalePersonvern() {
  const { data: avtale, isPending, error } = useAvtale();
  const { data: tiltakstype } = useTiltakstype(avtale?.tiltakstype.id);

  if (isPending) {
    return <Laster tekst="Laster avtale..." />;
  }

  if (error) {
    return <Alert variant="error">Klarte ikke hente avtaleinformasjon</Alert>;
  }

  if (avtale.personopplysninger.length === 0) {
    return <Alert variant="info">Personopplysninger som kan behandles er ikke bekreftet</Alert>;
  }

  return (
    <VStack gap="4" className={styles.info_container}>
      <HGrid columns={2}>
        <List size="small" as="ul" title="Opplysninger om brukeren som alltid kan/må behandles">
          {tiltakstype?.personopplysninger?.ALLTID.filter((p) =>
            avtale.personopplysninger.includes(p),
          ).map((p) => <List.Item key={p}>{personopplysningToTekst(p)}</List.Item>)}
        </List>
        <VStack justify="start">
          <List
            size="small"
            as="ul"
            title="Opplysninger om brukeren som ofte er nødvendig og relevant å behandle"
          >
            {tiltakstype?.personopplysninger?.OFTE.filter((p) =>
              avtale.personopplysninger.includes(p),
            ).map((p) => <List.Item key={p}>{personopplysningToTekst(p)}</List.Item>)}
          </List>
          <List
            size="small"
            as="ul"
            title="Opplysninger om brukeren som sjelden eller i helt spesielle tilfeller er nødvendig og relevant å behandle"
          >
            {tiltakstype?.personopplysninger?.SJELDEN.filter((p) =>
              avtale.personopplysninger.includes(p),
            ).map((p) => <List.Item key={p}>{personopplysningToTekst(p)}</List.Item>)}
          </List>
        </VStack>
      </HGrid>
    </VStack>
  );
}
