import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useOpprettAvtale } from "@/api/avtaler/useOpprettAvtale";
import { AvtaleDetaljerForm } from "@/components/avtaler/AvtaleDetaljerForm";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { WizardForm } from "@/components/skjema/WizardForm";
import {
  avtaleDetaljerFormSchema,
  AvtaleFormValues,
  PersonopplysningerSchema,
  PrismodellSchema,
  VeilederinformasjonStepSchema,
} from "@/pages/avtaler/form/validation";
import { WizardStep } from "@/hooks/useWizardForm";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { Heading } from "@navikt/ds-react";
import { useLocation, useNavigate } from "react-router";
import { toOpprettAvtaleRequest } from "./form/mappers";
import { AvtaleInformasjonForVeiledereForm } from "@/components/avtaler/AvtaleInformasjonForVeiledereForm";
import AvtalePrismodellStep from "@/components/avtaler/AvtalePrismodellStep";
import { v4 as uuidv4 } from "uuid";
import { defaultAvtaleData } from "@/pages/avtaler/form/defaults";

const steps: WizardStep[] = [
  {
    key: "Detaljer",
    schema: avtaleDetaljerFormSchema,
    Component: <AvtaleDetaljerForm />,
  },
  {
    key: "Prismodell",
    schema: PrismodellSchema,
    Component: <AvtalePrismodellStep />,
  },
  {
    key: "Personvern",
    schema: PersonopplysningerSchema,
    Component: <AvtalePersonvernForm />,
  },
  {
    key: "Veilederinformasjon",
    schema: VeilederinformasjonStepSchema,
    Component: <AvtaleInformasjonForVeiledereForm />,
  },
];

export function OpprettAvtaleFormPage() {
  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Avtaler", lenke: "/avtaler" },
    { tittel: "Ny avtale" },
  ];

  const navigate = useNavigate();
  const location = useLocation();
  const opprettAvtale = useOpprettAvtale();
  const { data: ansatt } = useHentAnsatt();

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <AvtaleIkon />
        <Heading size="large" level="2">
          Opprett ny avtale
        </Heading>
      </Header>
      <WizardForm<AvtaleFormValues>
        steps={steps}
        defaultValues={defaultAvtaleData(ansatt, location.state?.dupliserAvtale)}
        onCancel={() => navigate("/avtaler")}
        onSubmit={(data, form) => {
          const id = uuidv4();
          opprettAvtale.mutate(toOpprettAvtaleRequest(id, data), {
            onSuccess: () => navigate(`/avtaler/${id}`),
            onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
          });
        }}
        isSubmitting={opprettAvtale.isPending}
        labels={{
          submit: "Opprett avtale",
        }}
      />
    </>
  );
}
