import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Switch } from "@navikt/ds-react";
import React from "react";
import { useNavigate } from "react-router";
import { useSetPublisertIndividuellGjennomforing } from "@/api/individuell-gjennomforing/useSetPublisertIndividuellGjennomforing";
import {
  IndividuellGjennomforingHandling,
  useIndividuellGjennomforingHandlinger,
} from "@/api/individuell-gjennomforing/useIndividuellGjennomforingHandlinger";
import { IndividuellGjennomforing } from "@/api/individuell-gjennomforing/useIndividuelleGjennomforinger";
import { KnapperadContainer } from "@/layouts/KnapperadContainer";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { previewArbeidsmarkedstiltakUrl } from "@/constants";
import { NavAnsattDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  gjennomforing: IndividuellGjennomforing;
  ansatt: NavAnsattDto;
}

export function IndividuellGjennomforingHandlinger({ gjennomforing, ansatt }: Props) {
  const navigate = useNavigate();
  const { data: handlinger } = useIndividuellGjennomforingHandlinger(gjennomforing.id);
  const { mutate: setPublisert } = useSetPublisertIndividuellGjennomforing(gjennomforing.id);

  function togglePublisert(e: React.MouseEvent<HTMLInputElement>) {
    setPublisert({ publisert: e.currentTarget.checked });
  }

  const administratorer = gjennomforing.administratorer.map((a) => a.navIdent);
  console.log(administratorer)

  return (
    <KnapperadContainer>
      {handlinger.includes("PUBLISER") && (
        <Switch name="publiser" checked={gjennomforing.publisert} onClick={togglePublisert}>
          Publiser
        </Switch>
      )}
      <Handlinger<IndividuellGjennomforingHandling>
        handlinger={handlinger}
        navIdent={ansatt.navIdent}
        grupper={[
          {
            label: "Individuell gjennomføring",
            items: [
              {
                label: "Rediger",
                onClick: () => navigate(`/individuelle-gjennomforinger/${gjennomforing.id}/rediger`),
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
