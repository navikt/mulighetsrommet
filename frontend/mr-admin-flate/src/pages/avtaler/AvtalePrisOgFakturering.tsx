import { Alert, HStack, Select, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { InferredAvtaleSchema } from "@/components/redaksjoneltInnhold/AvtaleSchema";
import { EmbeddedTiltakstype, Prismodell, Tiltakskode } from "@mr/api-client";
import { DetaljerContainer } from "@/pages/DetaljerContainer";
import { SkjemaDetaljerContainer } from "@/components/skjema/SkjemaDetaljerContainer";
import { Metadata } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { FormGroup } from "@/components/skjema/FormGroup";
import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";
import { DateInput } from "@/components/skjema/DateInput";
import { BorderedContainer } from "@/components/skjema/BorderedContainer";

interface Props {
  tiltakstype?: EmbeddedTiltakstype;
}

export function AvtalePrisOgFakturering({ tiltakstype }: Props) {
  const { watch } = useFormContext<InferredAvtaleSchema>();

  if (!tiltakstype) {
    return (
      <SkjemaDetaljerContainer>
        <Alert variant="info">Tiltakstype må velges før prismodell kan velges.</Alert>
      </SkjemaDetaljerContainer>
    );
  }

  const prismodell = watch("prismodell");

  return (
    <DetaljerContainer>
      <FormGroup>
        <Metadata header={avtaletekster.tiltakstypeLabel} verdi={tiltakstype.navn} />

        <SelectPrismodell />

        {prismodell === Prismodell.FORHANDSGODKJENT && (
          <ForhandsgodkjentAvtalePrismodell tiltakstype={tiltakstype.tiltakskode} />
        )}
      </FormGroup>
    </DetaljerContainer>
  );
}

function SelectPrismodell() {
  const {
    register,
    formState: { errors },
  } = useFormContext<InferredAvtaleSchema>();

  return (
    <Select
      label="Prismodell"
      size="small"
      error={errors.prismodell?.message}
      {...register("prismodell")}
    >
      <option value={Prismodell.FORHANDSGODKJENT}>
        {avtaletekster.prismodell.beskrivelse(Prismodell.FORHANDSGODKJENT)}
      </option>
      <option value={Prismodell.FRI}>{avtaletekster.prismodell.beskrivelse(Prismodell.FRI)}</option>
    </Select>
  );
}

interface ForhandsgodkjentAvtalePrismodellProps {
  tiltakstype: Tiltakskode;
}

function ForhandsgodkjentAvtalePrismodell({ tiltakstype }: ForhandsgodkjentAvtalePrismodellProps) {
  const { data: satser } = useForhandsgodkjenteSatser(tiltakstype);

  if (!satser) return null;

  return (
    <VStack gap="4">
      {satser.map((sats) => (
        <BorderedContainer key={sats.periodeStart}>
          <HStack key={sats.periodeStart} gap="4">
            <Select readOnly label="Valuta" size="small">
              <option value={undefined}>{sats.valuta}</option>
            </Select>

            <TextField
              readOnly
              label={avtaletekster.prismodell.pris.label}
              size="small"
              value={sats.pris}
            />

            <DateInput
              label={avtaletekster.prismodell.periodeStart.label}
              readOnly={true}
              onChange={() => {}}
              fromDate={new Date(sats.periodeStart)}
              toDate={new Date(sats.periodeSlutt)}
              format={"iso-string"}
              size="small"
              value={sats.periodeStart}
            />

            <DateInput
              label={avtaletekster.prismodell.periodeSlutt.label}
              readOnly={true}
              onChange={() => {}}
              fromDate={new Date(sats.periodeStart)}
              toDate={new Date(sats.periodeSlutt)}
              format={"iso-string"}
              size="small"
              value={sats.periodeSlutt}
            />
          </HStack>
        </BorderedContainer>
      ))}
    </VStack>
  );
}
