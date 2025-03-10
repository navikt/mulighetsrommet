import { defaultAvtaleData } from "@/components/avtaler/AvtaleFormConst";
import { AvtaleFormContainer } from "@/components/avtaler/AvtaleFormContainer";
import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Heading } from "@navikt/ds-react";
import { useLocation, useNavigate } from "react-router";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";

export function NewAvtaleFormPage() {
  const navigate = useNavigate();
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: ansatt } = useHentAnsatt();
  const { data: enheter } = useNavEnheter();

  const location = useLocation();

  const navigerTilbake = () => {
    navigate(-1);
  };

  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Avtaler", lenke: "/avtaler" },
    {
      tittel: "Ny avtale",
    },
  ];

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <AvtaleIkon />
        <Heading size="large" level="2">
          Opprett ny avtale
        </Heading>
      </Header>
      <ContentBox>
        <WhitePaddedBox>
          <AvtaleFormContainer
            onClose={() => {
              navigerTilbake();
            }}
            onSuccess={async (id) => {
              navigate(`/avtaler/${id}`);
            }}
            tiltakstyper={tiltakstyper.data}
            ansatt={ansatt}
            enheter={enheter}
            defaultValues={defaultAvtaleData(ansatt, location.state?.dupliserAvtale)}
            redigeringsModus={false}
          />
        </WhitePaddedBox>
      </ContentBox>
    </main>
  );
}
