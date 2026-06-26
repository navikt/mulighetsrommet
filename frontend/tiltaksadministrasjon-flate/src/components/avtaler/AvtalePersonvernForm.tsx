import {
  BodyShort,
  Checkbox,
  GuidePanel,
  HelpText,
  HStack,
  Link,
  Loader,
  Radio,
  VStack,
} from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { AvtaleFormValues } from "@/pages/avtaler/form/validation";
import { FormRadioGroup } from "@/components/skjema/FormRadioGroup";
import { usePersonopplysninger } from "@/api/avtaler/usePersonopplysninger";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { PersonopplysningType } from "@tiltaksadministrasjon/api-client";
import { FormCheckboxGroup } from "../skjema/FormCheckboxGroup";
import { FormCheckbox } from "../skjema/FormCheckbox";
import { FormTextarea } from "../skjema/FormTextarea";

export function AvtalePersonvernForm() {
  const { setValue, watch } = useFormContext<AvtaleFormValues>();
  const { data: personopplysninger } = usePersonopplysninger();

  const watchedPersonopplysninger = watch("personvern.personopplysninger");

  if (!personopplysninger) return <Loader />;

  const annetChecked = watch("personvern.annetChecked");
  const annetPersonopplysning = personopplysninger.find(
    (p) => p.type === PersonopplysningType.ANNET,
  );

  return (
    <VStack gap="space-16">
      <GuidePanel poster>
        Huk av de personopplysningene som er avtalt i databehandleravtalen. Nav tiltaksenhet/fylke
        er ansvarlig for at listen er i samsvar med gjeldende databehandleravtale.
      </GuidePanel>
      <FormCheckboxGroup<AvtaleFormValues>
        name="personvern.personopplysninger"
        legend="Personopplysninger om deltaker"
        description="Huk av de personopplysningene som kan behandles i denne avtalen."
      >
        {personopplysninger
          .filter((p) => p.type !== PersonopplysningType.ANNET)
          .map((p) => {
            return (
              <>
                <Checkbox key={p.type} value={p.type} size="small">
                  <HStack gap="space-8" align="center">
                    {p.title}
                    {p.helpText && <HelpText>{p.helpText}</HelpText>}
                  </HStack>
                </Checkbox>
              </>
            );
          })}
      </FormCheckboxGroup>
      {annetPersonopplysning && (
        <VStack gap="space-8" className="-mt-5">
          <FormCheckbox name="personvern.annetChecked" size="small">
            <HStack gap="space-8" align="center">
              {annetPersonopplysning.title}
              {annetPersonopplysning.helpText && (
                <HelpText>{annetPersonopplysning.helpText}</HelpText>
              )}
            </HStack>
          </FormCheckbox>
          {annetChecked && (
            <FormTextarea
              label="Beskriv nærmere hvilke personopplysninger som behandles"
              size="small"
              name="personvern.annetBeskrivelse"
            />
          )}
        </VStack>
      )}
      <Checkbox
        checked={watchedPersonopplysninger.length === personopplysninger.length && annetChecked}
        indeterminate={
          watchedPersonopplysninger.length > 0 &&
          (watchedPersonopplysninger.length !== personopplysninger.length || !annetChecked)
        }
        onChange={() => {
          if (watchedPersonopplysninger.length === personopplysninger.length) {
            setValue("personvern.personopplysninger", []);
            setValue("personvern.annetChecked", false);
          } else {
            setValue(
              "personvern.personopplysninger",
              personopplysninger.map(({ type }) => type),
            );
            setValue("personvern.annetChecked", true);
          }
        }}
        size="small"
      >
        <b>Velg alle</b>
      </Checkbox>
      <BodyShort size="small">
        *Se egne retningslinjer om dette i{" "}
        <Link
          target="_blank"
          href="https://navno.sharepoint.com/sites/fag-og-ytelser-veileder-for-arbeidsrettet-brukeroppfolging/SitePages/Arbeidsrettede-tiltak.aspx"
        >
          veileder for arbeidsrettet brukeroppfølging
        </Link>{" "}
        pkt. 4.3.
      </BodyShort>
      <Separator />
      <FormRadioGroup<AvtaleFormValues>
        size="small"
        name="personvern.personvernBekreftet"
        legend="Kan personopplysningene som kan behandles vises til veileder?"
      >
        <VStack align="start" justify="start" gap="space-8">
          <Radio size="small" value={false}>
            Hvilke personopplysninger som kan behandles er uavklart og kan ikke vises til veileder
          </Radio>
          <Radio size="small" value={true} id={"bekreft-personopplysninger"}>
            Bekreft og vis hvilke personopplysninger som kan behandles til veileder
          </Radio>
        </VStack>
      </FormRadioGroup>
    </VStack>
  );
}
