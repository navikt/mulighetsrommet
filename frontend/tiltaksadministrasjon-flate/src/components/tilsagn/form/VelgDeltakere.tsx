import { TilsagnRequest } from "@tiltaksadministrasjon/api-client";
import { Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { useTilsagnValgbareDeltakere } from "@/api/tilsagn/useTilsagnValgbareDeltakere";
import { useState } from "react";
import { VelgDeltakereModal } from "./VelgDeltakereModal";
import { PlusIcon } from "@navikt/aksel-icons";
import { TilsagnDeltakereTable } from "./TilsagnDeltakereTable";
import { FeilmeldingMedVarselTrekant } from "@mr/frontend-common/components/varsel/FeilmeldingMedVarseltrekant";

interface Props {
  gjennomforingId: string;
}

export function VelgDeltakere({ gjennomforingId }: Props) {
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const {
    watch,
    setValue,
    clearErrors,
    formState: { errors },
  } = useFormContext<TilsagnRequest>();

  const {
    data: { tilsagnPerDeltaker, deltakere },
  } = useTilsagnValgbareDeltakere({
    gjennomforingId,
  });

  if (!tilsagnPerDeltaker) {
    return null;
  }

  const selected = watch("deltakere");

  return (
    <VStack gap="space-4">
      <Heading size="small">Deltakere</Heading>
      <TilsagnDeltakereTable
        deltakere={deltakere.filter((d) => selected?.some((s) => s.deltakerId === d.deltakerId))}
        selected={selected ?? []}
      />
      <HStack justify="end">
        <Button
          size="small"
          type="button"
          icon={<PlusIcon aria-hidden fontSize="1.5rem" />}
          variant="tertiary"
          onClick={() => {
            setModalOpen(true);
            clearErrors("deltakere");
          }}
        >
          Legg til deltakere
        </Button>
      </HStack>
      <VelgDeltakereModal
        open={modalOpen}
        deltakere={deltakere}
        selectedDeltakere={selected ?? []}
        onClose={() => setModalOpen(false)}
        onBekreft={(deltakerIds) => {
          setValue("deltakere", deltakerIds);
          setModalOpen(false);
        }}
      />
      {errors.deltakere?.message && (
        <FeilmeldingMedVarselTrekant size="small">
          {errors.deltakere.message}
        </FeilmeldingMedVarselTrekant>
      )}
    </VStack>
  );
}
