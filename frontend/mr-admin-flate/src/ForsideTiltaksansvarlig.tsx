import { Heading, Loader } from "@navikt/ds-react";
import { useFeatureToggles } from "./api/features/feature-toggles";
import { Shortcuts } from "./components/shortcuts/Shortcuts";
import { shortcutsForTiltaksansvarlig } from "./constants";

export function ForsideTiltaksansvarlig() {
  const { data, isLoading } = useFeatureToggles();

  if (isLoading) return <Loader size="xlarge" />;

  if (!data) return null;

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
      <Shortcuts shortcuts={shortcutsForTiltaksansvarlig} />
    </>
  );
}
