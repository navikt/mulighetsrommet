import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakstypeInformasjonForVeiledereForm } from "@/components/tiltakstype/TiltakstypeInformasjonForVeiledereForm";
import { useNavigate } from "react-router";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function TiltakstypePageRedigerInformasjonForVeiledere() {
  const { data: tiltakstype } = useTiltakstypeById();
  const navigate = useNavigate();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstype", lenke: `/tiltakstyper/${tiltakstype.id}` },
    { tittel: "Rediger informasjon for veiledere" },
  ];

  async function handleSuccess() {
    navigate(`/tiltakstyper/${tiltakstype.id}/redaksjonelt-innhold`);
  }

  return (
    <>
      <title>{`Rediger tiltakstype | ${tiltakstype.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakstypeIkon />} heading={tiltakstype.navn} />
      <WhitePaddedBox>
        <TiltakstypeInformasjonForVeiledereForm
          tiltakstype={tiltakstype}
          onSuccess={handleSuccess}
          onCancel={() => navigate(-1)}
        />
      </WhitePaddedBox>
    </>
  );
}
