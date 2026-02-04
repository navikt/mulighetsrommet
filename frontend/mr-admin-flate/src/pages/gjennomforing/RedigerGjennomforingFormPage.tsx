import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { QueryKeys } from "@/api/QueryKeys";
import { Header } from "@/components/detaljside/Header";
import { defaultGjennomforingData } from "@/components/gjennomforing/GjennomforingFormConst";
import { GjennomforingFormContainer } from "@/components/gjennomforing/GjennomforingFormContainer";
import { ErrorMeldinger } from "@/components/gjennomforing/GjennomforingFormErrors";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { avtaleHarRegioner } from "@/utils/Utils";
import { Alert, Box, Heading } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";
import { DataElementStatusTag } from "@mr/frontend-common";
import {
  AmoKategorisering,
  AvtaleDto,
  AvtaleGjennomforingDto,
  GjennomforingTiltakstype,
  GjennomforingVeilederinfoDto,
  PrismodellDto,
  UtdanningslopDto,
} from "@tiltaksadministrasjon/api-client";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { isEnkeltplass, isGruppetiltak } from "@/api/gjennomforing/utils";

export function RedigerGjennomforingFormPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const detaljer = useGjennomforing(gjennomforingId);
  const { data: avtale } = usePotentialAvtale(
    isGruppetiltak(detaljer.gjennomforing) ? detaljer.gjennomforing.avtaleId : null,
  );

  const isError = !avtale || !avtaleHarRegioner(avtale);

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: "/gjennomforinger",
    },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${detaljer.gjennomforing.id}`,
    },
    {
      tittel: "Rediger gjennomføring",
    },
  ];

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          Rediger gjennomføring
        </Heading>
        <DataElementStatusTag {...detaljer.gjennomforing.status.status} />
      </Header>
      <ContentBox>
        <Box padding="4" background="bg-default">
          {isError ? (
            <Alert variant="error">{ErrorMeldinger(avtale)}</Alert>
          ) : isEnkeltplass(detaljer.gjennomforing) ? (
            <Alert variant={"error"}>Enkeltplasser kan ikke redigeres</Alert>
          ) : (
            <RedigerGjennomforing
              tiltakstype={detaljer.tiltakstype}
              avtale={avtale}
              gjennomforing={detaljer.gjennomforing}
              veilederinfo={detaljer.veilederinfo}
              prismodell={detaljer.prismodell}
              amoKategorisering={detaljer.amoKategorisering}
              utdanningslop={detaljer.utdanningslop}
            />
          )}
        </Box>
      </ContentBox>
    </>
  );
}

interface RedigerGjennomforingProps {
  avtale: AvtaleDto;
  tiltakstype: GjennomforingTiltakstype;
  gjennomforing: AvtaleGjennomforingDto;
  veilederinfo: GjennomforingVeilederinfoDto | null;
  prismodell: PrismodellDto | null;
  amoKategorisering: AmoKategorisering | null;
  utdanningslop: UtdanningslopDto | null;
}

function RedigerGjennomforing(props: RedigerGjennomforingProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const tiltakstype = useTiltakstype(props.tiltakstype.id);
  const { data: ansatt } = useHentAnsatt();
  const { data: deltakere } = useGjennomforingDeltakerSummary(props.gjennomforing.id);

  const navigerTilbake = () => {
    navigate(-1);
  };

  const navigerTilGjennomforing = async (id: string) => {
    await queryClient.invalidateQueries({
      queryKey: QueryKeys.gjennomforing(id),
      type: "all",
    });
    navigate(`/gjennomforinger/${id}`);
  };

  return (
    <GjennomforingFormContainer
      onClose={navigerTilbake}
      onSuccess={navigerTilGjennomforing}
      tiltakstype={tiltakstype}
      avtale={props.avtale}
      gjennomforing={props.gjennomforing}
      veilederinfo={props.veilederinfo}
      deltakere={deltakere}
      defaultValues={defaultGjennomforingData(
        ansatt,
        tiltakstype,
        props.avtale,
        props.gjennomforing,
        props.veilederinfo,
        props.prismodell,
        props.amoKategorisering,
        props.utdanningslop,
      )}
    />
  );
}
