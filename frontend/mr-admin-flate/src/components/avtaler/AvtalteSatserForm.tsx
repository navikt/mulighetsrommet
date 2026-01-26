import { useFieldArray, useFormContext } from "react-hook-form";
import { AvtaleFormValues } from "@/schemas/avtale";
import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, HStack, Spacer, TextField, VStack } from "@navikt/ds-react";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { ControlledDateInput } from "../skjema/ControlledDateInput";
import { addDuration, subDuration } from "@mr/frontend-common/utils/date";
import { useEffect } from "react";

export function AvtalteSatserForm({
  avtaleStartDato,
  field,
}: {
  avtaleStartDato: Date;
  field: `prismodeller.${number}`;
}) {
  const {
    control,
    register,
    setValue,
    getValues,
    watch,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();

  const { fields, append, remove } = useFieldArray({
    name: `${field}.satser` as const,
    control,
  });

  const valuta = watch(`${field}.valuta`);
  useEffect(() => {
    fields.forEach((satsField, index) => {
      setValue(`${field}.satser.${index}.pris.valuta` as const, valuta);
    });
  }, [valuta]);

  // Flere aktive avtaler har start i 2001, 2010 osv, 30 år holder enn så lenge men
  // burde ha en bedre løsning her. F. eks ikke bruk datepicker, men tekstfelt
  const toDate = addDuration(avtaleStartDato, { years: 30 });

  // Kan legge inn en måned før for hele uker case hvor første uke skal i måneden før
  const fromDate = subDuration(avtaleStartDato, { months: 1 });

  return (
    <VStack gap="4">
      {fields.map((satsField, index) => (
        <HStack
          key={satsField.id}
          padding="4"
          gap="4"
          wrap={false}
          align="center"
          className="border-border-subtle border rounded-lg"
        >
          <HStack key={satsField.id} gap="4" align="start">
            <TextField
              label={avtaletekster.prismodell.pris.label + " (" + valuta + ")"}
              size="small"
              type="number"
              error={
                errors.prismodeller?.[parseInt(field.split(".")[1])]?.satser?.[index]?.pris?.belop
                  ?.message
              }
              {...register(`${field}.satser.${index}.pris.belop` as const, {
                setValueAs: (v) => (v === "" ? null : Number(v)),
              })}
            />
            <ControlledDateInput
              label={avtaletekster.prismodell.periodeStart.label}
              fromDate={fromDate}
              toDate={toDate}
              onChange={(val) => setValue(`${field}.satser.${index}.gjelderFra` as const, val)}
              required
              error={
                errors.prismodeller?.[parseInt(field.split(".")[1])]?.satser?.[index]?.gjelderFra
                  ?.message
              }
              defaultSelected={getValues(`${field}.satser.${index}.gjelderFra` as const)}
            />
          </HStack>
          <Spacer />
          <Button
            variant="secondary-neutral"
            size="small"
            type="button"
            icon={<TrashIcon aria-hidden />}
            onClick={() => remove(index)}
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
        onClick={() =>
          append({
            gjelderFra: "",
            gjelderTil: null,
            pris: { belop: 0, valuta: valuta },
          })
        }
      >
        Legg til ny prisperiode
      </Button>
    </VStack>
  );
}
