import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { ActionMenu } from "@navikt/ds-react";
import { useNavigate } from "react-router";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { useTiltakstypeHandlinger } from "@/api/tiltakstyper/useTiltakstypeHandlinger";
import { EndringshistorikkType, TiltakstypeHandling } from "@tiltaksadministrasjon/api-client";
import { useEndringshistorikk } from "@/api/endringshistorikk/useEndringshistorikk";

export function TiltakstypeHandlinger() {
  const navigate = useNavigate();
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();
  const handlinger = useTiltakstypeHandlinger();

  return (
    <KnapperadContainer>
      <EndringshistorikkPopover>
        <TiltakstypeEndringshistorikk id={tiltakstypeId} />
      </EndringshistorikkPopover>
      <Handlinger>
        {handlinger.includes(TiltakstypeHandling.REDIGER_VEILEDERINFO) && (
          <ActionMenu.Item
            onClick={() => navigate(`/tiltakstyper/${tiltakstypeId}/redaksjonelt-innhold/rediger`)}
          >
            Rediger informasjon for veiledere
          </ActionMenu.Item>
        )}
        {handlinger.includes(TiltakstypeHandling.REDIGER_DELTAKERINFO) && (
          <ActionMenu.Item
            onClick={() => navigate(`/tiltakstyper/${tiltakstypeId}/deltaker-registrering/rediger`)}
          >
            Rediger informasjon for deltakere
          </ActionMenu.Item>
        )}
      </Handlinger>
    </KnapperadContainer>
  );
}

function TiltakstypeEndringshistorikk({ id }: { id: string }) {
  const historikk = useEndringshistorikk(id, EndringshistorikkType.TILTAKSTYPE);

  return <ViewEndringshistorikk historikk={historikk.data} />;
}
