import { useFormContext } from "react-hook-form";
import { AvtaleFormValues } from "@/schemas/avtale";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import AvtalePrismodellForm from "./AvtalePrismodellForm";
import { parseDate } from "@mr/frontend-common/utils/date";

export default function AvtalePrismodellStep() {
  const { watch } = useFormContext<AvtaleFormValues>();
  const tiltakskode = watch("detaljer.tiltakskode");
  const startDato = parseDate(watch("detaljer.startDato")) ?? new Date();

  return (
    <TwoColumnGrid separator>
      <AvtalePrismodellForm tiltakskode={tiltakskode} avtaleStartDato={startDato} />
    </TwoColumnGrid>
  );
}
