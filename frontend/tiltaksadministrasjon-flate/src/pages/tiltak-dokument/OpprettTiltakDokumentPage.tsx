import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakDokumentDetaljerForm } from "@/components/tiltak-dokument/TiltakDokumentDetaljerForm";
import { TiltakDokumentVeilederinformasjonForm } from "@/components/tiltak-dokument/TiltakDokumentVeilederinformasjonForm";
import { WizardForm } from "@/components/skjema/WizardForm";
import { WizardStep } from "@/hooks/useWizardForm";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useNavigate } from "react-router";
import { v4 as uuidv4 } from "uuid";
import { useUpsertTiltakDokument } from "@/api/tiltak-dokument/useUpsertTiltakDokument";
import {
  TiltakDokumentFormValues,
  tiltakDokumentDetaljerSchema,
  tiltakDokumentVeilederinfoSchema,
} from "./TiltakDokumentFormValues";
import { Faneinnhold, ValidationError } from "@tiltaksadministrasjon/api-client";
import { TiltakDokumentIkon } from "@/components/ikoner/TiltakDokumentIkon";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { UseFormReturn } from "react-hook-form";

const brodsmuler: Brodsmule[] = [
  { tittel: "Tiltaksdokumenter", lenke: "/tiltak-dokumenter" },
  { tittel: "Opprett tiltaksdokument" },
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
    schema: tiltakDokumentDetaljerSchema,
    Component: <TiltakDokumentDetaljerForm />,
  },
  {
    key: "Informasjon for veiledere",
    schema: tiltakDokumentVeilederinfoSchema,
    Component: <TiltakDokumentVeilederinformasjonForm />,
  },
];

export function OpprettTiltakDokumentPage() {
  const navigate = useNavigate();
  const upsert = useUpsertTiltakDokument();

  function onSubmit(data: TiltakDokumentFormValues, form: UseFormReturn<TiltakDokumentFormValues>) {
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
        onSuccess: () => navigate(`/tiltak-dokumenter/${id}`),
        onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
      },
    );
  }

  return (
    <>
      <title>Opprett tiltaksdokument</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakDokumentIkon />} heading="Opprett tiltaksdokument" />
      <WizardForm<TiltakDokumentFormValues>
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
