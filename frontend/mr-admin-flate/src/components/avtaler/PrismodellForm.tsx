import { useFieldArray, useFormContext } from "react-hook-form";
import { memo } from "react";
import { useForhandsgodkjenteSatser } from "@/api/tilsagn/useForhandsgodkjenteSatser";
import { AvtaleFormValues } from "@/schemas/avtale";
import { Prismodell, Tiltakskode } from "@mr/api-client-v2";
import { PlusIcon } from "@navikt/aksel-icons";
import { VStack, Box, HStack, TextField, Select, Button, Textarea } from "@navikt/ds-react";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { addDuration, formaterDato, parseDate } from "@mr/frontend-common/utils/date";
import TrashButton from "../skjema/TrashButton";

interface Props {
  tiltakskode: Tiltakskode;
  prismodell?: Prismodell;
}

const PrismodellForm = memo(({ tiltakskode, prismodell }: Props) => {
  switch (prismodell) {
    case Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
      return <ForhandsgodkjenteSatser tiltakskode={tiltakskode} />;
    case Prismodell.AVTALT_PRIS_PER_MANEDSVERK:
    case Prismodell.AVTALT_PRIS_PER_UKESVERK:
      return <AvtalteSatser />;
    case Prismodell.ANNEN_AVTALT_PRIS:
    default:
      return <Prisbetingelser />;
  }
});

function ForhandsgodkjenteSatser({ tiltakskode }: { tiltakskode: Tiltakskode }) {
  const { data: satser = [] } = useForhandsgodkjenteSatser(tiltakskode);
  if (!satser || satser.length === 0) return null;
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
            <TextField
              readOnly
              label={avtaletekster.prismodell.valuta.label}
              size="small"
              value={sats.valuta}
            />
            <TextField
              readOnly
              label={avtaletekster.prismodell.pris.label}
              size="small"
              value={sats.pris}
            />
            <TextField
              readOnly
              label={avtaletekster.prismodell.periodeStart.label}
              size="small"
              value={formaterDato(sats.periodeStart)}
            />
            <TextField
              readOnly
              label={avtaletekster.prismodell.periodeSlutt.label}
              size="small"
              value={formaterDato(sats.periodeSlutt)}
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

  const fromDate = parseDate(watch("startDato")) ?? new Date();
  // Pluss 10 år er vilkårlig valgt. Endre ved behov
  const toDate = addDuration(fromDate, { years: 10 })!;

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
          <HStack align="start" key={field.periodeStart} gap="4">
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
              fromDate={fromDate}
              toDate={toDate}
              format={"iso-string"}
              size="small"
              {...register(`satser.${index}.periodeStart`)}
              control={control}
            />
            <ControlledDateInput
              size="small"
              label={avtaletekster.prismodell.periodeSlutt.label}
              fromDate={fromDate}
              toDate={toDate}
              format={"iso-string"}
              {...register(`satser.${index}.periodeSlutt`)}
              control={control}
            />
            <TrashButton
              className="mt-7 ml-auto"
              tooltip="Fjern periode"
              onClick={() => remove(index)}
            />
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

export default PrismodellForm;
