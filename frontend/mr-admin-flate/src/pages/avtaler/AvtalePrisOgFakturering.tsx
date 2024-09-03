import { useAvtale } from "@/api/avtaler/useAvtale";
import { useAFTSatser } from "@/api/tilsagn/useAFTSatser";
import { Laster } from "@/components/laster/Laster";
import { DetaljerContainer } from "@/pages/DetaljerContainer";
import { DetaljerInfoContainer } from "@/pages/DetaljerInfoContainer";
import { AFTSats, Tiltakskode } from "@mr/api-client";
import { Alert, BodyShort, Box, Heading, HStack, Label, Select, VStack } from "@navikt/ds-react";
import { useState } from "react";

export function AvtalePrisOgFakturering() {
  const { data: avtale, isPending, error } = useAvtale();
  const [prismodell, setPrismodell] = useState<string>(
    avtale?.tiltakstype.tiltakskode === Tiltakskode.ARBEIDSFORBEREDENDE_TRENING ? "AFT" : "FRI",
  );

  if (isPending) {
    return <Laster tekst="Laster avtale..." />;
  }

  if (error) {
    return <Alert variant="error">Klarte ikke hente avtaleinformasjon</Alert>;
  }

  return (
    <DetaljerContainer>
      <DetaljerInfoContainer>
        <Box
          background="surface-subtle"
          borderColor="border-subtle"
          padding="4"
          borderWidth="1"
          borderRadius="medium"
        >
          <VStack gap="4">
            <Heading size="medium">Prisinfo</Heading>
            <HStack gap="2">
              <Label size="small">Tiltakstype:</Label>
              <BodyShort size="small">{avtale.tiltakstype.navn}</BodyShort>
            </HStack>
            <Select
              readOnly
              label="Prismodell"
              size="small"
              value={prismodell}
              onChange={(e) => setPrismodell(e.target.value)}
            >
              <option value={"AFT"}>Standardpris per tiltaksplass per måned</option>
              <option value={"FRI"}>Fri prismodell</option>
            </Select>
            {prismodell === "AFT" && <AFTPrisOgFakturering />}
          </VStack>
        </Box>
      </DetaljerInfoContainer>
    </DetaljerContainer>
  );
}

function AFTPrisOgFakturering() {
  const { data: satser } = useAFTSatser();

  function findSats(): number | undefined {
    return satser?.sort(
      (a: AFTSats, b: AFTSats) => new Date(b.startDato).getTime() - new Date(a.startDato).getTime(),
    )[0]?.belop;
  }

  return (
    <HStack gap="4">
      <Select readOnly label="Valuta" size="small">
        <option value={undefined}>NOK</option>
      </Select>
      <Select readOnly label="Pris" size="small">
        <option value={undefined}>{`${findSats()} kr`}</option>
      </Select>
    </HStack>
  );
}
