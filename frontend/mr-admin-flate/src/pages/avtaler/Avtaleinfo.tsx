import { Alert } from "@navikt/ds-react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Laster } from "../../components/Laster";

export function Avtaleinfo() {
  const { data: avtale, isLoading, error } = useAvtale();

  if (!avtale && isLoading) {
    return <Laster tekst="Laster avtaleinformasjon..." />;
  }

  if (error) {
    return <Alert variant="error">Klarte ikke hente avtaleinformasjon</Alert>;
  }

  if (!avtale) {
    return <Alert variant="warning">Fant ingen avtale</Alert>;
  }

  return (
    <div>
      <pre>{JSON.stringify(avtale, null, 2)}</pre>
      {/**TODO Her kommer detaljer om avtalen */}
    </div>
  );
}
