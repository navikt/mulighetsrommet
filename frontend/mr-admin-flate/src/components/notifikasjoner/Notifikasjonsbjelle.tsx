import { BellDotIcon } from "@navikt/aksel-icons";
import { useFeatureToggles } from "../../api/features/feature-toggles";

export function Notifikasjonsbjelle() {
  const { data: features } = useFeatureToggles();

  return features?.["mulighetsrommet.admin-flate-se-notifikasjoner"] ? (
    <div>
      <BellDotIcon fontSize={30} title="Notifikasjonsbjelle" />
    </div>
  ) : null;
}
