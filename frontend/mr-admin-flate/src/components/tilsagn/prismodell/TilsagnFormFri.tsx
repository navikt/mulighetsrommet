import { TilsagnBeregningFri, GjennomforingDto } from "@mr/api-client-v2";
import { TilsagnForm } from "@/components/tilsagn/prismodell/TilsagnForm";
import { DeepPartial, useFieldArray, useFormContext } from "react-hook-form";
import { Button, HStack, Textarea, TextField, VStack } from "@navikt/ds-react";
import { TilsagnBeregningPreview } from "@/components/tilsagn/prismodell/TilsagnBeregningPreview";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { TrashIcon } from "@navikt/aksel-icons";

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
  } = useFormContext<FriTilsagn>();

  return (
    <>
      <div className="pb-3">
        <Textarea
          size="small"
          label={avtaletekster.prisOgBetalingLabel}
          readOnly
          error={errors.beregning?.prisbetingelser?.message}
          {...register("beregning.prisbetingelser")}
        />
      </div>

      <BeregningInputLinjerSkjema />
    </>
  );
}

function BeregningInputLinjerSkjema() {
  const {
    register,
    formState: { errors },
    control,
  } = useFormContext<FriTilsagn>();
  const { fields, append, remove } = useFieldArray({ control, name: "beregning.linjer" });
  const linjer = fields.map((item, index) => (
    <HStack gap="4" key={item.id}>
      <Textarea
        className="flex-1"
        size="small"
        label="Beskrivelse"
        error={errors.beregning?.input?.linjer?.[index]?.beskrivelse?.message}
        {...register(`beregning.input.linjer.${index}.beskrivelse`)}
        defaultValue={item.beskrivelse}
      />
      <div>
        <TextField
          size="small"
          type="number"
          label="Beløp"
          style={{ width: "180px" }}
          error={errors.beregning?.input?.linjer?.[index]?.belop?.message}
          {...register(`beregning.input.linjer.${index}.belop`, { valueAsNumber: true })}
          defaultValue={item.belop}
        />
      </div>
      <div>
        <TextField
          size="small"
          type="number"
          label="Antall"
          style={{ width: "180px" }}
          error={errors.beregning?.input?.linjer?.[index]?.antall?.message}
          {...register(`beregning.input.linjer.${index}.antall`, { valueAsNumber: true })}
          defaultValue={item.antall}
        />
      </div>
      <div>
        <TextField
          label="Linje-id"
          hideLabel
          hidden
          {...register(`beregning.input.linjer.${index}.id`)}
          defaultValue={item.id}
        />
        <Button
          className="mt-7"
          size="small"
          variant="danger"
          icon={<TrashIcon title="Slett linje" />}
          onClickCapture={(e) => {
            e.preventDefault();
            e.stopPropagation();
            remove(index);
          }}
        />
      </div>
    </HStack>
  ));
  return (
    <VStack className="mt-4" gap="4">
      {linjer}
      <div>
        <Button
          size="small"
          variant="secondary"
          onClickCapture={(e) => {
            e.preventDefault();
            e.stopPropagation();
            append({ id: window.crypto.randomUUID(), beskrivelse: "", belop: 0, antall: 1 });
          }}
        >
          Legg til ny linje
        </Button>
      </div>
    </VStack>
  );
}

function BeregningOutputPreview() {
  const { watch } = useFormContext<FriTilsagn>();

  const values = watch();
  return (
    <TilsagnBeregningPreview
      input={{
        type: "FRI",
        linjer: values.beregning?.input?.linjer || [],
        prisbetingelser: values.beregning?.prisbetingelser,
      }}
    />
  );
}
