import { Button, Input } from "@chakra-ui/react";
import { useState } from "react";
import { Section } from "../components/Section";
import { syncVirksomhet } from "../core/api";

function UpdateVirksomhet() {
  const [orgnr, setOrgnr] = useState<string>("");

  return (
    <Section
      headerText="Update virksomhet"
      loadingText={"Laster"}
      isLoading={false}
    >
      <Input
        placeholder="Organisasjonsnummer, gjerne kommaseparert hvis du trenger flere nr samtidig"
        value={orgnr}
        onChange={({ currentTarget }) => {
          setOrgnr(currentTarget.value);
        }}
      />
      <Button
        onClick={async () => {
          for (const nr of orgnr.split(",")) {
            if (nr.trim().length !== 9) {
              alert(`${nr} er ikke 9 siffer. Orgnr er 9 siffer.`);
              return;
            }
            await syncVirksomhet(nr.trim());
          }
        }}
      >
        Oppdater virksomhet
      </Button>
    </Section>
  );
}

export default UpdateVirksomhet;
