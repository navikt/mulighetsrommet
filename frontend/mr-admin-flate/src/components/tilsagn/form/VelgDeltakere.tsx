import { TilsagnDeltakerPersonalia, TilsagnRequest } from "@tiltaksadministrasjon/api-client";
import { UNSAFE_Combobox } from "@navikt/ds-react";
import { Controller, useFormContext } from "react-hook-form";
import { useTilsagnValgbareDeltakere } from "@/api/tilsagn/useTilsagnValgbareDeltakere";
import { formatTilsagnDeltaker } from "@/utils/Utils";

interface Props {
  gjennomforingId: string;
}

export function VelgDeltakere({ gjennomforingId }: Props) {
  const {
    watch,
    control,
    formState: { errors },
  } = useFormContext<TilsagnRequest>();

  const periodeStart = watch("periodeStart");
  const periodeSlutt = watch("periodeSlutt");

  const {
    data: { tilsagnPerDeltaker, deltakere },
  } = useTilsagnValgbareDeltakere({
    gjennomforingId,
    periodeStart,
    periodeSlutt,
  });

  if (!tilsagnPerDeltaker) {
    return null;
  }

  return (
    <Controller
      control={control}
      name="deltakere"
      render={({ field }) => (
        <UNSAFE_Combobox
          description="Filtrert basert på overlappende tilsagn og deltakelsesperiode"
          size="small"
          id="arrangorKontaktpersoner"
          label="Deltakere"
          placeholder="Velg deltakere"
          isMultiSelect
          name={field.name}
          error={errors.deltakere?.message}
          options={deltakere.map((deltaker: TilsagnDeltakerPersonalia) => ({
            label: formatTilsagnDeltaker(deltaker),
            value: deltaker.deltakerId,
          }))}
          onToggleSelected={(option, isSelected) => {
            const currentValues = field.value ?? [];
            if (isSelected) {
              field.onChange([...currentValues, option]);
            } else {
              field.onChange(currentValues.filter((v) => v !== option));
            }
          }}
        />
      )}
    />
  );
}
