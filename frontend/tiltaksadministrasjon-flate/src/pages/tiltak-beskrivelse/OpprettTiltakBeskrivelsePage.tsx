import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakBeskrivelseDetaljerForm } from "@/components/tiltak-beskrivelse/TiltakBeskrivelseDetaljerForm";
import { TiltakBeskrivelseVeilederinformasjonForm } from "@/components/tiltak-beskrivelse/TiltakBeskrivelseVeilederinformasjonForm";
import { WizardForm } from "@/components/skjema/WizardForm";
import { WizardStep } from "@/hooks/useWizardForm";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useNavigate } from "react-router";
import { v4 as uuidv4 } from "uuid";
import { useUpsertTiltakBeskrivelse } from "@/api/tiltak-beskrivelse/useUpsertTiltakBeskrivelse";
import {
  TiltakBeskrivelseFormValues,
  tiltakBeskrivelseDetaljerSchema,
  tiltakBeskrivelseVeilederinfoSchema,
} from "./TiltakBeskrivelseFormValues";
import { Faneinnhold, ValidationError } from "@tiltaksadministrasjon/api-client";
import { TiltakBeskrivelseIkon } from "@/components/ikoner/TiltakBeskrivelseIkon";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { UseFormReturn } from "react-hook-form";

const brodsmuler: Brodsmule[] = [
  { tittel: "Tiltaksbeskrivelser", lenke: "/tiltak-beskrivelser" },
  { tittel: "Opprett tiltaksbeskrivelse" },
];

const defaultValues = {
  navn: "",
  tiltaksnummer: null,
  tiltakstypeId: "",
  stedForGjennomforing: null,
  arrangorId: null,
  arrangorKontaktpersoner: [],
  administratorer: [],
  veilederinformasjon: {
    beskrivelse: null,
    faneinnhold: null,
    navRegioner: [],
    navKontorer: [],
    navAndreEnheter: [],
    kontaktpersoner: [],
  },
};

const steps: WizardStep[] = [
  {
    key: "Detaljer",
    schema: tiltakBeskrivelseDetaljerSchema,
    Component: <TiltakBeskrivelseDetaljerForm />,
  },
  {
    key: "Informasjon for veiledere",
    schema: tiltakBeskrivelseVeilederinfoSchema,
    Component: <TiltakBeskrivelseVeilederinformasjonForm />,
  },
];

export function OpprettTiltakBeskrivelsePage() {
  const navigate = useNavigate();
  const upsert = useUpsertTiltakBeskrivelse();

  function onSubmit(
    data: TiltakBeskrivelseFormValues,
    form: UseFormReturn<TiltakBeskrivelseFormValues>,
  ) {
    const id = uuidv4();
    upsert.mutate(
      {
        id,
        navn: data.navn,
        tiltaksnummer: data.tiltaksnummer ?? null,
        tiltakstypeId: data.tiltakstypeId,
        stedForGjennomforing: data.stedForGjennomforing ?? null,
        arrangorId: data.arrangorId ?? null,
        arrangorKontaktpersoner: data.arrangorKontaktpersoner,
        administratorer: data.administratorer,
        veilederinformasjon: {
          beskrivelse: data.veilederinformasjon.beskrivelse ?? null,
          faneinnhold: (data.veilederinformasjon.faneinnhold as Faneinnhold | null) ?? null,
          navRegioner: data.veilederinformasjon.navRegioner,
          navKontorer: data.veilederinformasjon.navKontorer,
          navAndreEnheter: data.veilederinformasjon.navAndreEnheter,
          kontaktpersoner: data.veilederinformasjon.kontaktpersoner.map((k) => ({
            navIdent: k.navIdent,
            beskrivelse: k.beskrivelse ?? null,
          })),
        },
      },
      {
        onSuccess: () => navigate(`/tiltak-beskrivelser/${id}`),
        onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
      },
    );
  }

  return (
    <>
      <title>Opprett tiltaksbeskrivelse</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakBeskrivelseIkon />} heading="Opprett tiltaksbeskrivelse" />
      <WizardForm<TiltakBeskrivelseFormValues>
        steps={steps}
        defaultValues={defaultValues}
        onCancel={() => navigate(-1)}
        onSubmit={onSubmit}
        isSubmitting={upsert.isPending}
        labels={{ submit: "Opprett" }}
      />
    </>
  );
}
