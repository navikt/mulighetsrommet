import { useAvtalteSatser } from "@/api/tilsagn/useAvtalteSatser";
import { Avtaletype } from "@mr/api-client";
import {
  BodyShort,
  Box,
  Heading,
  HStack,
  Label,
  Select,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useState } from "react";
import { avtaleLoader } from "@/pages/avtaler/avtaleLoader";
import { useLoaderData } from "react-router";
import { DateInput } from "@/components/skjema/DateInput";
import { SkjemaInputContainer } from "@/components/skjema/SkjemaInputContainer";
import { SkjemaDetaljerContainer } from "@/components/skjema/SkjemaDetaljerContainer";

type Prismodell = "FORHANDSGODKJENT" | "FRI";

export function AvtalePrisOgFakturering() {
  const { avtale } = useLoaderData<typeof avtaleLoader>();

  const [prismodell, setPrismodell] = useState<Prismodell>(
    avtale.avtaletype === Avtaletype.FORHAANDSGODKJENT ? "FORHANDSGODKJENT" : "FRI",
  );

  return (
    <SkjemaDetaljerContainer>
      <SkjemaInputContainer>
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
              onChange={(e) => setPrismodell(e.target.value as Prismodell)}
            >
              <option value={"FORHANDSGODKJENT"}>Standardpris per tiltaksplass per måned</option>
              <option value={"FRI"}>Fri prismodell</option>
            </Select>
            {prismodell === "FORHANDSGODKJENT" && (
              <ForhandsgodkjentAvtalePrismodell avtaleId={avtale.id} />
            )}
          </VStack>
        </Box>
      </SkjemaInputContainer>
    </SkjemaDetaljerContainer>
  );
}

interface ForhandsgodkjentAvtalePrismodellProps {
  avtaleId: string;
}

function ForhandsgodkjentAvtalePrismodell({ avtaleId }: ForhandsgodkjentAvtalePrismodellProps) {
  const { data: satser } = useAvtalteSatser(avtaleId);

  if (!satser) return null;

  return (
    <VStack gap="4">
      {satser.map((sats) => (
        <HStack key={sats.periodeStart} gap="4">
          <Select readOnly label="Valuta" size="small">
            <option value={undefined}>NOK</option>
          </Select>
          <TextField readOnly label="Pris" size="small" value={sats.pris} />
          <DateInput
            label="Gjelder fra"
            readOnly={true}
            onChange={() => {}}
            fromDate={new Date(sats.periodeStart)}
            toDate={new Date(sats.periodeSlutt)}
            format={"iso-string"}
            value={sats.periodeStart}
          />
          <DateInput
            label="Gjelder til"
            readOnly={true}
            onChange={() => {}}
            fromDate={new Date(sats.periodeStart)}
            toDate={new Date(sats.periodeSlutt)}
            format={"iso-string"}
            value={sats.periodeSlutt}
          />
        </HStack>
      ))}
    </VStack>
  );
}
