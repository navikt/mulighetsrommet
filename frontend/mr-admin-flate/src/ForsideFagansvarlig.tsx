import { Heading } from "@navikt/ds-react";
import { useFeatureToggles } from "./api/features/feature-toggles";
import { Shortcuts } from "./components/shortcuts/Shortcuts";
import { shortcutsForFagansvarlig } from "./constants";

export function ForsideFagansvarlig() {
  const { data, isLoading } = useFeatureToggles();

  if (!data || isLoading) return null;

  if (!data["mulighetsrommet.enable-admin-flate"]) {
    return (
      <Heading data-testid="admin-heading" size="xlarge">
        Admin-flate er skrudd av 💤
      </Heading>
    );
  }

  return (
    <>
      <h1>Oversikt</h1>
      <Shortcuts shortcuts={shortcutsForFagansvarlig} />
    </>
  );
}
