import { Box, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { RammedetaljerRequest } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

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
      <VStack gap="2">
        <TextField
          label={avtaletekster.rammedetaljer.totalRamme}
          size="small"
          type="number"
          error={errors.totalRamme?.message}
          {...register("totalRamme", { required: true })}
        />
        <TextField
          label={avtaletekster.rammedetaljer.utbetaltArena}
          size="small"
          type="number"
          error={errors.utbetaltArena?.message}
          {...register("utbetaltArena", {
            valueAsNumber: true,
          })}
        />
      </VStack>
    </Box>
  );
}
