import { Box, TextField } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { RammedetaljerRequest } from "@tiltaksadministrasjon/api-client";
import { isNumber } from "@grafana/faro-web-sdk";

export default function AvtaleRammeDetaljerForm() {
  const {
    formState: { errors },
    register,
  } = useFormContext<RammedetaljerRequest>();

  return (
    <Box
      borderWidth="1"
      borderColor="border-subtle"
      borderRadius="large"
      padding="4"
      background="surface-subtle"
    >
      <TextField
        label="Total ramme"
        size="small"
        type="number"
        error={errors.totalRamme?.message}
        {...register("totalRamme", { required: true })}
      />
      <TextField
        label="Utbetalt fra Arena"
        size="small"
        type="number"
        error={errors.utbetaltArena?.message}
        {...register("utbetaltArena", { setValueAs: (value) => (isNumber(value) ? value : null) })}
      />
    </Box>
  );
}
