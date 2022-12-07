import { Heading } from "@navikt/ds-react";
import { useFeatureToggles } from "./api/features/feature-toggles";
import { ShortcutsFagansvarlig } from "./components/shortcuts/ShortcutsFagansvarlig";

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
      <ShortcutsFagansvarlig />
    </>
  );
}
