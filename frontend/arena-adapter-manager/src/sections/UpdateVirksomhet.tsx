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
          setOrgnr(currentTarget.value.trim());
        }}
        type="number"
      />
      <Button
        onClick={async () => {
          if (orgnr.length === 9) {
            for (const nr of orgnr.split(",")) {
              await syncVirksomhet(nr);
            }
          } else {
            alert("Orgnr må være 9 siffer");
          }
        }}
      >
        Oppdater virksomhet
      </Button>
    </Section>
  );
}

export default UpdateVirksomhet;
