import { Alert, BodyShort } from "@navikt/ds-react";
import { Route, Routes } from "react-router-dom";
import { useHentAnsatt } from "./api/administrator/useHentAdministrator";
import { AutentisertApp } from "./AutentisertApp";
import { Laster } from "./components/Laster";
import { Forside } from "./Forside";

export function App() {
  const optionalAnsatt = useHentAnsatt();

  if (optionalAnsatt.error) {
    return (
      <main>
        <Alert variant="error">
          <BodyShort>
            Vi klarte ikke hente brukerinformasjon. Prøv igjen senere.
          </BodyShort>
          <pre>{JSON.stringify(optionalAnsatt?.error, null, 2)}</pre>
        </Alert>
      </main>
    );
  }

  if (optionalAnsatt.isFetching || !optionalAnsatt.data) {
    return (
      <main>
        <Laster tekst="Laster..." size="xlarge" />
      </main>
    );
  }

  return (
    <Routes>
      <Route index element={<Forside />} />
      <Route path="/*" element={<AutentisertApp />} />
    </Routes>
  );
}
