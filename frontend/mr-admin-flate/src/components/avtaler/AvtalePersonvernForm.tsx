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
import { FormCheckboxGroup } from "@/components/skjema/FormCheckboxGroup";
import { usePersonopplysninger } from "@/api/avtaler/usePersonopplysninger";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

export function AvtalePersonvernForm() {
  const { setValue, watch } = useFormContext<AvtaleFormValues>();
  const { data: personopplysninger } = usePersonopplysninger();

  const watchedPersonopplysninger = watch("personvern.personopplysninger");
  if (!personopplysninger) return <Loader />;

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
        {personopplysninger.map((p) => (
          <Checkbox key={p.type} value={p.type} size="small">
            <HStack gap="space-8" align="center">
              {p.title}
              {p.helpText && <HelpText>{p.helpText}</HelpText>}
            </HStack>
          </Checkbox>
        ))}
      </FormCheckboxGroup>
      <Checkbox
        checked={watchedPersonopplysninger.length === personopplysninger.length}
        indeterminate={
          watchedPersonopplysninger.length > 0 &&
          watchedPersonopplysninger.length !== personopplysninger.length
        }
        onChange={() => {
          if (watchedPersonopplysninger.length === personopplysninger.length) {
            setValue("personvern.personopplysninger", []);
          } else {
            setValue(
              "personvern.personopplysninger",
              personopplysninger.map(({ type }) => type),
            );
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
