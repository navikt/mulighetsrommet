import { defaultAvtaleData } from "@/components/avtaler/AvtaleFormConst";
import { AvtaleFormContainer } from "@/components/avtaler/AvtaleFormContainer";
import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { AvtalestatusTag } from "@/components/statuselementer/AvtalestatusTag";
import { inneholderUrl } from "@/utils/Utils";
import { Heading } from "@navikt/ds-react";
import { useLoaderData, useLocation, useNavigate } from "react-router";
import { avtaleSkjemaLoader } from "./avtaleLoader";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";

export function AvtaleFormPage() {
  const navigate = useNavigate();
  const { avtale, ansatt, enheter, tiltakstyper } = useLoaderData<typeof avtaleSkjemaLoader>();
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
            onSuccess={(id) => navigate(`/avtaler/${id}`)}
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
