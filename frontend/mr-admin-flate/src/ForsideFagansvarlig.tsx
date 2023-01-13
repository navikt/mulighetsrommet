import { Heading } from "@navikt/ds-react";
import { useFeatureToggles } from "./api/features/feature-toggles";
import { Laster } from "./components/Laster";
import { Navbar } from "./components/shortcuts/Navbar";
import { shortcutsForFagansvarlig } from "./constants";

export function ForsideFagansvarlig() {
  const { data, isLoading } = useFeatureToggles();

  if (isLoading) return <Laster size="xlarge" />;

  if (!data) return null;

  if (!data["mulighetsrommet.enable-admin-flate"]) {
    return (
      <Heading data-testid="admin-heading" size="xlarge">
        Admin-flate er skrudd av 💤
      </Heading>
    );
  }

  return <Navbar shortcuts={shortcutsForFagansvarlig} />;
}
