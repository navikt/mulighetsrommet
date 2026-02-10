import { useFormContext } from "react-hook-form";
import { AvtaleFormValues } from "@/schemas/avtale";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import AvtalePrismodellForm from "./AvtalePrismodellForm";
import { safeParseDate } from "@mr/frontend-common/utils/date";
import { Box, Select } from "@navikt/ds-react";
import { Avtaletype, PrismodellType } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

export default function AvtalePrismodellStep() {
  const { watch } = useFormContext<AvtaleFormValues>();

  const avtaletype = watch("detaljer.avtaletype");
  if (avtaletype === Avtaletype.FORHANDSGODKJENT) {
    return (
      <Box
        borderWidth="1"
        borderColor="neutral-subtle"
        borderRadius="8"
        padding="space-16"
        background="neutral-soft"
      >
        <Select
          label={avtaletekster.prismodell.label}
          readOnly
          value={PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK}
        >
          <option>Forhåndsgodkjent pris per månedsverk</option>
        </Select>
      </Box>
    );
  }

  const tiltakskode = watch("detaljer.tiltakskode");
  const startDato = safeParseDate(watch("detaljer.startDato"));
  return (
    <TwoColumnGrid>
      <AvtalePrismodellForm tiltakskode={tiltakskode} avtaleStartDato={startDato} />
    </TwoColumnGrid>
  );
}
