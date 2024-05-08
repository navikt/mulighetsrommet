import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import { TiltakstypeTabell } from "../../components/tabell/TiltakstypeTabell";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { ReloadAppErrorBoundary, useTitle } from "mulighetsrommet-frontend-common";
import { TilToppenKnapp } from "mulighetsrommet-frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Brodsmuler } from "../../components/navigering/Brodsmuler";
import { TiltakstypeIkon } from "../../components/ikoner/TiltakstypeIkon";

export function TiltakstyperPage() {
  useTitle("Tiltakstyper");
  return (
    <>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Forside", lenke: "/" },
          { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
        ]}
      />
      <HeaderBanner heading="Oversikt over tiltakstyper" ikon={<TiltakstypeIkon />} />
      <MainContainer>
        <ContainerLayout>
          <ReloadAppErrorBoundary>
            <TiltakstypeTabell />
          </ReloadAppErrorBoundary>
        </ContainerLayout>
      </MainContainer>
      <TilToppenKnapp />
    </>
  );
}
