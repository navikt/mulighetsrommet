import { Endringshistorikk } from "@/components/endringshistorikk/Endringshistorikk";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { useTiltakstypeHandlinger } from "@/api/tiltakstyper/useTiltakstypeHandlinger";
import { EndringshistorikkType, TiltakstypeHandling } from "@tiltaksadministrasjon/api-client";

export function TiltakstypeHandlinger() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();
  const handlinger = useTiltakstypeHandlinger();

  return (
    <KnapperadContainer>
      <Endringshistorikk id={tiltakstypeId} type={EndringshistorikkType.TILTAKSTYPE} />
      <Handlinger
        handlinger={handlinger}
        grupper={[
          {
            items: [
              {
                label: "Rediger informasjon for veiledere",
                href: `/tiltakstyper/${tiltakstypeId}/redaksjonelt-innhold/rediger`,
                handling: TiltakstypeHandling.REDIGER_VEILEDERINFO,
              },
              {
                label: "Rediger informasjon for deltakere",
                href: `/tiltakstyper/${tiltakstypeId}/deltaker-registrering/rediger`,
                handling: TiltakstypeHandling.REDIGER_DELTAKERINFO,
              },
            ],
          },
        ]}
      />
    </KnapperadContainer>
  );
}
