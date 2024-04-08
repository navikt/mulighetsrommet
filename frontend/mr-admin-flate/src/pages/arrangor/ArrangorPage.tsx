import { Heading } from "@navikt/ds-react";
import { useParams } from "react-router-dom";
import { Header } from "../../components/detaljside/Header";
import { Brodsmule, Brodsmuler } from "../../components/navigering/Brodsmuler";
import { ContainerLayout } from "../../layouts/ContainerLayout";

export function ArrangorPage() {
  const { arrangorId } = useParams();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Forside", lenke: "/" },
    { tittel: "Arrangører", lenke: "/arrangorer" },
    { tittel: "Arrangørdetaljer", lenke: `/arrangorer/${arrangorId}` },
  ];

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <main>
        <Header>
          <Heading size="large" level="2">
            Arrangør: {arrangorId}
          </Heading>
        </Header>
        <ContainerLayout>
          <p>Her kommer det noe detaljer om arrangøren</p>
        </ContainerLayout>
      </main>
    </>
  );
}
