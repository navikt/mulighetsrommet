import { defaultAvtaleData } from "@/components/avtaler/AvtaleFormConst";
import { AvtaleFormContainer } from "@/components/avtaler/AvtaleFormContainer";
import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { AvtalestatusTag } from "@/components/statuselementer/AvtalestatusTag";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { inneholderUrl } from "@/utils/Utils";
import { Heading } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useLocation, useNavigate } from "react-router";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { QueryKeys } from "../../api/QueryKeys";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";

export function AvtaleFormPage() {
  const navigate = useNavigate();
  const { data: avtale } = useAvtale();
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: ansatt } = useHentAnsatt();
  const { data: enheter } = useNavEnheter();

  const queryClient = useQueryClient();
  const location = useLocation();

  const navigerTilbake = () => {
    navigate(-1);
  };

  const redigeringsModus = avtale ? inneholderUrl(avtale.id) : false;

  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Avtaler", lenke: "/avtaler" },
    redigeringsModus ? { tittel: "Avtale", lenke: `/avtaler/${avtale?.id}` } : undefined,
    {
      tittel: redigeringsModus ? "Rediger avtale" : "Ny avtale",
    },
  ];

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <AvtaleIkon />
        <Heading size="large" level="2">
          {redigeringsModus ? "Rediger avtale" : "Opprett ny avtale"}
        </Heading>
        {avtale ? <AvtalestatusTag avtale={avtale} showAvbruttAarsak /> : null}
      </Header>
      <ContentBox>
        <WhitePaddedBox>
          <AvtaleFormContainer
            onClose={() => {
              navigerTilbake();
            }}
            onSuccess={async (id) => {
              await queryClient.invalidateQueries({
                queryKey: QueryKeys.avtale(avtale?.id),
                refetchType: "all",
              });
              navigate(`/avtaler/${id}`);
            }}
            tiltakstyper={tiltakstyper.data}
            ansatt={ansatt}
            enheter={enheter}
            avtale={avtale}
            defaultValues={defaultAvtaleData(ansatt, location.state?.dupliserAvtale ?? avtale)}
            redigeringsModus={redigeringsModus}
          />
        </WhitePaddedBox>
      </ContentBox>
    </main>
  );
}
