import { useFormContext } from "react-hook-form";
import { AvtaleFormValues } from "@/schemas/avtale";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import AvtalePrismodellForm from "./AvtalePrismodellForm";

export default function AvtalePrismodellStep() {
  const { watch } = useFormContext<AvtaleFormValues>();
  const tiltakskode = watch("tiltakskode");

  return (
    <TwoColumnGrid separator>
      <AvtalePrismodellForm tiltakskode={tiltakskode} />
    </TwoColumnGrid>
  );
}
