import { useGetGjennomforingIdFromUrl } from "@/hooks/useGetGjennomforingIdFromUrl";
import { MicroFrontend } from "@/micro-frontend/MicroFrontend";

export function DeltakerlisteContainer() {
  const gjennomforingId = useGetGjennomforingIdFromUrl();

  if (!gjennomforingId) {
    return null;
  }

  const deltakerlisteUrl = resolveDeltakerlisteCdnUrl();

  return (
    <MicroFrontend
      baseUrl={deltakerlisteUrl}
      customComponentName={"tiltakskoordinator-deltakerliste"}
      customComponentProps={{ gjennomforingId }}
    />
  );
}

function resolveDeltakerlisteCdnUrl(): string {
  return "https://cdn.nav.no/amt/amt-tiltakskoordinator-flate-dev/build";
}
