import { Button, Input } from "@chakra-ui/react";
import { useState } from "react";
import { Section } from "../components/Section";
import { syncVirksomhet } from "../core/api";

export function UpdateVirksomhet() {
  const [orgnr, setOrgnr] = useState<string>("");
  const [loading, setLoading] = useState(false);

  const syncVirksomheter = async (orgnrInput: string) => {
    setLoading(true);
    const nrs = orgnrInput.split(",").map((nr) => nr.trim());
    for (const nr of nrs) {
      await syncVirksomhet(nr);
    }
    setLoading(false);
  };

  return (
    <Section headerText="Update virksomhet" loadingText={"Laster"} isLoading={loading}>
      <Input
        placeholder="Organisasjonsnummer, gjerne kommaseparert hvis du trenger flere nr samtidig"
        value={orgnr}
        onChange={({ currentTarget }) => {
          setOrgnr(currentTarget.value);
        }}
      />
      <Button disabled={loading} onClick={() => syncVirksomheter(orgnr)}>
        Oppdater virksomhet
      </Button>
    </Section>
  );
}
