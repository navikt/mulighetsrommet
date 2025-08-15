import { GjennomforingDto, TilsagnBeregningFri, TilsagnBeregningType } from "@mr/api-client-v2";
import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { DeepPartial, useFieldArray, useFormContext } from "react-hook-form";
import {
  Alert,
  Button,
  Heading,
  HStack,
  Label,
  Spacer,
  Textarea,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { TilsagnBeregningPreview } from "@/components/tilsagn/form/TilsagnBeregningPreview";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { PlusIcon, TrashIcon } from "@navikt/aksel-icons";
import { InferredTilsagn } from "./TilsagnSchema";

type FriTilsagn = InferredTilsagn & { beregning: TilsagnBeregningFri };

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<FriTilsagn>;
  regioner: string[];
}

export function TilsagnFormFri(props: Props) {
  return (
    <TilsagnForm
      {...props}
      beregningInput={<BeregningInputSkjema />}
      beregningOutput={<BeregningOutputPreview />}
    />
  );
}

function BeregningInputSkjema() {
  const {
    register,
    formState: { errors },
    setError,
    control,
  } = useFormContext<FriTilsagn>();

  const { fields, append, remove } = useFieldArray({ control, name: "beregning.linjer" });

  return (
    <VStack gap="4">
      <Heading size="small">Prismodell - Annen avtalt pris</Heading>
      <Textarea
        size="small"
        label={avtaletekster.prisOgBetalingLabel}
        readOnly
        error={errors.beregning?.prisbetingelser?.message}
        {...register("beregning.prisbetingelser")}
      />
      <Label size="small">Avtalte priser</Label>
      {fields.map((item, index) => (
        <HStack
          gap="4"
          align="start"
          key={item.id}
          padding="4"
          className="border-border-subtle border-1 rounded-lg"
        >
          <Textarea
            size="small"
            label="Beskrivelse"
            className="flex-3"
            error={errors.beregning?.linjer?.[index]?.beskrivelse?.message}
            {...register(`beregning.linjer.${index}.beskrivelse`)}
            defaultValue={item.beskrivelse}
          />
          <TextField
            size="small"
            type="number"
            label="Beløp"
            className="flex-2"
            error={errors.beregning?.linjer?.[index]?.belop?.message}
            {...register(`beregning.linjer.${index}.belop`, { valueAsNumber: true })}
            defaultValue={item.belop}
          />
          <TextField
            size="small"
            type="number"
            label="Antall"
            className="flex-1"
            error={errors.beregning?.linjer?.[index]?.antall?.message}
            {...register(`beregning.linjer.${index}.antall`, { valueAsNumber: true })}
            defaultValue={item.antall}
          />
          <input
            type="hidden"
            {...register(`beregning.linjer.${index}.id`)}
            defaultValue={item.id}
          />
          <Spacer />
          <Button
            size="small"
            variant="secondary-neutral"
            icon={<TrashIcon aria-hidden />}
            className="max-h-min self-center"
            onClick={() => remove(index)}
          >
            Fjern
          </Button>
        </HStack>
      ))}
      {errors.beregning?.linjer?.message && (
        <Alert size="small" variant="error">
          {errors.beregning?.linjer?.message}
        </Alert>
      )}
      <Button
        size="small"
        variant="tertiary"
        icon={<PlusIcon aria-hidden />}
        className="self-end"
        onClick={() => {
          setError("beregning.linjer", {});
          append({ id: window.crypto.randomUUID(), beskrivelse: "", belop: 0, antall: 1 });
        }}
      >
        Legg til ny pris
      </Button>
    </VStack>
  );
}

function BeregningOutputPreview() {
  const { watch } = useFormContext<FriTilsagn>();

  const values = watch();
  const linjer = values.beregning?.linjer || [];
  return (
    <>
      <TilsagnBeregningPreview
        input={{
          type: TilsagnBeregningType.FRI,
          linjer: linjer,
          prisbetingelser: values.beregning?.prisbetingelser,
        }}
      />
    </>
  );
}
