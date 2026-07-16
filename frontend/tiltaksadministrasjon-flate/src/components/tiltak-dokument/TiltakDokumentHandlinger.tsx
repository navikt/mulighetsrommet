import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Switch } from "@navikt/ds-react";
import React from "react";
import { useNavigate } from "react-router";
import { useSetPublisertTiltakDokument } from "@/api/tiltak-dokument/useSetPublisertTiltakDokument";
import {
  TiltakDokumentHandling,
  useTiltakDokumentHandlinger,
} from "@/api/tiltak-dokument/useTiltakDokumentHandlinger";
import { TiltakDokument } from "@/api/tiltak-dokument/useTiltakDokumenter";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { previewArbeidsmarkedstiltakUrl } from "@/constants";
import { NavAnsattDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  gjennomforing: TiltakDokument;
  ansatt: NavAnsattDto;
}

export function TiltakDokumentHandlinger({ gjennomforing, ansatt }: Props) {
  const navigate = useNavigate();
  const { data: handlinger } = useTiltakDokumentHandlinger(gjennomforing.id);
  const { mutate: setPublisert } = useSetPublisertTiltakDokument(gjennomforing.id);

  function togglePublisert(e: React.MouseEvent<HTMLInputElement>) {
    setPublisert({ publisert: e.currentTarget.checked });
  }

  const administratorer = gjennomforing.administratorer.map((a) => a.navIdent);

  return (
    <KnapperadContainer>
      {handlinger.includes("PUBLISER") && (
        <Switch name="publiser" checked={gjennomforing.publisert} onClick={togglePublisert}>
          Publiser
        </Switch>
      )}
      <Handlinger<TiltakDokumentHandling>
        handlinger={handlinger}
        navIdent={ansatt.navIdent}
        grupper={[
          {
            label: "Tiltaksdokument",
            items: [
              {
                label: "Rediger",
                onClick: () => navigate(`/tiltak-dokumenter/${gjennomforing.id}/rediger`),
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
                href: `${previewArbeidsmarkedstiltakUrl()}/tiltak/${gjennomforing.id}`,
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
