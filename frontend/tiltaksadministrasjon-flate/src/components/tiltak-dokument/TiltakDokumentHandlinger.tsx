import { Endringshistorikk } from "@/components/endringshistorikk/Endringshistorikk";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Switch } from "@navikt/ds-react";
import React from "react";
import { useNavigate } from "react-router";
import { useSetPublisertTiltakDokument } from "@/api/tiltak-dokument/useSetPublisertTiltakDokument";
import {
  type TiltakDokumentHandling,
  useTiltakDokumentHandlinger,
} from "@/api/tiltak-dokument/useTiltakDokumentHandlinger";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { previewArbeidsmarkedstiltakUrl } from "@/constants";
import {
  EndringshistorikkType,
  NavAnsattDto,
  TiltakDokumentDto,
} from "@tiltaksadministrasjon/api-client";

interface Props {
  tiltakDokument: TiltakDokumentDto;
  ansatt: NavAnsattDto;
}

export function TiltakDokumentHandlinger({ tiltakDokument, ansatt }: Props) {
  const navigate = useNavigate();
  const { data: handlinger } = useTiltakDokumentHandlinger(tiltakDokument.id);
  const { mutate: setPublisert } = useSetPublisertTiltakDokument(tiltakDokument.id);

  function togglePublisert(e: React.MouseEvent<HTMLInputElement>) {
    setPublisert({ publisert: e.currentTarget.checked });
  }

  const administratorer = tiltakDokument.administratorer.map((a) => a.navIdent);

  return (
    <KnapperadContainer>
      {handlinger.includes("PUBLISER") && (
        <Switch
          name="publiser"
          checked={tiltakDokument.veilederinfo.publisert}
          onClick={togglePublisert}
        >
          Publiser
        </Switch>
      )}
      <Endringshistorikk id={tiltakDokument.id} type={EndringshistorikkType.TILTAK_DOKUMENT} />
      <Handlinger<TiltakDokumentHandling>
        handlinger={handlinger}
        navIdent={ansatt.navIdent}
        grupper={[
          {
            label: "Tiltaksdokument",
            items: [
              {
                label: "Rediger",
                onClick: () => navigate(`/tiltak-dokumenter/${tiltakDokument.id}/rediger`),
                handling: "REDIGER",
                administratorer,
              },
            ],
          },
          {
            label: "Lenker",
            items: [
              {
                label: "Forhåndsvis i Modia",
                href: `${previewArbeidsmarkedstiltakUrl()}/tiltak/${tiltakDokument.id}`,
                isExternal: true,
                icon: <ExternalLinkIcon aria-hidden />,
                handling: "FORHANDSVIS_I_MODIA",
              },
            ],
          },
        ]}
      />
    </KnapperadContainer>
  );
}
