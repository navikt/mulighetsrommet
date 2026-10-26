import { Endringshistorikk } from "@/components/endringshistorikk/Endringshistorikk";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Switch } from "@navikt/ds-react";
import React from "react";
import { useNavigate } from "react-router";
import { useSetPublisertTiltakBeskrivelse } from "@/api/tiltak-beskrivelse/useSetPublisertTiltakBeskrivelse";
import {
  type TiltakBeskrivelseHandling,
  useTiltakBeskrivelseHandlinger,
} from "@/api/tiltak-beskrivelse/useTiltakBeskrivelseHandlinger";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { previewArbeidsmarkedstiltakUrl } from "@/constants";
import {
  EndringshistorikkType,
  NavAnsattDto,
  TiltakBeskrivelseDto,
} from "@tiltaksadministrasjon/api-client";

interface Props {
  tiltakBeskrivelse: TiltakBeskrivelseDto;
  ansatt: NavAnsattDto;
}

export function TiltakBeskrivelseHandlinger({ tiltakBeskrivelse, ansatt }: Props) {
  const navigate = useNavigate();
  const { data: handlinger } = useTiltakBeskrivelseHandlinger(tiltakBeskrivelse.id);
  const { mutate: setPublisert } = useSetPublisertTiltakBeskrivelse(tiltakBeskrivelse.id);

  function togglePublisert(e: React.MouseEvent<HTMLInputElement>) {
    setPublisert({ publisert: e.currentTarget.checked });
  }

  const administratorer = tiltakBeskrivelse.administratorer.map((a) => a.navIdent);

  return (
    <KnapperadContainer>
      {handlinger.includes("PUBLISER") && (
        <Switch
          name="publiser"
          checked={tiltakBeskrivelse.veilederinfo.publisert}
          onClick={togglePublisert}
        >
          Publiser
        </Switch>
      )}
      <Endringshistorikk
        id={tiltakBeskrivelse.id}
        type={EndringshistorikkType.TILTAK_BESKRIVELSE}
      />
      <Handlinger<TiltakBeskrivelseHandling>
        handlinger={handlinger}
        navIdent={ansatt.navIdent}
        grupper={[
          {
            label: "Tiltaksbeskrivelse",
            items: [
              {
                label: "Rediger",
                onClick: () => navigate(`/tiltak-beskrivelser/${tiltakBeskrivelse.id}/rediger`),
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
                href: `${previewArbeidsmarkedstiltakUrl()}/tiltak/${tiltakBeskrivelse.id}`,
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
