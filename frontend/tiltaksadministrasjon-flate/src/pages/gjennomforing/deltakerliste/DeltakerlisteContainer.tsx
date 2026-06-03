import { useGetGjennomforingIdFromUrl } from "@/hooks/useGetGjennomforingIdFromUrl";
import { MicroFrontend } from "@/micro-frontend/MicroFrontend";
import { Environment, environment } from "@/environment";

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
  switch (environment) {
    case Environment.PROD:
      return "https://cdn.nav.no/amt/amt-tiltakskoordinator-flate-prod/build";
    case Environment.DEV:
    case Environment.LOCAL:
      return "https://cdn.nav.no/amt/amt-tiltakskoordinator-flate-dev/build";
  }
}
