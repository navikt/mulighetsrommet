import { useCreateGjennomforing } from "@/api/gjennomforing/useCreateGjennomforing";
import { defaultGjennomforingData } from "@/pages/gjennomforing/form/defaults";
import { GjennomforingFormDetaljer } from "@/components/gjennomforing/GjennomforingFormDetaljer";
import { GjennomforingInformasjonForVeiledereForm } from "@/components/gjennomforing/GjennomforingInformasjonForVeiledereForm";
import { GjennomforingAvtaleIkon } from "@/components/ikoner/GjennomforingAvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { WizardForm } from "@/components/skjema/WizardForm";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import {
  gjennomforingDetaljerSchema,
  GjennomforingFormValues,
  gjennomforingVeilederinfoSchema,
} from "@/pages/gjennomforing/form/validation";
import { WizardStep } from "@/hooks/useWizardForm";
import { useLocation, useNavigate } from "react-router";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { toCreateGjennomforingRequest } from "./form/mappers";
import { v4 as uuidv4 } from "uuid";
import { HeaderBanner } from "@/layouts/HeaderBanner";

const brodsmuler: Array<Brodsmule | undefined> = [
  { tittel: "Gjennomføringer", lenke: "/gjennomforinger" },
  { tittel: "Opprett gjennomføring" },
];

export function OpprettGjennomforingPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const location = useLocation();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const tiltakstype = useTiltakstype(avtale.tiltakstype.id);
  const { data: ansatt } = useHentAnsatt();
  const createGjennomforing = useCreateGjennomforing();

  const steps: WizardStep[] = [
    {
      key: "Detaljer",
      schema: gjennomforingDetaljerSchema,
      Component: (
        <GjennomforingFormDetaljer
          tiltakstype={tiltakstype}
          avtale={avtale}
          gjennomforing={null}
          deltakere={null}
        />
      ),
    },
    {
      key: "Informasjon for veiledere",
      schema: gjennomforingVeilederinfoSchema,
      Component: <GjennomforingInformasjonForVeiledereForm avtale={avtale} veilederinfo={null} />,
    },
  ];

  return (
    <>
      <title>Opprett gjennomføring</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<GjennomforingAvtaleIkon />} heading="Opprett gjennomføring" />
      <WizardForm<GjennomforingFormValues>
        steps={steps}
        defaultValues={defaultGjennomforingData(
          ansatt,
          tiltakstype,
          avtale,
          location.state?.dupliserGjennomforing?.gjennomforing,
          location.state?.dupliserGjennomforing?.veilederinfo,
          null,
          null,
          null,
        )}
        onCancel={() => navigate(-1)}
        onSubmit={(data, form) => {
          const id = uuidv4();
          createGjennomforing.mutate(toCreateGjennomforingRequest(id, data, avtale), {
            onSuccess: () => navigate(`/gjennomforinger/${id}`),
            onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
          });
        }}
        isSubmitting={createGjennomforing.isPending}
        labels={{
          submit: "Opprett gjennomføring",
        }}
      />
    </>
  );
}
