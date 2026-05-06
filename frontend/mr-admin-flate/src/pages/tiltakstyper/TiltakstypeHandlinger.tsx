import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { useTiltakstypeHandlinger } from "@/api/tiltakstyper/useTiltakstypeHandlinger";
import { EndringshistorikkType, TiltakstypeHandling } from "@tiltaksadministrasjon/api-client";
import { useEndringshistorikk } from "@/api/endringshistorikk/useEndringshistorikk";

export function TiltakstypeHandlinger() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();
  const handlinger = useTiltakstypeHandlinger();

  return (
    <KnapperadContainer>
      <EndringshistorikkPopover>
        <TiltakstypeEndringshistorikk id={tiltakstypeId} />
      </EndringshistorikkPopover>
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

function TiltakstypeEndringshistorikk({ id }: { id: string }) {
  const historikk = useEndringshistorikk(id, EndringshistorikkType.TILTAKSTYPE);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
