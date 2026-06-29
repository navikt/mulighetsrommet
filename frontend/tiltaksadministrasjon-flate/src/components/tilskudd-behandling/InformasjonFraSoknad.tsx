import { KostnadsstedDto, Periode } from "@tiltaksadministrasjon/api-client";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { formaterNavEnhet } from "@/utils/nav-enhet";

interface InformasjonFraSoknadProps {
  journalpostId: string | null;
  soknadsdato: string | null;
  periode: Periode | null;
  kostnadssted: KostnadsstedDto | null;
}

export function InformasjonFraSoknad(props: InformasjonFraSoknadProps) {
  return (
    <Definisjonsliste
      title="Informasjon fra søknad"
      definitions={[
        { key: "Journalpost-ID i Gosys", value: props.journalpostId },
        {
          key: "Søknadsdato",
          value: props.soknadsdato ? formaterDato(props.soknadsdato) : null,
        },
        {
          key: "Tilskuddsperiode",
          value: props.periode ? formaterPeriode(props.periode) : null,
        },
        {
          key: "Kostnadssted",
          value: props.kostnadssted ? formaterNavEnhet(props.kostnadssted) : null,
        },
      ]}
    />
  );
}
