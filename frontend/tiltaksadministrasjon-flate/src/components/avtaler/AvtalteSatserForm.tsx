import { useFieldArray, useFormContext } from "react-hook-form";
import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { Button, HStack, Spacer, VStack } from "@navikt/ds-react";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";
import { addDuration, subDuration } from "@mr/frontend-common/utils/date";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { NumberInput } from "@/components/skjema/NumberInput";

export function AvtalteSatserForm({
  avtaleStartDato,
  field,
}: {
  avtaleStartDato: Date;
  field: `prismodeller.${number}`;
}) {
  const { control, watch } = useFormContext<AvtaleFormValues>();

  const { fields, append, remove } = useFieldArray({
    name: `${field}.satser` as const,
    control,
  });

  const valuta = watch(`${field}.valuta`);

  // Flere aktive avtaler har start i 2001, 2010 osv, 30 år holder enn så lenge men
  // burde ha en bedre løsning her. F. eks ikke bruk datepicker, men tekstfelt
  const toDate = addDuration(avtaleStartDato, { years: 30 });

  // Kan legge inn en måned før for hele uker case hvor første uke skal i måneden før
  const fromDate = subDuration(avtaleStartDato, { months: 1 });

  return (
    <VStack gap="space-16">
      {fields.map((satsField, index) => (
        <HStack
          key={satsField.id}
          padding="space-16"
          gap="space-16"
          wrap={false}
          align="center"
          className="border-ax-border-neutral-subtle border rounded-lg"
        >
          <HStack key={satsField.id} gap="space-16" align="start">
            <NumberInput<AvtaleFormValues>
              name={`${field}.satser.${index}.pris`}
              label={`${avtaletekster.prismodell.pris.label} (${valuta})`}
            />
            <FormDateInput<AvtaleFormValues>
              name={`${field}.satser.${index}.gjelderFra`}
              label={avtaletekster.prismodell.periodeStart.label}
              fromDate={fromDate}
              toDate={toDate}
              required
            />
          </HStack>
          <Spacer />
          <Button
            data-color="neutral"
            variant="secondary"
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
            pris: 0,
          })
        }
      >
        Legg til ny prisperiode
      </Button>
    </VStack>
  );
}
