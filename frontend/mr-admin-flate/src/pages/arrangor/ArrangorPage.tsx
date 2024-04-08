import { useParams } from "react-router-dom";
import { ArrangorIkon } from "../../components/ikoner/ArrangorIkon";
import { Brodsmule, Brodsmuler } from "../../components/navigering/Brodsmuler";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";

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
        <HeaderBanner heading="Arrangørdetaljer" ikon={<ArrangorIkon />} />
        <ContainerLayout>
          <p>Her kommer det noe detaljer om arrangøren</p>
        </ContainerLayout>
      </main>
    </>
  );
}
