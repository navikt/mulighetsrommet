import {
  Alert,
  Box,
  Button,
  HGrid,
  HStack,
  Select,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { EmbeddedTiltakstype, Prismodell, PrismodellDto, Tiltakskode } from "@mr/api-client-v2";
import { Metadata } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { FormGroup } from "@/components/skjema/FormGroup";
import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";
import { DateInput } from "@/components/skjema/DateInput";
import { useEffect } from "react";
import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { usePrismodeller } from "@/api/tilsagn/usePrismodeller";
import { AvtaleFormValues } from "@/schemas/avtale";

interface AvtalPrisOgFaktureringProps {
  tiltakstype?: EmbeddedTiltakstype;
}

export function AvtalePrisOgFaktureringForm({ tiltakstype }: AvtalPrisOgFaktureringProps) {
  if (!tiltakstype) {
    return <Alert variant="info">Tiltakstype må velges før prismodell kan velges.</Alert>;
  }

  return <AvtalePrisOgFakturering tiltakstype={tiltakstype} />;
}

function AvtalePrisOgFakturering({ tiltakstype }: Required<AvtalPrisOgFaktureringProps>) {
  const { data: prismodeller } = usePrismodeller(tiltakstype.tiltakskode);

  return (
    <HGrid columns={2} align="start">
      <FormGroup>
        <Metadata header={avtaletekster.tiltakstypeLabel} verdi={tiltakstype.navn} />
        <SelectPrismodell prismodeller={prismodeller} />
        <PrismodellDetaljerForm tiltakstype={tiltakstype} />
      </FormGroup>
    </HGrid>
  );
}

interface SelectPrismodellProps {
  prismodeller: PrismodellDto[];
}

function SelectPrismodell({ prismodeller }: SelectPrismodellProps) {
  const fieldName = "prismodell";
  const {
    register,
    formState: { errors },
    setValue,
  } = useFormContext<AvtaleFormValues>();

  const preselectPrismodell: false | Prismodell = prismodeller.length === 1 && prismodeller[0].type;

  useEffect(() => {
    if (preselectPrismodell) {
      setValue(fieldName, preselectPrismodell);
    }
  }, [setValue, preselectPrismodell]);

  return (
    <Select
      label={avtaletekster.prismodell.label}
      size="small"
      error={errors.prismodell?.message}
      {...register(fieldName)}
    >
      {prismodeller.map(({ type, beskrivelse }) => (
        <option key={type} value={type}>
          {beskrivelse}
        </option>
      ))}
    </Select>
  );
}

function PrismodellDetaljerForm({ tiltakstype }: Required<AvtalPrisOgFaktureringProps>) {
  const { watch } = useFormContext<AvtaleFormValues>();

  const prismodell = watch("prismodell");

  switch (prismodell) {
    case Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
      return <ForhandsgodkjenteSatser tiltakstype={tiltakstype.tiltakskode} />;
    case Prismodell.AVTALT_PRIS_PER_MANEDSVERK:
    case Prismodell.AVTALT_PRIS_PER_UKESVERK:
      return <AvtalteSatser />;
    case Prismodell.ANNEN_AVTALT_PRIS:
      return <Prisbetingelser />;
  }
}

interface ForhandsgodkjentAvtalePrismodellProps {
  tiltakstype: Tiltakskode;
}

function ForhandsgodkjenteSatser({ tiltakstype }: ForhandsgodkjentAvtalePrismodellProps) {
  const { data: satser } = useForhandsgodkjenteSatser(tiltakstype);

  return (
    <VStack gap="4">
      {satser.map((sats) => (
        <Box
          padding="4"
          borderColor="border-subtle"
          borderRadius="large"
          borderWidth="1"
          key={sats.periodeStart}
        >
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
        </Box>
      ))}
    </VStack>
  );
}

function AvtalteSatser() {
  const {
    control,
    register,
    watch,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  const { fields, append, remove } = useFieldArray({
    name: "satser",
    control,
  });

  const startDato = watch("startDato");
  const sluttDato = watch("sluttDato");

  return (
    <VStack gap="4">
      {fields.map((field, index) => (
        <Box
          padding="4"
          borderColor="border-subtle"
          borderRadius="large"
          borderWidth="1"
          key={field.periodeStart}
        >
          <HStack key={field.periodeStart} gap="4">
            <Select readOnly label="Valuta" size="small">
              <option value={undefined}>{field.valuta}</option>
            </Select>

            <TextField
              label={avtaletekster.prismodell.pris.label}
              size="small"
              type="number"
              error={errors?.satser?.[index]?.pris?.message}
              {...register(`satser.${index}.pris`, {
                valueAsNumber: true,
              })}
            />

            <ControlledDateInput
              label={avtaletekster.prismodell.periodeStart.label}
              fromDate={new Date(startDato)}
              toDate={sluttDato ? new Date(sluttDato) : new Date()}
              format={"iso-string"}
              size="small"
              {...register(`satser.${index}.periodeStart`)}
              control={control}
            />

            <ControlledDateInput
              size="small"
              label={avtaletekster.prismodell.periodeSlutt.label}
              fromDate={new Date(startDato)}
              toDate={sluttDato ? new Date(sluttDato) : new Date()}
              format={"iso-string"}
              {...register(`satser.${index}.periodeSlutt`)}
              control={control}
            />

            <Button
              className="mt-2 ml-auto"
              variant="tertiary"
              size="small"
              type="button"
              onClick={() => remove(index)}
            >
              <XMarkIcon fontSize="1.5rem" aria-label="Fjern periode" />
            </Button>
          </HStack>
        </Box>
      ))}
      <Button
        className="mt-2 ml-auto"
        variant="tertiary"
        size="small"
        type="button"
        onClick={() => append({ periodeStart: "", periodeSlutt: "", pris: 0, valuta: "NOK" })}
      >
        <div className="flex items-center gap-2">
          <PlusIcon aria-label="Legg til ny periode" />
          Legg til ny periode
        </div>
      </Button>
    </VStack>
  );
}

function Prisbetingelser() {
  const {
    register,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  return (
    <Textarea
      size="small"
      error={errors.prisbetingelser?.message}
      label={avtaletekster.prisOgBetalingLabel}
      {...register("prisbetingelser")}
    />
  );
}
