import {
  BodyShort,
  Checkbox,
  CheckboxGroup,
  GuidePanel,
  HelpText,
  HStack,
  Link,
  Loader,
  Radio,
  VStack,
} from "@navikt/ds-react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { Separator } from "@/components/detaljside/Metadata";
import { AvtaleFormValues } from "@/schemas/avtale";
import { ControlledRadioGroup } from "@/components/skjema/ControlledRadioGroup";
import { usePersonopplysninger } from "@/api/avtaler/usePersonopplysninger";

export function AvtalePersonvernForm() {
  const { register, control, setValue } = useFormContext<AvtaleFormValues>();
  const { data: personopplysninger } = usePersonopplysninger();

  const watchedPersonopplysninger = useWatch({
    name: "personopplysninger",
  });

  if (!personopplysninger) return <Loader />;

  return (
    <VStack gap="4">
      <GuidePanel poster>
        Huk av de personopplysningene som er avtalt i databehandleravtalen. Nav tiltaksenhet/fylke
        er ansvarlig for at listen er i samsvar med gjeldende databehandleravtale.
      </GuidePanel>
      <Controller
        control={control}
        name="personopplysninger"
        render={({ field: { onChange, value } }) => (
          <CheckboxGroup
            legend="Personopplysninger om deltaker"
            description="Huk av de personopplysningene som kan behandles i denne avtalen."
            onChange={onChange}
            value={value}
          >
            {personopplysninger.map((p) => (
              <Checkbox key={p.personopplysning} value={p.personopplysning} size="small">
                <HStack gap="2" align="center">
                  {p.tittel}
                  {p.hjelpetekst && <HelpText>{p.hjelpetekst}</HelpText>}
                </HStack>
              </Checkbox>
            ))}
          </CheckboxGroup>
        )}
      />
      <Checkbox
        checked={watchedPersonopplysninger?.length === personopplysninger.length}
        indeterminate={
          watchedPersonopplysninger?.length > 0 &&
          watchedPersonopplysninger?.length !== personopplysninger.length
        }
        onChange={() => {
          if (watchedPersonopplysninger?.length === personopplysninger.length) {
            setValue("personopplysninger", []);
          } else {
            setValue(
              "personopplysninger",
              personopplysninger.map(({ personopplysning }) => personopplysning),
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
      <ControlledRadioGroup
        size="small"
        legend="Kan personopplysningene som kan behandles vises til veileder?"
        {...register("personvernBekreftet")}
      >
        <VStack align="start" justify="start" gap="2">
          <Radio size="small" value={false}>
            Hvilke personopplysninger som kan behandles er uavklart og kan ikke vises til veileder
          </Radio>
          <Radio size="small" value={true} id={"bekreft-personopplysninger"}>
            Bekreft og vis hvilke personopplysninger som kan behandles til veileder
          </Radio>
        </VStack>
      </ControlledRadioGroup>
    </VStack>
  );
}
