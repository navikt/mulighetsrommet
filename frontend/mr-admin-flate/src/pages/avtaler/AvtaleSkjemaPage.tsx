import { useLocation, useNavigate } from "react-router-dom";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { inneholderUrl } from "@/utils/Utils";
import { Header } from "@/components/detaljside/Header";
import { Laster } from "@/components/laster/Laster";
import { AvtaleSkjemaContainer } from "@/components/avtaler/AvtaleSkjemaContainer";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { AvtalestatusTag } from "@/components/statuselementer/AvtalestatusTag";
import { Heading } from "@navikt/ds-react";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { SkjemaContainer } from "@/components/skjema/SkjemaContainer";
import { SkjemaContent } from "@/components/skjema/SkjemaContent";
import { defaultAvtaleData } from "@/components/avtaler/AvtaleSkjemaConst";

export function AvtaleSkjemaPage() {
  const navigate = useNavigate();

  const { data: avtale, isLoading: isAvtaleLoading } = useAvtale();
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } = useTiltakstyper();
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: enheter, isLoading: isLoadingEnheter } = useNavEnheter();
  const location = useLocation();

  const navigerTilbake = () => {
    navigate(-1);
  };

  if (isAvtaleLoading || isLoadingAnsatt) {
    return <Laster size="xlarge" tekst={"Laster avtale..."} />;
  }

  const redigeringsModus = avtale ? inneholderUrl(avtale.id) : false;

  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Forside", lenke: "/" },
    { tittel: "Avtaler", lenke: "/avtaler" },
    redigeringsModus
      ? {
          tittel: "Avtaledetaljer",
          lenke: `/avtaler/${avtale?.id}`,
        }
      : undefined,
    {
      tittel: redigeringsModus ? "Rediger avtale" : "Ny avtale",
      lenke: redigeringsModus ? `/avtaler/${avtale?.id}/skjema` : "/avtaler/skjema",
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

      <ContainerLayout>
        <SkjemaContainer>
          {isLoadingTiltakstyper || isLoadingEnheter ? <Laster /> : null}
          <SkjemaContent>
            {!tiltakstyper?.data || !ansatt || !enheter ? null : (
              <AvtaleSkjemaContainer
                onClose={() => {
                  navigerTilbake();
                }}
                onSuccess={(id) => navigate(`/avtaler/${id}`)}
                tiltakstyper={tiltakstyper.data}
                ansatt={ansatt}
                enheter={enheter}
                avtale={avtale}
                defaultValues={defaultAvtaleData(ansatt, location.state?.dupliserAvtale ?? avtale)}
                redigeringsModus={redigeringsModus}
              />
            )}
          </SkjemaContent>
        </SkjemaContainer>
      </ContainerLayout>
    </main>
  );
}
