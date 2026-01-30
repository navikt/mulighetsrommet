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
import { AvtaleDto, GjennomforingDetaljerDto } from "@tiltaksadministrasjon/api-client";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";

export function RedigerGjennomforingFormPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const detaljer = useGjennomforing(gjennomforingId);
  const { data: avtale } = usePotentialAvtale(detaljer.gjennomforing.avtaleId);

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
          ) : (
            <RedigerGjennomforing avtale={avtale} detaljer={detaljer} />
          )}
        </Box>
      </ContentBox>
    </>
  );
}

interface RedigerGjennomforingProps {
  avtale: AvtaleDto;
  detaljer: GjennomforingDetaljerDto;
}

function RedigerGjennomforing(props: RedigerGjennomforingProps) {
  const { avtale, detaljer } = props;

  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);
  const { data: ansatt } = useHentAnsatt();
  const { data: deltakere } = useGjennomforingDeltakerSummary(detaljer.gjennomforing.id);

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
      avtale={avtale}
      gjennomforing={detaljer.gjennomforing}
      veilederinfo={detaljer.veilederinfo}
      deltakere={deltakere}
      defaultValues={defaultGjennomforingData(ansatt, tiltakstype, avtale, detaljer)}
    />
  );
}
