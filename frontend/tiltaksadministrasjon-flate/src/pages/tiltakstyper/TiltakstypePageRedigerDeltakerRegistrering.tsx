import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakstypeDeltakerRegistreringForm } from "@/components/tiltakstype/TiltakstypeDeltakerRegistreringForm";
import { useNavigate } from "react-router";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function TiltakstypePageRedigerDeltakerRegistrering() {
  const { data: tiltakstype } = useTiltakstypeById();
  const navigate = useNavigate();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstype", lenke: `/tiltakstyper/${tiltakstype.id}` },
    { tittel: "Rediger informasjon for deltakere" },
  ];

  async function handleSuccess() {
    navigate(`/tiltakstyper/${tiltakstype.id}/deltaker-registrering`);
  }

  return (
    <>
      <title>{`Rediger informasjon for deltakere | ${tiltakstype.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakstypeIkon />} heading={tiltakstype.navn} />
      <WhitePaddedBox>
        <TiltakstypeDeltakerRegistreringForm
          tiltakstype={tiltakstype}
          onSuccess={handleSuccess}
          onCancel={() => navigate(-1)}
        />
      </WhitePaddedBox>
    </>
  );
}
