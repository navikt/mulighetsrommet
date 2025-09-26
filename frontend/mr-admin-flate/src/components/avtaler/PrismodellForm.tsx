import { useFieldArray, useFormContext } from "react-hook-form";
import { memo } from "react";
import { AvtaleFormValues } from "@/schemas/avtale";
import { PrismodellType } from "@mr/api-client-v2";
import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, HStack, Select, Spacer, Textarea, TextField, VStack } from "@navikt/ds-react";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { addDuration } from "@mr/frontend-common/utils/date";

interface Props {
  prismodell?: PrismodellType;
  avtaleStartDato: Date;
}

const PrismodellForm = memo(({ prismodell, avtaleStartDato }: Props) => {
  switch (prismodell) {
    case PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK:
      return null;
    case PrismodellType.AVTALT_PRIS_PER_MANEDSVERK:
    case PrismodellType.AVTALT_PRIS_PER_UKESVERK:
    case PrismodellType.AVTALT_PRIS_PER_HELE_UKESVERK:
    case PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER:
      return <AvtalteSatser fromDate={avtaleStartDato} />;
    case PrismodellType.ANNEN_AVTALT_PRIS:
    case undefined:
      return <PrisbetingelserTextArea />;
  }
});

function AvtalteSatser({ fromDate }: { fromDate: Date }) {
  const {
    control,
    register,
    setValue,
    getValues,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  const { fields, append, remove } = useFieldArray({
    name: "prismodell.satser",
    control,
  });

  // Flere aktive avtaler har start i 2001, 2010 osv, 30 år holder enn så lenge men
  // burde ha en bedre løsning her. F. eks ikke bruk datepicker, men tekstfelt
  const toDate = addDuration(fromDate, { years: 30 });

  return (
    <VStack gap="4">
      {fields.map((field, index) => (
        <HStack
          key={field.gjelderFra}
          padding="4"
          gap="4"
          wrap={false}
          align="center"
          className="border-border-subtle border-1 rounded-lg"
        >
          <HStack key={field.gjelderFra} gap="4" align="start">
            <Select readOnly label="Valuta" size="small">
              <option value={undefined}>{field.valuta}</option>
            </Select>
            <TextField
              label={avtaletekster.prismodell.pris.label}
              size="small"
              type="number"
              error={errors.prismodell?.satser?.[index]?.pris?.message}
              {...register(`prismodell.satser.${index}.pris`, {
                valueAsNumber: true,
              })}
            />
            <ControlledDateInput
              label={avtaletekster.prismodell.periodeStart.label}
              fromDate={fromDate}
              toDate={toDate}
              onChange={(val) => setValue(`prismodell.satser.${index}.gjelderFra`, val)}
              error={errors.prismodell?.satser?.[index]?.gjelderFra?.message}
              defaultSelected={getValues(`prismodell.satser.${index}.gjelderFra`)}
            />
          </HStack>
          <Spacer />
          <Button
            variant="secondary-neutral"
            size="small"
            type="button"
            icon={<TrashIcon aria-hidden />}
            onClick={() => remove(index)}
            className="max-h-min"
          >
            Fjern
          </Button>
        </HStack>
      ))}
      <Button
        className="self-end"
        variant="tertiary"
        size="small"
        type="button"
        icon={<PlusIcon aria-hidden />}
        onClick={() => append({ gjelderFra: null, gjelderTil: null, pris: 0, valuta: "NOK" })}
      >
        Legg til ny prisperiode
      </Button>
      <PrisbetingelserTextArea />
    </VStack>
  );
}

function PrisbetingelserTextArea() {
  const {
    register,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  return (
    <Textarea
      size="small"
      error={errors.prismodell?.prisbetingelser?.message}
      label={avtaletekster.prisOgBetalingLabel}
      {...register("prismodell.prisbetingelser")}
    />
  );
}

export default PrismodellForm;
