import { useFormContext } from "react-hook-form";
import { AvtaleFormValues } from "@/schemas/avtale";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import AvtalePrismodellForm from "./AvtalePrismodellForm";
import { safeParseDate } from "@mr/frontend-common/utils/date";
import { Box, Select } from "@navikt/ds-react";
import { Tiltakskode, PrismodellType } from "@tiltaksadministrasjon/api-client";
import { avtaletekster } from "../ledetekster/avtaleLedetekster";

export default function AvtalePrismodellStep() {
  const { watch } = useFormContext<AvtaleFormValues>();
  const tiltakskode = watch("detaljer.tiltakskode");
  const startDato = safeParseDate(watch("detaljer.startDato"));

  if (
    [
      Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
      Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
    ].includes(tiltakskode)
  ) {
    return (
      <Box
        borderWidth="1"
        borderColor="border-subtle"
        borderRadius="large"
        padding="4"
        background="surface-subtle"
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
  return (
    <TwoColumnGrid>
      <AvtalePrismodellForm tiltakskode={tiltakskode} avtaleStartDato={startDato} />
    </TwoColumnGrid>
  );
}
